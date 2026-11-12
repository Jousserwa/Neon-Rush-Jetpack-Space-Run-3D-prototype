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

        // Four repeating wide hangar bays form a continuous room beyond the doorway.
        // Each bay is 9 metres long; the runtime recycles them as the pilot advances.
        for (i in 0..3) {
            val z = -40.5f - i * 9f
            node(0, 0f, -2.2f, z, 8.5f, 0.15f, 9f, "hangar_floor_$i")
            node(0, 0f, 2.6f, z, 8.5f, 0.15f, 9f, "hangar_ceiling_$i")
            node(0, -8.5f, 1.3f, z, 0.15f, 3.5f, 9f, "hangar_wall_left_$i")
            node(0, 8.5f, 1.3f, z, 0.15f, 3.5f, 9f, "hangar_wall_right_$i")
            node(0, -7.2f, 0.65f, z, 0.42f, 2.85f, 0.48f, "hangar_pillar_left_$i")
            node(0, 7.2f, 0.65f, z, 0.42f, 2.85f, 0.48f, "hangar_pillar_right_$i")
            node(1, -4.6f, 4.68f, z, 1.7f, 0.025f, 0.07f, "hangar_light_left_$i")
            node(2, 4.6f, 4.68f, z, 1.7f, 0.025f, 0.07f, "hangar_light_right_$i")
            node(1, -4.2f, -2.08f, z, 1.4f, 0.025f, 0.045f, "hangar_floor_light_left_$i")
            node(1, 4.2f, -2.08f, z, 1.4f, 0.025f, 0.045f, "hangar_floor_light_right_$i")

            // Heavy docking-bay frames make the hangar read as a large industrial space.
            node(0, -6.8f, 1.15f, z, 0.32f, 2.65f, 0.48f, "hangar_arch_left_$i")
            node(0, 6.8f, 1.15f, z, 0.32f, 2.65f, 0.48f, "hangar_arch_right_$i")
            node(0, 0f, 3.65f, z, 6.9f, 0.32f, 0.48f, "hangar_arch_top_$i")

            // Raised service platforms and their illuminated safety edges.
            node(0, -4.55f, -1.91f, z, 2.35f, 0.18f, 3.9f, "hangar_platform_left_$i")
            node(0, 4.55f, -1.91f, z, 2.35f, 0.18f, 3.9f, "hangar_platform_right_$i")
            node(1, -3.35f, -1.77f, z, 0.045f, 0.035f, 3.45f, "hangar_platform_edge_left_$i")
            node(1, 3.35f, -1.77f, z, 0.045f, 0.035f, 3.45f, "hangar_platform_edge_right_$i")

            // Overhead service rails and bulky equipment pods add depth and scale.
            node(0, -2.75f, 4.35f, z, 0.16f, 0.14f, 8.2f, "hangar_rail_left_$i")
            node(0, 2.75f, 4.35f, z, 0.16f, 0.14f, 8.2f, "hangar_rail_right_$i")
            node(0, -2.0f, -0.55f, z - 1.8f, 0.82f, 1.15f, 1.05f, "hangar_equipment_left_$i")
            node(0, 2.0f, -0.55f, z + 1.8f, 0.82f, 1.15f, 1.05f, "hangar_equipment_right_$i")
        }


        // Four repeating outdoor forest sections begin beyond the hangar.
        // Each section is 9 metres long and is streamed by the gameplay loop.
        for (i in 0..3) {
            val z = -130.5f - i * 9f
            // Broad ground with offset earth patches to break the perfectly flat silhouette.
            node(4, 0f, -2.2f, z, 25f, 0.16f, 9f, "forest_ground_$i")
            node(6, -7.8f, -2.02f, z - 2.0f, 2.6f, 0.12f, 3.4f, "forest_moss_left_$i")
            node(6, 8.2f, -2.04f, z + 2.3f, 4.2f, 0.10f, 3.0f, "forest_moss_right_$i")
            node(4, -3.9f, -1.98f, z + 1.5f, 1.8f, 0.18f, 1.15f, "forest_earth_mound_left_$i")
            node(4, 4.3f, -2.00f, z - 2.1f, 2.0f, 0.16f, 1.25f, "forest_earth_mound_right_$i")

            // Distant tree line: staggered trunks and layered crowns for depth.
            node(5, -2.6f, -0.25f, z - 1.0f, 0.38f, 2.2f, 0.42f, "forest_trunk_far_left_$i")
            node(5, 2.6f, -0.25f, z - 3.0f, 0.42f, 2.3f, 0.45f, "forest_trunk_far_right_$i")
            node(3, -2.6f, 1.55f, z - 1.0f, 2.7f, 2.25f, 2.55f, "forest_canopy_far_left_$i")
            node(3, 2.6f, 1.7f, z - 3.0f, 3.0f, 2.35f, 2.8f, "forest_canopy_far_right_$i")
            node(3, -5.4f, 2.35f, z - 1.0f, 1.8f, 1.45f, 1.9f, "forest_crown_far_left_$i")
            node(3, 5.4f, 2.45f, z - 3.0f, 1.9f, 1.55f, 2.0f, "forest_crown_far_right_$i")

            // Near trees use several differently sized foliage clusters instead of one box.
            node(5, -2.8f, -0.55f, z - 3.1f, 0.34f, 1.75f, 0.38f, "forest_trunk_left_$i")
            node(5, 2.8f, -0.55f, z + 2.7f, 0.36f, 1.85f, 0.4f, "forest_trunk_right_$i")
            node(5, -2.35f, 0.12f, z - 3.1f, 0.22f, 0.72f, 0.24f, "forest_branch_left_$i")
            node(5, 2.35f, 0.18f, z + 2.7f, 0.24f, 0.78f, 0.25f, "forest_branch_right_$i")
            node(3, -2.8f, 0.75f, z - 3.1f, 2.0f, 1.75f, 1.95f, "forest_canopy_left_$i")
            node(3, -3.55f, 1.35f, z - 3.0f, 1.45f, 1.45f, 1.5f, "forest_canopy_left_cluster_$i")
            node(3, -2.0f, 1.25f, z - 3.25f, 1.35f, 1.25f, 1.4f, "forest_canopy_left_outer_$i")
            node(3, 2.8f, 0.8f, z + 2.7f, 2.1f, 1.8f, 2.0f, "forest_canopy_right_$i")
            node(3, 2.0f, 1.4f, z + 2.65f, 1.5f, 1.4f, 1.55f, "forest_canopy_right_cluster_$i")
            node(3, 3.55f, 1.35f, z + 2.85f, 1.4f, 1.3f, 1.45f, "forest_canopy_right_outer_$i")
            node(3, -3.0f, 2.0f, z - 3.0f, 1.2f, 1.15f, 1.25f, "forest_crown_left_$i")
            node(3, 3.0f, 2.15f, z + 2.8f, 1.25f, 1.2f, 1.3f, "forest_crown_right_$i")

            // Low shrubs, exposed roots, fallen timber and rocks stay outside the flight lane.
            node(6, -2.3f, -1.72f, z - 1.8f, 1.25f, 0.42f, 0.85f, "forest_shrub_left_$i")
            node(6, 2.3f, -1.73f, z + 1.7f, 1.35f, 0.4f, 0.9f, "forest_shrub_right_$i")
            node(5, -3.1f, -1.82f, z + 1.4f, 1.35f, 0.12f, 0.16f, "forest_root_left_$i")
            node(5, 3.1f, -1.83f, z - 1.7f, 1.45f, 0.12f, 0.16f, "forest_root_right_$i")
            node(7, -2.0f, -1.86f, z + 1.4f, 1.0f, 0.45f, 0.8f, "forest_rock_left_$i")
            node(7, 2.0f, -1.88f, z - 2.0f, 1.25f, 0.38f, 0.95f, "forest_rock_right_$i")
            node(1, -2.6f, -1.72f, z - 3.0f, 0.08f, 0.045f, 0.7f, "forest_glow_left_$i")
            node(1, 2.6f, -1.72f, z + 2.8f, 0.08f, 0.045f, 0.7f, "forest_glow_right_$i")
            node(2, -1.5f, -1.73f, z, 0.045f, 0.035f, 0.55f, "forest_path_marker_left_$i")
            node(2, 1.5f, -1.73f, z, 0.045f, 0.035f, 0.55f, "forest_path_marker_right_$i")
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
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":2}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":3}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":4}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":5}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":6}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":7}]}
            ],
            "materials":[
              {"name":"Midnight alloy","pbrMetallicRoughness":{"baseColorFactor":[0.12,0.16,0.19,1],"metallicFactor":0.55,"roughnessFactor":0.38}},
              {"name":"Cyan energy","emissiveFactor":[0.02,0.75,1.0],"pbrMetallicRoughness":{"baseColorFactor":[0.02,0.35,0.48,1],"metallicFactor":0.25,"roughnessFactor":0.25}},
              {"name":"Magenta energy","emissiveFactor":[0.9,0.025,0.35],"pbrMetallicRoughness":{"baseColorFactor":[0.4,0.015,0.12,1],"metallicFactor":0.2,"roughnessFactor":0.3}},
              {"name":"Forest canopy","pbrMetallicRoughness":{"baseColorFactor":[0.035,0.22,0.075,1],"metallicFactor":0.0,"roughnessFactor":0.92}},
              {"name":"Forest earth","pbrMetallicRoughness":{"baseColorFactor":[0.075,0.09,0.065,1],"metallicFactor":0.0,"roughnessFactor":0.98}},
              {"name":"Bark and roots","pbrMetallicRoughness":{"baseColorFactor":[0.19,0.095,0.045,1],"metallicFactor":0.0,"roughnessFactor":0.96}},
              {"name":"Forest undergrowth","pbrMetallicRoughness":{"baseColorFactor":[0.045,0.16,0.045,1],"metallicFactor":0.0,"roughnessFactor":1.0}},
              {"name":"Weathered stone","pbrMetallicRoughness":{"baseColorFactor":[0.19,0.22,0.20,1],"metallicFactor":0.0,"roughnessFactor":0.94}}
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
