#!/usr/bin/env python3
"""Generate the clean 5 x 4 x 13 Neon Rush corridor GLB using only stdlib."""
import base64, json, struct
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "app/src/main/assets/corridor.glb"

# Each quad is wound toward the corridor interior. Vertices are not shared
# between faces, so every face has its own explicit normal and UVs.
groups = [
    (0, [((0, 1, 0), [(-2.5,0,3),(2.5,0,3),(2.5,0,-10),(-2.5,0,-10)])]),
    (1, [
        ((1,0,0), [(-2.5,0,3),(-2.5,0,-10),(-2.5,4,-10),(-2.5,4,3)]),
        ((-1,0,0), [(2.5,0,3),(2.5,4,3),(2.5,4,-10),(2.5,0,-10)]),
        ((0,0,1), [(-2.5,0,-10),(2.5,0,-10),(2.5,4,-10),(-2.5,4,-10)]),
    ]),
    (2, [((0,-1,0), [(-2.5,4,3),(-2.5,4,-10),(2.5,4,-10),(2.5,4,3)])]),
    (3, [
        ((1,0,0), [(-2.42,.12,3),(-2.42,.12,-10),(-2.42,.20,-10),(-2.42,.20,3)]),
        ((-1,0,0), [(2.42,.12,3),(2.42,.20,3),(2.42,.20,-10),(2.42,.12,-10)]),
        ((0,-1,0), [(-1.8,3.96,3),(-1.8,3.96,-10),(-1.65,3.96,-10),(-1.65,3.96,3)]),
        ((0,-1,0), [(1.65,3.96,3),(1.65,3.96,-10),(1.8,3.96,-10),(1.8,3.96,3)]),
    ]),
    (4, [((0,1,0), [(-2.45,.015,-9.7),(-2.45,.015,-9.55),(2.45,.015,-9.55),(2.45,.015,-9.7)])]),
]

materials = [
 {"name":"Graphite floor","doubleSided":True,"pbrMetallicRoughness":{"baseColorFactor":[.055,.075,.095,1],"metallicFactor":.72,"roughnessFactor":.48}},
 {"name":"Dark wall panels","doubleSided":True,"pbrMetallicRoughness":{"baseColorFactor":[.12,.16,.19,1],"metallicFactor":.55,"roughnessFactor":.42}},
 {"name":"Ceiling panels","doubleSided":True,"pbrMetallicRoughness":{"baseColorFactor":[.075,.095,.12,1],"metallicFactor":.6,"roughnessFactor":.5}},
 {"name":"Cyan emissive strips","doubleSided":True,"pbrMetallicRoughness":{"baseColorFactor":[.02,.32,.42,1],"metallicFactor":.15,"roughnessFactor":.3},"emissiveFactor":[0,.75,1]},
 {"name":"Amber warning strips","doubleSided":True,"pbrMetallicRoughness":{"baseColorFactor":[.5,.18,.025,1],"metallicFactor":.1,"roughnessFactor":.35},"emissiveFactor":[1,.22,.015]},
]

binary = bytearray()
buffer_views, accessors, primitives = [], [], []

def align4():
    while len(binary) % 4:
        binary.append(0)

def add_view(data, target):
    align4()
    offset = len(binary)
    binary.extend(data)
    buffer_views.append({"buffer":0,"byteOffset":offset,"byteLength":len(data),"target":target})
    return len(buffer_views) - 1

for material_index, quads in groups:
    positions, normals, uvs, indices = [], [], [], []
    for normal, vertices in quads:
        base = len(positions) // 3
        for i, (x,y,z) in enumerate(vertices):
            positions.extend((x,y,z))
            normals.extend(normal)
            uvs.extend((1.0 if i in (1,2) else 0.0, 1.0 if i in (2,3) else 0.0))
        indices.extend((base,base+1,base+2,base,base+2,base+3))

    p_view = add_view(struct.pack("<"+"f"*len(positions), *positions), 34962)
    n_view = add_view(struct.pack("<"+"f"*len(normals), *normals), 34962)
    uv_view = add_view(struct.pack("<"+"f"*len(uvs), *uvs), 34962)
    i_view = add_view(struct.pack("<"+"H"*len(indices), *indices), 34963)

    points = list(zip(positions[0::3], positions[1::3], positions[2::3]))
    p_accessor = len(accessors)
    accessors.append({"bufferView":p_view,"componentType":5126,"count":len(points),"type":"VEC3",
                      "min":[min(p[a] for p in points) for a in range(3)],
                      "max":[max(p[a] for p in points) for a in range(3)]})
    n_accessor = len(accessors)
    accessors.append({"bufferView":n_view,"componentType":5126,"count":len(points),"type":"VEC3"})
    uv_accessor = len(accessors)
    accessors.append({"bufferView":uv_view,"componentType":5126,"count":len(points),"type":"VEC2"})
    i_accessor = len(accessors)
    accessors.append({"bufferView":i_view,"componentType":5123,"count":len(indices),"type":"SCALAR",
                      "min":[0],"max":[max(indices)]})

    primitives.append({"attributes":{"POSITION":p_accessor,"NORMAL":n_accessor,"TEXCOORD_0":uv_accessor},
                        "indices":i_accessor,"material":material_index,"mode":4})

document = {
 "asset":{"version":"2.0","generator":"Neon Rush Watertight Corridor Generator"},
 "scene":0,"scenes":[{"nodes":[0]}],
 "nodes":[{"mesh":0,"name":"Watertight Neon Rush Corridor"}],
 "meshes":[{"name":"Watertight Corridor 5x4x13","primitives":primitives}],
 "materials":materials,"accessors":accessors,
 "buffers":[{"byteLength":len(binary)}],"bufferViews":buffer_views,
}
json_chunk = json.dumps(document, separators=(",",":")).encode("utf-8")
json_chunk += b" " * ((-len(json_chunk)) % 4)
binary.extend(b"\0" * ((-len(binary)) % 4))
total_length = 12 + 8 + len(json_chunk) + 8 + len(binary)
glb = (struct.pack("<4sII",b"glTF",2,total_length)
       + struct.pack("<II",len(json_chunk),0x4E4F534A) + json_chunk
       + struct.pack("<II",len(binary),0x004E4942) + binary)

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_bytes(glb)
OUT.with_suffix(".glb.b64").write_text(base64.b64encode(glb).decode("ascii"), encoding="ascii")
print(f"Wrote {OUT} ({len(glb)} bytes)")
