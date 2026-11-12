package com.neonrush.prototype3d

import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.Utils

class MainActivity : ComponentActivity() {
    companion object {
        init { Utils.init() }
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var modelViewer: ModelViewer
    private val choreographer by lazy { Choreographer.getInstance() }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isFinishing && ::modelViewer.isInitialized) {
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
        // Fixed third-person framing for this milestone; gameplay input comes later.
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
