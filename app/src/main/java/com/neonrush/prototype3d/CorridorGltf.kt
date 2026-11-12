package com.neonrush.prototype3d

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

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
        val sphereVertices = mutableListOf<Float>()
        val sphereIndices = mutableListOf<Short>()
        val rings = 16
        val segments = 24
        for (ring in 0..rings) {
            val phi = Math.PI.toFloat() * ring / rings
            for (segment in 0..segments) {
                val theta = (2.0 * Math.PI * segment / segments).toFloat()
                // Superellipsoid profile: broad, softly bevelled armour volumes
                // instead of balloon-like spheres. Each named body part is scaled
                // independently, preserving the separate equipment attachment points.
                fun bevel(value: Float): Float =
                    kotlin.math.sign(value) * kotlin.math.abs(value).toFloat().let {
                        it.pow(0.62f)
                    }
                val rawX = kotlin.math.sin(phi) * kotlin.math.cos(theta)
                val rawY = kotlin.math.cos(phi)
                val rawZ = kotlin.math.sin(phi) * kotlin.math.sin(theta)
                val x = bevel(rawX)
                val y = bevel(rawY)
                val z = bevel(rawZ)
                val normalLength = kotlin.math.sqrt(x * x + y * y + z * z).coerceAtLeast(0.0001f)
                sphereVertices.addAll(listOf(x, y, z, x / normalLength, y / normalLength, z / normalLength))
            }
        }
        for (ring in 0 until rings) {
            for (segment in 0 until segments) {
                val a = ring * (segments + 1) + segment
                val b = a + segments + 1
                sphereIndices.addAll(listOf(a.toShort(), (a + 1).toShort(), b.toShort(), (a + 1).toShort(), (b + 1).toShort(), b.toShort()))
            }
        }
        val binary = ByteBuffer.allocate(24000).order(ByteOrder.LITTLE_ENDIAN)
        vertices.forEach(binary::putFloat)
        indices.forEach(binary::putShort)
        sphereVertices.forEach(binary::putFloat)
        sphereIndices.forEach(binary::putShort)
        val bytes = ByteArray(binary.position())
        binary.flip()
        binary.get(bytes)
        val dataUri = "data:application/octet-stream;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)

        val nodes = mutableListOf<String>()
        fun node(mesh: Int, x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, name: String? = null) {
            val nodeName = name?.let { "\"name\":\"$it\"," } ?: ""
            nodes += """{$nodeName"mesh":$mesh,"translation":[$x,$y,$z],"scale":[$sx,$sy,$sz]}"""
        }

        nodes += """{"name":"pilot_root","children":[]}"""
        nodes += """{"name":"pilot_head_root","translation":[0,0.22,-3.42],"children":[]}"""
        val headStart = nodes.size
        fun headPart(mesh: Int, x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, name: String) {
            node(mesh, x, y - 0.22f, z + 3.42f, sx, sy, sz, name)
        }
        fun pilotPart(mesh: Int, x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, name: String) {
            node(mesh, x, y, z, sx, sy, sz, name)
        }
        // Base suit only: the jetpack, weapons and upgrades remain separate equipment.
        val alloy = 8
        val cyan = 9
        val magenta = 10
        headPart(alloy, 0f, 0.22f, -3.42f, 0.37f, 0.42f, 0.34f, "pilot_helmet")
        headPart(cyan, 0f, 0.18f, -3.105f, 0.305f, 0.275f, 0.105f, "pilot_visor")
        headPart(magenta, 0.105f, 0.385f, -3.13f, 0.205f, 0.105f, 0.075f, "pilot_visor_magenta")
        headPart(cyan, -0.285f, 0.19f, -3.13f, 0.065f, 0.12f, 0.07f, "pilot_helmet_cyan_rim")
        headPart(magenta, -0.355f, 0.12f, -3.34f, 0.085f, 0.15f, 0.09f, "pilot_helmet_side_disc")
        headPart(cyan, -0.36f, 0.12f, -3.245f, 0.035f, 0.075f, 0.035f, "pilot_helmet_side_core")
        val headEnd = nodes.size
        nodes[1] = """{"name":"pilot_head_root","translation":[0,0.22,-3.42],"children":[${(headStart until headEnd).joinToString(",")}]}"""
        pilotPart(alloy, 0f, -0.19f, -3.43f, 0.19f, 0.16f, 0.19f, "pilot_neck")
        pilotPart(alloy, 0f, -0.31f, -3.43f, 0.235f, 0.075f, 0.22f, "pilot_neck_collar")
        pilotPart(alloy, 0f, -0.69f, -3.42f, 0.43f, 0.48f, 0.245f, "pilot_torso")
        pilotPart(alloy, -0.205f, -0.48f, -3.205f, 0.235f, 0.265f, 0.105f, "pilot_chest_left")
        pilotPart(alloy, 0.205f, -0.48f, -3.205f, 0.235f, 0.265f, 0.105f, "pilot_chest_right")
        pilotPart(cyan, 0f, -0.40f, -3.205f, 0.055f, 0.12f, 0.035f, "pilot_chest_core")
        pilotPart(magenta, 0f, -0.76f, -3.20f, 0.25f, 0.055f, 0.055f, "pilot_abdomen_upper_trim")
        pilotPart(alloy, 0f, -0.91f, -3.40f, 0.32f, 0.24f, 0.205f, "pilot_abdomen")
        pilotPart(alloy, -0.12f, -0.91f, -3.205f, 0.12f, 0.105f, 0.055f, "pilot_abdomen_left_plate")
        pilotPart(alloy, 0.12f, -0.91f, -3.205f, 0.12f, 0.105f, 0.055f, "pilot_abdomen_right_plate")
        pilotPart(cyan, 0f, -1.04f, -3.205f, 0.035f, 0.105f, 0.03f, "pilot_abdomen_light")
        pilotPart(alloy, 0f, -1.18f, -3.42f, 0.29f, 0.18f, 0.21f, "pilot_hip")
        pilotPart(alloy, -0.48f, -0.39f, -3.42f, 0.255f, 0.245f, 0.245f, "pilot_left_shoulder")
        pilotPart(alloy, 0.48f, -0.39f, -3.42f, 0.255f, 0.245f, 0.245f, "pilot_right_shoulder")
        pilotPart(magenta, -0.48f, -0.56f, -3.205f, 0.205f, 0.055f, 0.045f, "pilot_left_shoulder_trim")
        pilotPart(magenta, 0.48f, -0.56f, -3.205f, 0.205f, 0.055f, 0.045f, "pilot_right_shoulder_trim")
        pilotPart(cyan, -0.48f, -0.36f, -3.185f, 0.075f, 0.045f, 0.035f, "pilot_left_shoulder_light")
        pilotPart(cyan, 0.48f, -0.36f, -3.185f, 0.075f, 0.045f, 0.035f, "pilot_right_shoulder_light")
        pilotPart(alloy, -0.53f, -0.73f, -3.43f, 0.155f, 0.29f, 0.16f, "pilot_left_upper_arm")
        pilotPart(alloy, 0.53f, -0.73f, -3.43f, 0.155f, 0.29f, 0.16f, "pilot_right_upper_arm")
        pilotPart(alloy, -0.57f, -1.08f, -3.43f, 0.125f, 0.27f, 0.14f, "pilot_left_forearm")
        pilotPart(alloy, 0.57f, -1.08f, -3.43f, 0.125f, 0.27f, 0.14f, "pilot_right_forearm")
        pilotPart(cyan, -0.57f, -1.10f, -3.285f, 0.045f, 0.19f, 0.035f, "pilot_left_forearm_light")
        pilotPart(magenta, 0.57f, -1.10f, -3.285f, 0.045f, 0.19f, 0.035f, "pilot_right_forearm_trim")
        pilotPart(magenta, -0.57f, -1.31f, -3.42f, 0.145f, 0.065f, 0.15f, "pilot_left_wrist")
        pilotPart(magenta, 0.57f, -1.31f, -3.42f, 0.145f, 0.065f, 0.15f, "pilot_right_wrist")
        pilotPart(alloy, -0.57f, -1.46f, -3.43f, 0.13f, 0.14f, 0.115f, "pilot_left_hand")
        pilotPart(alloy, 0.57f, -1.46f, -3.43f, 0.13f, 0.14f, 0.115f, "pilot_right_hand")
        pilotPart(alloy, -0.205f, -1.48f, -3.42f, 0.205f, 0.32f, 0.20f, "pilot_left_thigh")
        pilotPart(alloy, 0.205f, -1.48f, -3.42f, 0.205f, 0.32f, 0.20f, "pilot_right_thigh")
        pilotPart(magenta, -0.205f, -1.50f, -3.225f, 0.055f, 0.22f, 0.045f, "pilot_left_thigh_trim")
        pilotPart(cyan, 0.205f, -1.50f, -3.225f, 0.045f, 0.22f, 0.04f, "pilot_right_thigh_light")
        pilotPart(magenta, -0.205f, -1.79f, -3.25f, 0.15f, 0.15f, 0.095f, "pilot_left_knee")
        pilotPart(magenta, 0.205f, -1.79f, -3.25f, 0.15f, 0.15f, 0.095f, "pilot_right_knee")
        pilotPart(alloy, -0.205f, -2.08f, -3.42f, 0.155f, 0.31f, 0.16f, "pilot_left_shin")
        pilotPart(alloy, 0.205f, -2.08f, -3.42f, 0.155f, 0.31f, 0.16f, "pilot_right_shin")
        pilotPart(cyan, -0.205f, -2.08f, -3.265f, 0.055f, 0.23f, 0.035f, "pilot_left_shin_light")
        pilotPart(magenta, 0.205f, -2.08f, -3.265f, 0.055f, 0.23f, 0.035f, "pilot_right_shin_trim")
        pilotPart(alloy, -0.205f, -2.39f, -3.31f, 0.18f, 0.16f, 0.30f, "pilot_left_boot")
        pilotPart(alloy, 0.205f, -2.39f, -3.31f, 0.18f, 0.16f, 0.30f, "pilot_right_boot")
        pilotPart(magenta, -0.205f, -2.51f, -3.30f, 0.17f, 0.045f, 0.27f, "pilot_left_boot_sole")
        pilotPart(magenta, 0.205f, -2.51f, -3.30f, 0.17f, 0.045f, 0.27f, "pilot_right_boot_sole")
        val environmentStartIndex = nodes.size
        nodes[0] = """{"name":"pilot_root","children":[1,${(headEnd until environmentStartIndex).joinToString(",")}]}"""

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
            "scenes":[{"nodes":[${(listOf(0) + (environmentStartIndex until nodes.size)).joinToString(",")}]}],
            "nodes":[${nodes.joinToString(",")}],
            "meshes":[
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":0}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":1}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":2}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":3}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":4}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":5}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":6}]},
              {"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":7}]},
              {"primitives":[{"attributes":{"POSITION":3,"NORMAL":4},"indices":5,"material":8}]},
              {"primitives":[{"attributes":{"POSITION":3,"NORMAL":4},"indices":5,"material":9}]},
              {"primitives":[{"attributes":{"POSITION":3,"NORMAL":4},"indices":5,"material":10}]}
            ],
            "materials":[
              {"name":"Midnight alloy","pbrMetallicRoughness":{"baseColorFactor":[0.28,0.34,0.38,1],"metallicFactor":0.22,"roughnessFactor":0.62}},
              {"name":"Cyan energy","emissiveFactor":[0.02,0.75,1.0],"pbrMetallicRoughness":{"baseColorFactor":[0.02,0.35,0.48,1],"metallicFactor":0.25,"roughnessFactor":0.25}},
              {"name":"Magenta energy","emissiveFactor":[0.9,0.025,0.35],"pbrMetallicRoughness":{"baseColorFactor":[0.4,0.015,0.12,1],"metallicFactor":0.2,"roughnessFactor":0.3}},
              {"name":"Forest canopy","pbrMetallicRoughness":{"baseColorFactor":[0.09,0.32,0.11,1],"metallicFactor":0.0,"roughnessFactor":0.92}},
              {"name":"Forest earth","pbrMetallicRoughness":{"baseColorFactor":[0.075,0.09,0.065,1],"metallicFactor":0.0,"roughnessFactor":0.98}},
              {"name":"Bark and roots","pbrMetallicRoughness":{"baseColorFactor":[0.30,0.16,0.075,1],"metallicFactor":0.0,"roughnessFactor":0.96}},
              {"name":"Forest undergrowth","pbrMetallicRoughness":{"baseColorFactor":[0.045,0.16,0.045,1],"metallicFactor":0.0,"roughnessFactor":1.0}},
              {"name":"Weathered stone","pbrMetallicRoughness":{"baseColorFactor":[0.19,0.22,0.20,1],"metallicFactor":0.0,"roughnessFactor":0.94}},
              {"name":"Pilot graphite armour","pbrMetallicRoughness":{"baseColorFactor":[0.105,0.12,0.19,1],"metallicFactor":0.48,"roughnessFactor":0.34}},
              {"name":"Pilot cyan visor and energy","emissiveFactor":[0.02,0.72,1.0],"pbrMetallicRoughness":{"baseColorFactor":[0.015,0.38,0.52,1],"metallicFactor":0.3,"roughnessFactor":0.19}},
              {"name":"Pilot magenta armour accents","emissiveFactor":[0.75,0.015,0.42],"pbrMetallicRoughness":{"baseColorFactor":[0.48,0.018,0.22,1],"metallicFactor":0.32,"roughnessFactor":0.24}}
            ],
            "buffers":[{"uri":"$dataUri","byteLength":${bytes.size}}],
            "bufferViews":[
              {"buffer":0,"byteOffset":0,"byteLength":576,"byteStride":24,"target":34962},
              {"buffer":0,"byteOffset":576,"byteLength":72,"target":34963},
              {"buffer":0,"byteOffset":648,"byteLength":10200,"byteStride":24,"target":34962},
              {"buffer":0,"byteOffset":10848,"byteLength":4608,"target":34963}
            ],
            "accessors":[
              {"bufferView":0,"byteOffset":0,"componentType":5126,"count":24,"type":"VEC3","min":[-0.5,-0.5,-0.5],"max":[0.5,0.5,0.5]},
              {"bufferView":0,"byteOffset":12,"componentType":5126,"count":24,"type":"VEC3"},
              {"bufferView":1,"byteOffset":0,"componentType":5123,"count":36,"type":"SCALAR"},
              {"bufferView":2,"byteOffset":0,"componentType":5126,"count":425,"type":"VEC3","min":[-1,-1,-1],"max":[1,1,1]},
              {"bufferView":2,"byteOffset":12,"componentType":5126,"count":425,"type":"VEC3"},
              {"bufferView":3,"byteOffset":0,"componentType":5123,"count":2304,"type":"SCALAR"}
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
