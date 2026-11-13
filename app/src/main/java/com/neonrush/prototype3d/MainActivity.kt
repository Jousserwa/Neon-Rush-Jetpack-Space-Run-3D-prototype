package com.neonrush.prototype3d

import android.os.Bundle
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import com.google.android.filament.View
import com.google.android.filament.Skybox
import com.google.android.filament.LightManager
import com.google.android.filament.EntityManager
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.Utils
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import android.util.Log
import java.nio.ByteBuffer

class MainActivity : ComponentActivity() {
    companion object { init { Utils.init() } }

    private data class MovingNode(
        val entity: Int,
        val transform: FloatArray,
        val baseX: Float,
        val baseZ: Float,
        val name: String
    )

    private lateinit var surfaceView: SurfaceView
    private lateinit var modelViewer: ModelViewer
    private val choreographer by lazy { Choreographer.getInstance() }
    private var animationStartNanos = 0L
    private var animationNodesReady = false
    private var pilotEntity = 0
    private var pilotTransform = FloatArray(16)
    private var pilotHeadEntity = 0
    private var pilotHeadTransform = FloatArray(16)
    private var importedPilotAsset: FilamentAsset? = null
    private var importedPilotLoader: AssetLoader? = null
    private var importedPilotMaterials: UbershaderProvider? = null
    private var pilotBaseX = 0f
    private var pilotBaseY = 0f
    private var pilotBaseZ = 0f
    private var desiredPilotX = 0f
    private var desiredPilotY = 0f
    private var currentPilotX = 0f
    private var currentPilotY = 0f
    private val corridorNodes = mutableListOf<MovingNode>()
    private val doorNodes = mutableListOf<MovingNode>()
    private val hangarNodes = mutableListOf<MovingNode>()
    private val forestNodes = mutableListOf<MovingNode>()
    private var transitionComplete = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isFinishing && ::modelViewer.isInitialized) {
                prepareAnimationNodes()
                if (animationNodesReady) animateScene(frameTimeNanos)
                modelViewer.render(frameTimeNanos)
                choreographer.postFrameCallback(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5, 8, 18)
        window.navigationBarColor = android.graphics.Color.rgb(5, 8, 18)
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        // Drag anywhere to steer the pilot left/right and up/down.
        surfaceView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val width = view.width.coerceAtLeast(1).toFloat()
                    val height = view.height.coerceAtLeast(1).toFloat()
                    val nx = (event.x / width - 0.5f) * 2f
                    val ny = (0.5f - event.y / height) * 2f
                    desiredPilotX = (nx * 1.45f).coerceIn(-1.45f, 1.45f)
                    desiredPilotY = (ny * 1.05f).coerceIn(-1.05f, 1.05f)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> true
            }
        }

        val gameplayCamera = Manipulator.Builder()
            .targetPosition(0f, 0.15f, -7f)
            .orbitHomePosition(0f, 1.15f, 2.8f)
            .upVector(0f, 1f, 0f)
            .fovDegrees(48f)
            .farPlane(100f)
            .build(Manipulator.Mode.ORBIT)
        modelViewer = ModelViewer(surfaceView, manipulator = gameplayCamera)
        // A softly lit woodland sky prevents the open world from disappearing into black.
        modelViewer.scene.skybox = Skybox.Builder()
            .color(0.16f, 0.29f, 0.19f, 1.0f)
            .build(modelViewer.engine)

        // Soft directional daylight makes PBR foliage, bark, rocks and pilot armour visible.
        val sunlight = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.88f, 0.96f, 0.82f)
            .intensity(70000.0f)
            .direction(-0.35f, -1.0f, -0.28f)
            .castShadows(false)
            .build(modelViewer.engine, sunlight)
        modelViewer.scene.addEntity(sunlight)

        // Front fill: the route-facing side of the imported pilot points toward -Z.
        // This light comes from ahead of the pilot so the opening dark corridor does not
        // leave the character as a black silhouette.
        val pilotFrontFill = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.72f, 0.84f, 1.0f)
            .intensity(42000.0f)
            .direction(0.0f, -0.18f, -1.0f)
            .castShadows(false)
            .build(modelViewer.engine, pilotFrontFill)
        modelViewer.scene.addEntity(pilotFrontFill)

        modelViewer.view.renderQuality = modelViewer.view.renderQuality.apply {
            hdrColorBuffer = View.QualityLevel.MEDIUM
        }
        modelViewer.view.antiAliasing = View.AntiAliasing.FXAA
        modelViewer.view.bloomOptions = modelViewer.view.bloomOptions.apply { enabled = true }
        modelViewer.loadModelGltf(CorridorGltf.create()) { uri -> CorridorGltf.resource(uri) }
        loadImportedPilot()
    }

    /**
     * Loads the uploaded rigged glTF character into the existing Filament scene.
     * The procedural corridor / hangar / forest remains the environment; only its old
     * placeholder pilot is hidden after the imported model has loaded successfully.
     */
    private fun loadImportedPilot() {
        val engine = modelViewer.engine
        var resourceLoader: ResourceLoader? = null
        try {
            val gltfBytes = assets.open("pilot/Superhero_Male_FullBody.gltf").use { it.readBytes() }
            val materials = UbershaderProvider(engine)
            val loader = AssetLoader(engine, materials, EntityManager.get())
            val character = loader.createAsset(ByteBuffer.wrap(gltfBytes))
                ?: throw IllegalStateException("Filament could not parse Superhero_Male_FullBody.gltf")

            resourceLoader = ResourceLoader(engine, true)
            for (uri in character.resourceUris) {
                val bytes = assets.open("pilot/$uri").use { it.readBytes() }
                resourceLoader.addResourceData(uri, ByteBuffer.wrap(bytes))
            }
            resourceLoader.loadResources(character)
            resourceLoader.destroy()
            resourceLoader = null

            // Add the imported character to the same scene as the existing environment.
            modelViewer.scene.addEntities(character.entities)

            val transformManager = engine.transformManager
            val rootInstance = transformManager.getInstance(character.root)
            if (rootInstance == 0) {
                loader.destroyAsset(character)
                loader.destroy()
                materials.destroyMaterials()
                materials.destroy()
                throw IllegalStateException("Imported character root has no transform component")
            }

            // The source model is approximately 1.81 units tall and stands on Y=0.
            // Position it at the existing pilot's gameplay depth and floor level.
            val characterTransform = FloatArray(16)
            // The source character faces the camera (+Z). Turn it toward the
            // forward route (-Z), away from the gameplay camera.
            characterTransform[0] = -1.0f
            characterTransform[5] = 1.0f
            characterTransform[10] = -1.0f
            characterTransform[12] = 0.0f
            characterTransform[13] = -0.85f
            characterTransform[14] = -3.42f
            characterTransform[15] = 1.0f
            transformManager.setTransform(rootInstance, characterTransform)

            // Hide the old primitive robot only after the real glTF has loaded and entered the scene.
            val environmentAsset = modelViewer.asset
            val oldPilotRoot = environmentAsset?.getFirstEntityByName("pilot_root") ?: 0
            val oldPilotInstance = if (oldPilotRoot != 0) transformManager.getInstance(oldPilotRoot) else 0
            if (oldPilotInstance != 0) {
                val hidden = FloatArray(16)
                hidden[15] = 1.0f
                transformManager.setTransform(oldPilotInstance, hidden)
            }

            importedPilotMaterials = materials
            importedPilotLoader = loader
            importedPilotAsset = character
            Log.i("NeonRushPilot", "Imported rigged pilot loaded. External resources: ${character.resourceUris.size}")
        } catch (error: Exception) {
            resourceLoader?.destroy()
            Log.e("NeonRushPilot", "Imported pilot failed to load; placeholder remains visible.", error)
        }
    }

    private fun prepareAnimationNodes() {
        if (animationNodesReady) return
        val environmentAsset = modelViewer.asset ?: return
        val characterAsset = importedPilotAsset ?: return
        val manager = modelViewer.engine.transformManager

        pilotEntity = characterAsset.root
        if (pilotEntity == 0) return
        val pilotInstance = manager.getInstance(pilotEntity)
        if (pilotInstance == 0) return
        pilotTransform = FloatArray(16)
        manager.getTransform(pilotInstance, pilotTransform)
        pilotBaseX = pilotTransform[12]
        pilotBaseY = pilotTransform[13]
        pilotBaseZ = pilotTransform[14]
        currentPilotX = pilotBaseX
        currentPilotY = pilotBaseY
        desiredPilotX = 0f
        desiredPilotY = 0f

        // The imported character has a skeleton but this particular glTF has no animation clips.
        // Keep the old head-pivot animation disabled; steering moves the complete rigged character.
        pilotHeadEntity = 0

        for (i in 0..3) {
            addMovingNode("env_floor_$i", corridorNodes)
            addMovingNode("env_ceiling_$i", corridorNodes)
            addMovingNode("env_wall_left_$i", corridorNodes)
            addMovingNode("env_wall_right_$i", corridorNodes)
        }
        for (i in 0..6) {
            addMovingNode("guide_left_$i", corridorNodes)
            addMovingNode("guide_right_$i", corridorNodes)
            addMovingNode("floor_left_$i", corridorNodes)
            addMovingNode("floor_right_$i", corridorNodes)
        }
        for (i in 0..5) addMovingNode("ceiling_marker_$i", corridorNodes)

        listOf(
            "door_frame_left", "door_frame_right", "door_frame_top",
            "door_left_panel", "door_right_panel", "door_status_light"
        ).forEach { addMovingNode(it, doorNodes) }

        for (i in 0..3) {
            listOf(
                "hangar_floor_$i", "hangar_ceiling_$i",
                "hangar_wall_left_$i", "hangar_wall_right_$i",
                "hangar_pillar_left_$i", "hangar_pillar_right_$i",
                "hangar_light_left_$i", "hangar_light_right_$i",
                "hangar_floor_light_left_$i", "hangar_floor_light_right_$i",
                "hangar_arch_left_$i", "hangar_arch_right_$i", "hangar_arch_top_$i",
                "hangar_platform_left_$i", "hangar_platform_right_$i",
                "hangar_platform_edge_left_$i", "hangar_platform_edge_right_$i",
                "hangar_rail_left_$i", "hangar_rail_right_$i",
                "hangar_equipment_left_$i", "hangar_equipment_right_$i"
            ).forEach { addMovingNode(it, hangarNodes) }
        }

        for (i in 0..3) {
            listOf(
                "forest_ground_$i", "forest_moss_left_$i", "forest_moss_right_$i",
                "forest_earth_mound_left_$i", "forest_earth_mound_right_$i",
                "forest_trunk_far_left_$i", "forest_trunk_far_right_$i",
                "forest_canopy_far_left_$i", "forest_canopy_far_right_$i",
                "forest_crown_far_left_$i", "forest_crown_far_right_$i",
                "forest_trunk_left_$i", "forest_trunk_right_$i",
                "forest_branch_left_$i", "forest_branch_right_$i",
                "forest_canopy_left_$i", "forest_canopy_left_cluster_$i", "forest_canopy_left_outer_$i",
                "forest_canopy_right_$i", "forest_canopy_right_cluster_$i", "forest_canopy_right_outer_$i",
                "forest_crown_left_$i", "forest_crown_right_$i",
                "forest_shrub_left_$i", "forest_shrub_right_$i",
                "forest_root_left_$i", "forest_root_right_$i",
                "forest_rock_left_$i", "forest_rock_right_$i",
                "forest_glow_left_$i", "forest_glow_right_$i",
                "forest_path_marker_left_$i", "forest_path_marker_right_$i"
            ).forEach { addMovingNode(it, forestNodes) }
        }

        animationStartNanos = 0L
        animationNodesReady = true
    }

    private fun addMovingNode(name: String, destination: MutableList<MovingNode>) {
        val entity = modelViewer.asset?.getFirstEntityByName(name) ?: 0
        if (entity == 0) return
        val manager = modelViewer.engine.transformManager
        val instance = manager.getInstance(entity)
        if (instance == 0) return
        val transform = FloatArray(16)
        manager.getTransform(instance, transform)
        destination += MovingNode(entity, transform, transform[12], transform[14], name)
    }

    private fun applyNodeTransform(node: MovingNode, x: Float, z: Float) {
        val manager = modelViewer.engine.transformManager
        val instance = manager.getInstance(node.entity)
        if (instance == 0) return
        node.transform[12] = x
        node.transform[14] = z
        manager.setTransform(instance, node.transform)
    }

    private fun animateScene(frameTimeNanos: Long) {
        if (animationStartNanos == 0L) animationStartNanos = frameTimeNanos
        val seconds = ((frameTimeNanos - animationStartNanos) / 1_000_000_000.0).toFloat()
        val manager = modelViewer.engine.transformManager

        // Smooth four-direction pilot steering.
        val currentPilotInstance = manager.getInstance(pilotEntity)
        if (currentPilotInstance != 0) {
            val smoothing = 1f - kotlin.math.exp(-seconds.coerceAtMost(0.05f) * 10f)
            currentPilotX += (pilotBaseX + desiredPilotX - currentPilotX) * smoothing
            currentPilotY += (pilotBaseY + desiredPilotY - currentPilotY) * smoothing
            pilotTransform[12] = currentPilotX
            pilotTransform[13] = currentPilotY
            pilotTransform[14] = pilotBaseZ
            manager.setTransform(currentPilotInstance, pilotTransform)
        }

        // Subtle three-quarter glance, with tiny idle drift and steering response.
        val headInstance = manager.getInstance(pilotHeadEntity)
        if (headInstance != 0 && pilotHeadTransform.size == 16) {
            val yaw = 0.22f + (currentPilotX - pilotBaseX) * 0.035f +
                kotlin.math.sin(seconds * 1.15f) * 0.012f
            val c = kotlin.math.cos(yaw)
            val s = kotlin.math.sin(yaw)
            pilotHeadTransform[0] = c
            pilotHeadTransform[2] = -s
            pilotHeadTransform[8] = s
            pilotHeadTransform[10] = c
            manager.setTransform(headInstance, pilotHeadTransform)
        }

        // Continuous forward travel. The door opens as the pilot approaches,
        // then the corridor gives way to a repeating sequence of hangar bays.
        val travel = seconds * 5f
        val openProgress = ((seconds - 3.8f) / 1.8f).coerceIn(0f, 1f)
        val easedOpen = openProgress * openProgress * (3f - 2f * openProgress)
        val hasEnteredHangar = seconds >= 7.2f

        if (!hasEnteredHangar) {
            corridorNodes.forEach { node ->
                var z = node.baseZ + travel
                while (z > 2f) z -= 36f
                applyNodeTransform(node, node.baseX, z)
            }
        } else {
            // Keep the corridor behind the player after entering the hangar.
            corridorNodes.forEach { node ->
                applyNodeTransform(node, node.baseX, node.baseZ - 120f)
            }
        }

        // Door panels slide apart, then the open doorway travels behind the pilot.
        doorNodes.forEach { node ->
            var x = node.baseX
            if (node.name == "door_left_panel") x -= 1.35f * easedOpen
            if (node.name == "door_right_panel") x += 1.35f * easedOpen
            applyNodeTransform(node, x, node.baseZ + travel)
        }

        // The hangar runs after the corridor. At 26 seconds, the route opens
        // into an outdoor forest and the hangar falls behind the player.
        val hasEnteredForest = seconds >= 26f
        if (!hasEnteredForest) {
            hangarNodes.forEach { node ->
                var z = node.baseZ + travel
                while (z > 5f) z -= 36f
                applyNodeTransform(node, node.baseX, z)
            }
        } else {
            hangarNodes.forEach { node ->
                applyNodeTransform(node, node.baseX, node.baseZ - 160f)
            }
        }

        // Forest sections begin approaching before the threshold, then recycle
        // continuously as the player flies along the open woodland route.
        forestNodes.forEach { node ->
            if (!hasEnteredForest) {
                applyNodeTransform(node, node.baseX, node.baseZ + travel)
            } else {
                var z = node.baseZ + travel
                while (z > 5f) z -= 36f
                applyNodeTransform(node, node.baseX, z)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::modelViewer.isInitialized) choreographer.postFrameCallback(frameCallback)
    }

    override fun onPause() {
        choreographer.removeFrameCallback(frameCallback)
        super.onPause()
    }

    override fun onDestroy() {
        choreographer.removeFrameCallback(frameCallback)
        if (::modelViewer.isInitialized) {
            importedPilotAsset?.let { character ->
                modelViewer.scene.removeEntities(character.entities)
                importedPilotLoader?.destroyAsset(character)
            }
            importedPilotAsset = null
            importedPilotLoader?.destroy()
            importedPilotLoader = null
            importedPilotMaterials?.destroyMaterials()
            importedPilotMaterials?.destroy()
            importedPilotMaterials = null
            modelViewer.destroyModel()
            modelViewer.engine.destroy()
        }
        super.onDestroy()
    }
}
