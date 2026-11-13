import bpy
import math
from mathutils import Vector
from pathlib import Path

# Neon Rush — first self-produced pilot asset.
# Coordinates: character faces +Z; Y is vertical. Hard-surface pieces are
# bone-weighted individually so equipment and armour can be replaced later.

OUT = Path("/tmp/neon-rush-pilot")
OUT.mkdir(parents=True, exist_ok=True)

bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
for datablocks in (bpy.data.meshes, bpy.data.curves, bpy.data.materials, bpy.data.armatures, bpy.data.actions):
    pass

def mat(name, color, metallic=0.0, rough=0.4, emission=None, strength=0.0):
    m = bpy.data.materials.new(name)
    m.diffuse_color = (*color, 1)
    m.use_nodes = True
    p = m.node_tree.nodes.get("Principled BSDF")
    p.inputs["Base Color"].default_value = (*color, 1)
    p.inputs["Metallic"].default_value = metallic
    p.inputs["Roughness"].default_value = rough
    if emission:
        p.inputs["Emission Color"].default_value = (*emission, 1)
        p.inputs["Emission Strength"].default_value = strength
    return m

armor = mat("Armor | graphite ceramic", (0.045,0.065,0.09), .72, .28)
edge = mat("Armor | titanium edges", (.22,.29,.34), .82, .24)
undersuit = mat("Undersuit | carbon weave", (.012,.019,.028), .18, .56)
cyan = mat("Energy | ion cyan", (.015,.55,.78), .35, .22, (.01,.48,1.0), 2.8)
magenta = mat("Energy | magenta", (.65,.025,.28), .3, .24, (.9,.015,.24), 2.0)
visor = mat("Visor | polarized glass", (.012,.12,.19), .82, .12, (.01,.23,.42), .65)
orange = mat("Safety | signal orange", (.95,.19,.025), .45, .3)
rubber = mat("Boots | rubber", (.018,.024,.03), .05, .7)

def finish(obj, material, bevel=0.0):
    obj.data.materials.append(material)
    if bevel and obj.type == "MESH":
        mod = obj.modifiers.new("Machined edge radii", "BEVEL")
        mod.width = bevel
        mod.segments = 3
        mod.limit_method = "ANGLE"
        obj.modifiers.new("Weighted corner normals", "WEIGHTED_NORMAL")
    return obj

def cube(name, loc, scale, material, bevel=.08, bone="spine"):
    bpy.ops.mesh.primitive_cube_add(size=1, location=loc)
    o=bpy.context.object; o.name=name; o.dimensions=scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    return finish(o, material, bevel), bone

def uv(name, loc, scale, material, bone="spine", seg=32, rings=20):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=seg, ring_count=rings, location=loc)
    o=bpy.context.object; o.name=name; o.scale=scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    for p in o.data.polygons: p.use_smooth=True
    return finish(o, material), bone

def cyl(name, loc, radius, depth, material, bone, axis="Y"):
    bpy.ops.mesh.primitive_cylinder_add(vertices=32, radius=radius, depth=depth, location=loc)
    o=bpy.context.object; o.name=name
    if axis=="X": o.rotation_euler[1]=math.pi/2
    if axis=="Z": o.rotation_euler[0]=math.pi/2
    return finish(o, material, .035), bone

def curve(name, points, radius, material, bone="spine"):
    c=bpy.data.curves.new(name,"CURVE"); c.dimensions="3D"; c.bevel_depth=radius; c.bevel_resolution=3
    s=c.splines.new("POLY"); s.points.add(len(points)-1)
    for p,co in zip(s.points,points): p.co=(*co,1)
    o=bpy.data.objects.new(name,c); bpy.context.collection.objects.link(o); o.data.materials.append(material)
    return o,bone

parts=[]
def add(v): parts.append(v)

# Torso: layered chest shell, articulated abdomen, waist and back harness.
add(cube("Torso | undersuit", (0,1.48,0), (.78,.88,.46), undersuit,.16,"spine"))
add(cube("Chest | upper shell", (0,1.72,.055), (1.02,.46,.56), armor,.16,"spine"))
add(uv("Chest | left sculpted plate",(-.245,1.73,.32),(.29,.27,.12),edge))
add(uv("Chest | right sculpted plate",(.245,1.73,.32),(.29,.27,.12),edge))
add(cube("Chest | central power housing",(0,1.72,.39),(.19,.27,.08),armor,.055,"spine"))
add(uv("Chest | cyan reactor",(0,1.73,.455),(.105,.15,.035),cyan))
for y in (1.37,1.18,1.00):
    add(cube("Abdominal segmented plate",(0,y,.255),(.55,.13,.24),edge,.055,"spine"))
    add(cube("Abdominal center seam",(0,y,.385),(.035,.075,.025),cyan,.012,"spine"))
add(cube("Waist | armored belt",(0,.88,0),(.78,.22,.5),armor,.09,"pelvis"))
add(cube("Waist | front buckle",(0,.89,.27),(.25,.14,.07),orange,.035,"pelvis"))
add(cube("Back | spine carrier",(0,1.48,-.29),(.48,.72,.2),edge,.08,"spine"))

# Helmet: oversized silhouette, cheek guards, brow and a single broad wrap visor.
add(uv("Helmet | main shell",(0,2.38,.015),(.53,.53,.48),armor,"head",48,32))
add(uv("Helmet | crown cap",(0,2.67,-.015),(.40,.23,.40),edge,"head"))
add(uv("Helmet | visor glass",(0,2.40,.415),(.455,.285,.105),visor,"head",48,24))
add(curve("Helmet | cyan visor upper arc",[(-.40,2.57,.46),(-.25,2.66,.49),(0,2.69,.50),(.25,2.66,.49),(.40,2.57,.46)],.022,cyan,"head"))
add(cube("Helmet | lower jaw guard",(0,2.15,.35),(.62,.16,.24),edge,.07,"head"))
for side in (-1,1):
    add(uv("Helmet | ear module",(side*.51,2.38,0),(.12,.2,.25),armor,"head"))
    add(uv("Helmet | ear light",(side*.59,2.39,.02),(.035,.08,.10),magenta,"head"))
    add(cube("Helmet | temple fin",(side*.42,2.70,-.02),(.13,.25,.38),armor,.06,"head"))

# Shoulder pauldrons and arms with clear mechanical joints.
for side, label in ((-1,"L"),(1,"R")):
    add(uv(f"{label} shoulder joint",(side*.61,1.82,0),(.22,.25,.23),undersuit,f"upper_arm_{label}"))
    add(uv(f"{label} shoulder pauldron",(side*.70,1.88,.015),(.34,.28,.34),armor,f"upper_arm_{label}"))
    add(cube(f"{label} shoulder signal",(side*.70,1.72,.31),(.28,.055,.045),orange,.018,f"upper_arm_{label}"))
    add(cyl(f"{label} upper arm",(side*.72,1.48,0),.16,.54,undersuit,f"upper_arm_{label}"))
    add(uv(f"{label} elbow joint",(side*.73,1.20,.02),(.17,.16,.17),edge,f"forearm_{label}"))
    add(cube(f"{label} forearm guard",(side*.74,1.02,.12),(.30,.42,.32),armor,.095,f"forearm_{label}"))
    add(cube(f"{label} forearm energy rail",(side*.74,1.02,.292),(.055,.30,.035),cyan,.014,f"forearm_{label}"))
    add(uv(f"{label} glove",(side*.74,.77,.06),(.17,.16,.18),undersuit,f"hand_{label}"))
    add(cube(f"{label} glove knuckle",(side*.74,.81,.20),(.22,.10,.12),edge,.04,f"hand_{label}"))

# Legs: thigh shells, knee caps, shin guards and broad flight boots.
for side,label in ((-1,"L"),(1,"R")):
    add(uv(f"{label} hip joint",(side*.22,.82,0),(.20,.22,.20),undersuit,"pelvis"))
    add(cube(f"{label} thigh undersuit",(side*.23,.57,0),(.31,.58,.34),undersuit,.12,f"thigh_{label}"))
    add(cube(f"{label} thigh armor",(side*.23,.59,.17),(.34,.42,.19),armor,.095,f"thigh_{label}"))
    add(cube(f"{label} thigh stripe",(side*.23,.61,.274),(.055,.30,.025),magenta,.012,f"thigh_{label}"))
    add(uv(f"{label} knee joint",(side*.23,.27,.015),(.17,.17,.18),undersuit,f"shin_{label}"))
    add(cube(f"{label} knee cap",(side*.23,.27,.17),(.32,.25,.18),edge,.07,f"shin_{label}"))
    add(cube(f"{label} shin guard",(side*.23,-.10,.10),(.30,.49,.29),armor,.09,f"shin_{label}"))
    add(cube(f"{label} shin light",(side*.23,-.10,.255),(.045,.30,.025),cyan,.012,f"shin_{label}"))
    add(cube(f"{label} boot",(side*.23,-.43,.16),(.34,.25,.56),rubber,.09,f"foot_{label}"))
    add(cube(f"{label} boot toe armor",(side*.23,-.38,.39),(.31,.14,.16),edge,.05,f"foot_{label}"))

# Distinctive back harness and exposed energy conduits; equipment socket stays clear.
for side in (-1,1):
    add(cube("Back | harness rail",(side*.30,1.56,-.39),(.10,.62,.12),edge,.035,"spine"))
    add(curve("Back | cyan conduit",[(side*.31,1.85,-.46),(side*.34,1.55,-.47),(side*.30,1.22,-.43)],.025,cyan,"spine"))
add(cube("Back | jetpack mounting plate",(0,1.48,-.48),(.58,.65,.16),edge,.07,"spine"))

# Armature with separate hard-surface pieces rigidly weighted to their controlling bones.
arm_data=bpy.data.armatures.new("NeonRushPilot_Rig")
arm=bpy.data.objects.new("NeonRushPilot_Rig",arm_data); bpy.context.collection.objects.link(arm)
bpy.context.view_layer.objects.active=arm; arm.select_set(True)
bpy.ops.object.mode_set(mode="EDIT")
bones={
 "root":( (0,0,0),(0,.45,0),None ),
 "pelvis":((0,.45,0),(0,.95,0),"root"),
 "spine":((0,.95,0),(0,1.95,0),"pelvis"),
 "head":((0,1.95,0),(0,2.55,0),"spine"),
}
for side,label in ((-1,"L"),(1,"R")):
    bones[f"upper_arm_{label}"]=((side*.48,1.88,0),(side*.73,1.40,0),"spine")
    bones[f"forearm_{label}"]=((side*.73,1.40,0),(side*.74,.95,0),f"upper_arm_{label}")
    bones[f"hand_{label}"]=((side*.74,.95,0),(side*.74,.70,0),f"forearm_{label}")
    bones[f"thigh_{label}"]=((side*.22,.85,0),(side*.23,.28,0),"pelvis")
    bones[f"shin_{label}"]=((side*.23,.28,0),(side*.23,-.28,0),f"thigh_{label}")
    bones[f"foot_{label}"]=((side*.23,-.28,0),(side*.23,-.50,.25),f"shin_{label}")
for name,(head,tail,parent) in bones.items():
    b=arm_data.edit_bones.new(name); b.head=head; b.tail=tail
    if parent: b.parent=arm_data.edit_bones[parent]
bpy.ops.object.mode_set(mode="OBJECT")
arm.show_in_front=True

for obj,bone in parts:
    if obj.type!="MESH":
        # Convert curves so they can be exported and weighted consistently.
        bpy.context.view_layer.objects.active=obj
        bpy.ops.object.convert(target="MESH")
    vg=obj.vertex_groups.new(name=bone)
    vg.add(list(range(len(obj.data.vertices))),1.0,"REPLACE")
    mod=obj.modifiers.new("Pilot armature","ARMATURE"); mod.object=arm
    obj.parent=arm

# Flight idle and bank clips. These are deliberately restrained, readable loops.
def animate_action(name,frames,poses):
    bpy.context.scene.frame_start=frames[0]; bpy.context.scene.frame_end=frames[-1]
    for bone_name, keys in poses.items():
        pb=arm.pose.bones[bone_name]
        for frame,rot in zip(frames,keys):
            pb.rotation_mode="XYZ"; pb.rotation_euler=rot
            pb.keyframe_insert(data_path="rotation_euler",frame=frame,group=bone_name)
    action=arm.animation_data.action
    action.name=name
    for fc in action.fcurves:
        for kp in fc.keyframe_points: kp.interpolation="BEZIER"
    bpy.ops.nla.bake(frame_start=frames[0],frame_end=frames[-1],only_selected=False,visual_keying=True,clear_constraints=False,use_current_action=True,bake_types={"POSE"})
    arm.animation_data.action.name=name

arm.animation_data_create()
animate_action("idle_flight",(1,16,32),{
 "spine":[(0,0,0),(.015,0,.01),(0,0,0)],
 "head":[(0,0,0),(0,.025,0),(0,0,0)],
 "upper_arm_L":[(-.10,0,-.08),(-.12,0,-.10),(-.10,0,-.08)],
 "upper_arm_R":[(-.10,0,.08),(-.12,0,.10),(-.10,0,.08)]
})
# Additional named clips are separate actions for the runtime animation system.
for action_name, tilt in (("fly_forward",-.08),("bank_left",-.24),("bank_right",.24),("ascend",-.15),("descend",.15),("hit_react",-.30),("boost",-.12),("victory",.10)):
    bpy.ops.object.mode_set(mode="OBJECT")
    if arm.animation_data: arm.animation_data.action=None
    for frame, value in ((1,0),(12,tilt),(24,0)):
        pb=arm.pose.bones["spine"]; pb.rotation_mode="XYZ"; pb.rotation_euler=(value,0,0)
        pb.keyframe_insert(data_path="rotation_euler",frame=frame,group="spine")
    arm.animation_data.action.name=action_name
    for fc in arm.animation_data.action.fcurves:
        for kp in fc.keyframe_points: kp.interpolation="BEZIER"

# Studio preview camera and lighting.
bpy.ops.object.camera_add(location=(4.8,-7.8,5.1))
cam=bpy.context.object; cam.name="Preview Camera"
target=Vector((0,1.1,0)); direction=target-cam.location
cam.rotation_euler=direction.to_track_quat("-Z","Y").to_euler()
cam.data.lens=55; bpy.context.scene.camera=cam
def area(name,loc,power,color,size):
    bpy.ops.object.light_add(type="AREA",location=loc)
    l=bpy.context.object; l.name=name; l.data.energy=power; l.data.color=color; l.data.shape="DISK"; l.data.size=size
    l.rotation_euler=(target-l.location).to_track_quat("-Z","Y").to_euler()
area("Key | cool softbox",(4,-4,5),850,(.55,.82,1),5)
area("Rim | cyan",(-4,2,1),1100,(.02,.65,1),3)
area("Rim | magenta",(2,3,-3),900,(1,.04,.35),3)
area("Fill | neutral",(-3,-2,2),500,(.75,.85,1),4)
world=bpy.context.scene.world or bpy.data.worlds.new("Studio World")
bpy.context.scene.world=world; world.use_nodes=True
world.node_tree.nodes["Background"].inputs[0].default_value=(.012,.018,.03,1)
world.node_tree.nodes["Background"].inputs[1].default_value=.35

scene=bpy.context.scene
scene.render.engine="BLENDER_EEVEE_NEXT"
scene.eevee.taa_render_samples=64
scene.render.resolution_x=1200; scene.render.resolution_y=1200; scene.render.resolution_percentage=100
scene.render.image_settings.file_format="PNG"; scene.render.film_transparent=True
scene.render.filepath=str(OUT/"pilot_preview.png")
scene.view_settings.view_transform="AgX"
bpy.ops.render.render(write_still=True)

# Export one GLB with the rig, mesh pieces, PBR materials and animation actions.
bpy.ops.object.select_all(action="SELECT")
bpy.context.view_layer.objects.active=arm
bpy.ops.export_scene.gltf(filepath=str(OUT/"neon_rush_pilot_v01.glb"),export_format="GLB",use_selection=True,export_animations=True,export_skins=True,export_morph=False,export_apply=True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/"neon_rush_pilot_source.blend"))
print("PILOT_ASSET_OUTPUT", *(str(p) for p in OUT.iterdir()))
