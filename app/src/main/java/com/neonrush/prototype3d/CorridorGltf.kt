package com.neonrush.prototype3d

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object CorridorGltf {
    fun create(): ByteBuffer {
        val vertices = floatArrayOf(
            -0.5f,-0.5f,0.5f, 0f,0f,1f, 0.5f,-0.5f,0.5f, 0f,0f,1f, 0.5f,0.5f,0.5f, 0f,0f,1f, -0.5f,0.5f,0.5f, 0f,0f,1f,
            0.5f,-0.5f,-0.5f, 0f,0f,-1f, -0.5f,-0.5f,-0.5f, 0f,0f,-1f, -0.5f,0.5f,-0.5f, 0f,0f,-1f, 0.5f,0.5f,-0.5f, 0f,0f,-1f,
            0.5f,-0.5f,0.5f, 1f,0f,0f, 0.5f,-0.5f,-0.5f, 1f,0f,0f, 0.5f,0.5f,-0.5f, 1f,0f,0f, 0.5f,0.5f,0.5f, 1f,0f,0f,
            -0.5f,-0.5f,-0.5f, -1f,0f,0f, -0.5f,-0.5f,0.5f, -1f,0f,0f, -0.5f,0.5f,0.5f, -1f,0f,0f, -0.5f,0.5f,-0.5f, -1f,0f,0f,
            -0.5f,0.5f,0.5f, 0f,1f,0f, 0.5f,0.5f,0.5f, 0f,1f,0f, 0.5f,0.5f,-0.5f, 0f,1f,0f, -0.5f,0.5f,-0.5f, 0f,1f,0f,
            -0.5f,-0.5f,-0.5f, 0f,-1f,0f, 0.5f,-0.5f,-0.5f, 0f,-1f,0f, 0.5f,-0.5f,0.5f, 0f,-1f,0f, -0.5f,-0.5f,0.5f, 0f,-1f,0f
        )
        val pattern = intArrayOf(0, 1, 2, 0, 2, 3)
        val indices = ShortArray(36) { i -> (i / 6 * 4 + pattern[i % 6]).toShort() }
        val binary = ByteBuffer.allocate(648).order(ByteOrder.LITTLE_ENDIAN)
        vertices.forEach(binary::putFloat)
        indices.forEach(binary::putShort)
        val bytes = ByteArray(binary.position())
        binary.flip()
        binary.get(bytes)
        val dataUri = "data:application/octet-stream;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)

        val nodes = mutableListOf<String>()
        fun node(mesh: Int, x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, name: String? = null) {
            val nodeName = name?.let { "\"name\":\"$it\"," } ?: ""
            nodes += """{$nodeName"mesh":$mesh,"translation":[$x,$y,$z],"scale":[$sx,$sy,$sz]}"""
        }

        nodes += """{"name":"pilot_root","children":[1,2,3,4,5,6,7,8,9,10]}"""
        node(0, 0f, -0.58f, -3.4f, 0.52f, 0.72f, 0.32f, "pilot_torso")
        node(0, 0f, 0.12f, -3.42f, 0.38f, 0.38f, 0.38f, "pilot_helmet")
        node(1, 0f, 0.13f, -3.20f, 0.27f, 0.09f, 0.035f, "pilot_visor")
        node(0, 0f, -0.55f, -3.82f, 0.36f, 0.42f, 0.24f, "pilot_jetpack")
        node(2, -0.43f, -0.55f, -3.42f, 0.18f, 0.58f, 0.22f, "pilot_left_arm")
        node(2, 0.43f, -0.55f, -3.42f, 0.18f, 0.58f, 0.22f, "pilot_right_arm")
        node(0, -0.17f, -1.13f, -3.35f, 0.19f, 0.55f, 0.22f, "pilot_left_leg")
        node(0, 0.17f, -1.13f, -3.35f, 0.19f, 0.55f, 0.22f, "pilot_right_leg")
        node(1, -0.23f, -0.52f, -3.83f, 0.06f, 0.34f, 0.08f, "pilot_left_pack_light")
        node(1, 0.23f, -0.52f, -3.83f, 0.06f, 0.34f, 0.08f, "pilot_right_pack_light")

        for (i in 0..3) {
            val z = -4.5f - i * 9f
            node(0, 0f, -2.2f, z, 2.6f, 0.15f, 9f, "env_floor_$i")
            node(0, 0f, 2.2f, z, 2.6f, 0.15f, 9f, "env_ceiling_$i")
            node(0, -2.6f, 0f, z, 0.15f, 2.4f, 9f, "env_wall_left_$i")
            node(0, 2.6f, 0f, z, 0.15f, 2.4f, 9f, "env_wall_right_$i")
        }
        for (i in 0..6) {
            val z = -1.5f - i * 3.7f
            node(1, -2.48f, 0.35f, z, 0.035f, 0.055f, 1.15f, "guide_left_$i")
            node(1, 2.48f, 0.35f, z, 0.035f, 0.055f, 1.15f, "guide_right_$i")
            node(1, -1.8f, -2.08f, z, 0.75f, 0.025f, 0.035f, "floor_left_$i")
            node(1, 1.8f, -2.08f, z, 0.75f, 0.025f, 0.035f, "floor_right_$i")
        }
        for (i in 0..5) node(2, 0f, 2.08f, -2.5f - i * 4.2f, 1.65f, 0.025f, 0.045f, "ceiling_marker_$i")

        // Security door: the two centre panels slide apart as the pilot approaches.
        node(0, -1.78f, 0f, -36f, 0.22f, 1.65f, 0.28f, "door_frame_left")
        node(0, 1.78f, 0f, -36f, 0.22f, 1.65f, 0.28f, "door_frame_right")
        node(0, 0f, 1.62f, -36f, 3.8f, 0.22f, 0.28f, "door_frame_top")
        node(2, -0.55f, 0f, -36f, 0.62f, 1.42f, 0.16f, "door_left_panel")
        node(2, 0.55f, 0f, -36f, 0.62f, 1.42f, 0.16f, "door_right_panel")
        node(1, 0f, 1.82f, -36f, 0.72f, 0.045f, 0.06f, "door_status_light")

        // Connected hangar beyond the doorway: wider and taller than the corridor.
        node(0, 0f, -2.2f, -47f, 8.5f, 0.15f, 14f, "hangar_floor")
        node(0, 0f, 4.8f, -47f, 8.5f, 0.15f, 14f, "hangar_ceiling")
        node(0, -8.5f, 1.3f, -47f, 0.15f, 3.5f, 14f, "hangar_wall_left")
        node(0, 8.5f, 1.3f, -47f, 0.15f, 3.5f, 14f, "hangar_wall_right")
        for (i in 0..2) {
            val z = -39f - i * 8f
            node(0, -7.2f, 0.65f, z, 0.42f, 2.85f, 0.48f, "hangar_pillar_left_$i")
            node(0, 7.2f, 0.65f, z, 0.42f, 2.85f, 0.48f, "hangar_pillar_right_$i")
        }
        for (i in 0..3) {
            val z = -39f - i * 6.5f
            node(1, -4.6f, 4.68f, z, 1.7f, 0.025f, 0.07f, "hangar_light_left_$i")
            node(2, 4.6f, 4.68f, z, 1.7f, 0.025f, 0.07f, "hangar_light_right_$i")
            node(1, -4.2f, -2.08f, z, 1.4f, 0.025f, 0.045f, "hangar_floor_light_left_$i")
            node(1, 4.2f, -2.08f, z, 1.4f, 0.025f, 0.045f, "hangar_floor_light_right_$i")
        }

        val json = """
          {
            "asset":{"version":"2.0","generator":"Neon Rush 3D Prototype"},
            "scene":0,
            "scenes":[{"nodes":[${(listOf(0) + (11 until nodes.size)).joinToString(",")}]}],
            "nodes":[${nodes.joinToString(",")}],
            "meshes":[
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":0}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":1}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":2}]}
            ],
            "materials":[
              {"name":"Midnight alloy","pbrMetallicRoughness":{"baseColorFactor":[0.035,0.065,0.12,1],"metallicFactor":0.72,"roughnessFactor":0.38}},
              {"name":"Cyan energy","emissiveFactor":[0.02,0.75,1.0],"pbrMetallicRoughness":{"baseColorFactor":[0.02,0.35,0.48,1],"metallicFactor":0.25,"roughnessFactor":0.25}},
              {"name":"Magenta energy","emissiveFactor":[0.9,0.025,0.35],"pbrMetallicRoughness":{"baseColorFactor":[0.4,0.015,0.12,1],"metallicFactor":0.2,"roughnessFactor":0.3}}
            ],
            "buffers":[{"uri":"$dataUri","byteLength":${bytes.size}}],
            "bufferViews":[
              {"buffer":0,"byteOffset":0,"byteLength":576,"byteStride":24,"target":34962},
              {"buffer":0,"byteOffset":576,"byteLength":72,"target":34963}
            ],
            "accessors":[
              {"bufferView":0,"byteOffset":0,"componentType":5126,"count":24,"type":"VEC3","min":[-0.5,-0.5,-0.5],"max":[0.5,0.5,0.5]},
              {"bufferView":0,"byteOffset":12,"componentType":5126,"count":24,"type":"VEC3"},
              {"bufferView":1,"byteOffset":0,"componentType":5123,"count":36,"type":"SCALAR"}
            ]
          }
        """.trimIndent()
        return ByteBuffer.wrap(json.toByteArray(Charsets.UTF_8))
    }

    fun resource(uri: String): ByteBuffer {
        val encoded = uri.substringAfter(',', "")
        return ByteBuffer.wrap(Base64.decode(encoded, Base64.DEFAULT))
    }
}
