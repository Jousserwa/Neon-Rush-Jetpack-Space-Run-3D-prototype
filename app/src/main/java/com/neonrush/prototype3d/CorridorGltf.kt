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

        node(0, 0f, -1.65f, -12f, 2.6f, 0.15f, 14f)
        node(0, 0f, 1.65f, -12f, 2.6f, 0.15f, 14f)
        node(0, -2.6f, 0f, -12f, 0.15f, 1.8f, 14f)
        node(0, 2.6f, 0f, -12f, 0.15f, 1.8f, 14f)
        node(0, 0f, 0f, -26f, 2.6f, 1.8f, 0.15f)
        for (i in 0..6) {
            val z = -1.5f - i * 3.7f
            node(1, -2.48f, 0.35f, z, 0.035f, 0.055f, 1.15f, "guide_left_$i")
            node(1, 2.48f, 0.35f, z, 0.035f, 0.055f, 1.15f, "guide_right_$i")
            node(1, -1.8f, -1.55f, z, 0.75f, 0.025f, 0.035f, "floor_left_$i")
            node(1, 1.8f, -1.55f, z, 0.75f, 0.025f, 0.035f, "floor_right_$i")
        }
        for (i in 0..5) node(2, 0f, 1.54f, -2.5f - i * 4.2f, 1.65f, 0.025f, 0.045f, "ceiling_marker_$i")

        val json = """
          {
            "asset":{"version":"2.0","generator":"Neon Rush 3D Prototype"},
            "scene":0,
            "scenes":[{"nodes":[${nodes.indices.joinToString(",") { it.toString() }}]}],
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
