package com.neonrush.prototype3d

import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.Utils
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    companion object {
        init { Utils.init() }
    }

    private data class MovingNode(
        val entity: Int,
        val transform: FloatArray,
        val baseZ: Float
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
    private val movingCorridorNodes = mutableListOf<MovingNode>()

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
        pilotBaseY = pilotTransform[13]
        pilotBaseZ = pilotTransform[14]

        for (i in 0..6) {
            addMovingNode("guide_left_$i")
            addMovingNode("guide_right_$i")
            addMovingNode("floor_left_$i")
            addMovingNode("floor_right_$i")
        }
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
        movingCorridorNodes += MovingNode(entity, transform, transform[14])
    }

    private fun animateScene(frameTimeNanos: Long) {
        if (animationStartNanos == 0L) animationStartNanos = frameTimeNanos
        val seconds = ((frameTimeNanos - animationStartNanos) / 1_000_000_000.0).toFloat()
        val manager = modelViewer.engine.transformManager

        // Keep entity IDs, not TransformManager instances: instances can change between frames.
        val currentPilotInstance = manager.getInstance(pilotEntity)
        if (currentPilotInstance != 0) {
            pilotTransform[12] = pilotBaseX + sin(seconds * 0.82f) * 0.48f
            pilotTransform[13] = pilotBaseY + sin(seconds * 1.65f) * 0.13f
            pilotTransform[14] = pilotBaseZ + sin(seconds * 0.58f) * 0.22f
            manager.setTransform(currentPilotInstance, pilotTransform)
        }

        // Luminous corridor bars stream toward the camera and loop for a forward-flight cue.
        val travel = (seconds * 4.2f) % 27.0f
        movingCorridorNodes.forEach { moving ->
            val instance = manager.getInstance(moving.entity)
            if (instance != 0) {
                moving.transform[14] = moving.baseZ + travel
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
