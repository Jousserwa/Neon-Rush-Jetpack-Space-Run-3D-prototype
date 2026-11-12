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

    private data class MovingNode(val entity: Int, val transform: FloatArray, val baseZ: Float)

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
    private val movingEnvironment = mutableListOf<MovingNode>()

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
        // Drag anywhere on the screen to steer. Horizontal and vertical finger
        // position map to the pilot's movement area inside the corridor.
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
        // Raise the pilot model within the corridor so it has safe clearance both above and below.\n        pilotBaseY = pilotTransform[13] + 0.55f
        pilotBaseZ = pilotTransform[14]
        currentPilotX = pilotBaseX
        currentPilotY = pilotBaseY
        desiredPilotX = 0f
        desiredPilotY = 0f

        for (i in 0..3) {
            addMovingNode("env_floor_$i")
            addMovingNode("env_ceiling_$i")
            addMovingNode("env_wall_left_$i")
            addMovingNode("env_wall_right_$i")
        }
        for (i in 0..6) {
            addMovingNode("guide_left_$i")
            addMovingNode("guide_right_$i")
            addMovingNode("floor_left_$i")
            addMovingNode("floor_right_$i")
        }
        for (i in 0..5) addMovingNode("ceiling_marker_$i")
        animationStartNanos = 0L
        animationNodesReady = true
    }

    private fun addMovingNode(name: String) {
        val entity = modelViewer.asset?.getFirstEntityByName(name) ?: 0
        if (entity == 0) return
        val manager = modelViewer.engine.transformManager
        val instance = manager.getInstance(entity)
        if (instance == 0) return
        val transform = FloatArray(16)
        manager.getTransform(instance, transform)
        movingEnvironment += MovingNode(entity, transform, transform[14])
    }

    private fun animateScene(frameTimeNanos: Long) {
        if (animationStartNanos == 0L) animationStartNanos = frameTimeNanos
        val seconds = ((frameTimeNanos - animationStartNanos) / 1_000_000_000.0).toFloat()
        val manager = modelViewer.engine.transformManager

        // Smoothly follow finger input so the pilot can steer in four directions.
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

        // The complete corridor shell and its luminous details stream toward the camera.
        // Each environment segment wraps at the far end, keeping four sections in motion.
        val travel = (seconds * 5.0f) % 36.0f
        movingEnvironment.forEach { moving ->
            val instance = manager.getInstance(moving.entity)
            if (instance != 0) {
                var z = moving.baseZ + travel
                if (moving.entity != 0 && moving.baseZ < -30f) {
                    // Fixed far-end wall is intentionally excluded from the scrolling set.
                }
                if (z > 2f) z -= 36f
                moving.transform[14] = z
                manager.setTransform(instance, moving.transform)
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
            modelViewer.destroyModel()
            modelViewer.engine.destroy()
        }
        super.onDestroy()
    }
}
