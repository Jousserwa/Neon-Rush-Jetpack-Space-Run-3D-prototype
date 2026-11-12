package com.neonrush.prototype3d

import android.os.Bundle
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.Utils

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
        modelViewer.view.renderQuality = modelViewer.view.renderQuality.apply {
            hdrColorBuffer = View.QualityLevel.MEDIUM
        }
        modelViewer.view.antiAliasing = View.AntiAliasing.FXAA
        modelViewer.view.bloomOptions = modelViewer.view.bloomOptions.apply { enabled = true }
        modelViewer.loadModelGltf(CorridorGltf.create()) { uri -> CorridorGltf.resource(uri) }
    }

    private fun prepareAnimationNodes() {
        if (animationNodesReady) return
        val asset = modelViewer.asset ?: return
        val manager = modelViewer.engine.transformManager

        pilotEntity = asset.getFirstEntityByName("pilot_root")
        if (pilotEntity == 0) return
        val pilotInstance = manager.getInstance(pilotEntity)
        if (pilotInstance == 0) return
        pilotTransform = FloatArray(16)
        manager.getTransform(pilotInstance, pilotTransform)
        pilotBaseX = pilotTransform[12]
        pilotBaseY = pilotTransform[13] + 0.55f
        pilotBaseZ = pilotTransform[14]
        currentPilotX = pilotBaseX
        currentPilotY = pilotBaseY
        desiredPilotX = 0f
        desiredPilotY = 0f

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

        listOf(
            "hangar_floor", "hangar_ceiling", "hangar_wall_left", "hangar_wall_right"
        ).forEach { addMovingNode(it, hangarNodes) }
        for (i in 0..2) {
            addMovingNode("hangar_pillar_left_$i", hangarNodes)
            addMovingNode("hangar_pillar_right_$i", hangarNodes)
        }
        for (i in 0..3) {
            addMovingNode("hangar_light_left_$i", hangarNodes)
            addMovingNode("hangar_light_right_$i", hangarNodes)
            addMovingNode("hangar_floor_light_left_$i", hangarNodes)
            addMovingNode("hangar_floor_light_right_$i", hangarNodes)
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

        // The corridor streams toward the pilot until the door-to-hangar transition finishes.
        val travel = (seconds * 5f).coerceAtMost(36f)
        val openProgress = ((seconds - 3.8f) / 1.8f).coerceIn(0f, 1f)
        val easedOpen = openProgress * openProgress * (3f - 2f * openProgress)

        if (seconds < 7.2f) {
            corridorNodes.forEach { node ->
                var z = node.baseZ + seconds * 5f
                if (z > 2f) z -= 36f
                applyNodeTransform(node, node.baseX, z)
            }
            doorNodes.forEach { node ->
                var x = node.baseX
                if (node.name == "door_left_panel") x -= 1.35f * easedOpen
                if (node.name == "door_right_panel") x += 1.35f * easedOpen
                applyNodeTransform(node, x, node.baseZ + travel)
            }
            hangarNodes.forEach { node ->
                applyNodeTransform(node, node.baseX, node.baseZ + travel)
            }
        } else if (!transitionComplete) {
            // Clear the corridor behind the pilot and settle the hangar around the player.
            corridorNodes.forEach { node -> applyNodeTransform(node, node.baseX, node.baseZ - 100f) }
            doorNodes.forEach { node ->
                var x = node.baseX
                if (node.name == "door_left_panel") x -= 1.35f
                if (node.name == "door_right_panel") x += 1.35f
                applyNodeTransform(node, x, node.baseZ + 36f)
            }
            hangarNodes.forEach { node -> applyNodeTransform(node, node.baseX, node.baseZ + 36f) }
            transitionComplete = true
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
            modelViewer.destroyModel()
            modelViewer.engine.destroy()
        }
        super.onDestroy()
    }
}
