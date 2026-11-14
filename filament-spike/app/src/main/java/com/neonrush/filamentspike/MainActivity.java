package com.neonrush.filamentspike;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.filament.Box;
import com.google.android.filament.Engine;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.Skybox;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.Gltfio;
import com.google.android.filament.utils.ModelViewer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

public final class MainActivity extends Activity implements Choreographer.FrameCallback {
    private ModelViewer viewer;
    private TextView status;
    private boolean running;
    private long windowStart;
    private int callbacks;
    private int renderedFrames;
    private int skippedFrames;
    private String modelInfo = "Waiting for GLB diagnostics…";

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        FrameLayout root = new FrameLayout(this);
        SurfaceView surface = new SurfaceView(this);
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));

        status = new TextView(this);
        status.setTextColor(-1);
        status.setTextSize(11);
        status.setPadding(18, 14, 18, 14);
        status.setBackgroundColor(0xDD050A12);
        status.setText("NEON RUSH • FILAMENT DIAGNOSTIC\nLoading corridor…");

        ScrollView panel = new ScrollView(this);
        panel.setFillViewport(false);
        panel.addView(status);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                -1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.topMargin = 12;
        lp.leftMargin = 12;
        lp.rightMargin = 12;
        root.addView(panel, lp);
        setContentView(root);

        try {
            Gltfio.init();
            viewer = new ModelViewer(surface, Engine.create(),
                    new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK), null);
            viewer.getScene().setSkybox(new Skybox.Builder().build(viewer.getEngine()));
            viewer.getScene().getSkybox().setColor(.008f, .015f, .028f, 1f);

            byte[] encoded = readAsset("corridor.glb.b64");
            byte[] glb = Base64.decode(
                    new String(encoded, java.nio.charset.StandardCharsets.US_ASCII),
                    Base64.DEFAULT);
            viewer.loadModelGlb(ByteBuffer.wrap(glb));

            FilamentAsset asset = viewer.getAsset();
            if (asset == null) {
                throw new IllegalStateException("Filament could not create the GLB asset");
            }
            modelInfo = diagnoseAndFitCamera(asset);
            updateStatus("NEON RUSH • FILAMENT DIAGNOSTIC");
        } catch (Throwable e) {
            showFailure("INITIALIZATION", e);
        }
    }

    private String diagnoseAndFitCamera(FilamentAsset asset) {
        Box bounds = asset.getBoundingBox();
        float[] center = bounds.getCenter();
        float[] half = bounds.getHalfExtent();
        float radius = (float) Math.sqrt(
                half[0] * half[0] + half[1] * half[1] + half[2] * half[2]);

        // Fit the complete asset from its +Z side without modifying its scale or root transform.
        double distance = Math.max(16.0, radius * 2.75);
        double near = Math.max(0.05, distance - radius - 0.5);
        double far = distance + radius + 0.5;
        viewer.getCamera().lookAt(
                center[0], center[1], center[2] + distance,
                center[0], center[1], center[2],
                0.0, 1.0, 0.0);
        viewer.setCameraNear((float) near);
        viewer.setCameraFar((float) far);

        int[] renderableEntities = asset.getRenderableEntities();
        RenderableManager renderables = viewer.getEngine().getRenderableManager();
        StringBuilder details = new StringBuilder();
        details.append(String.format(Locale.US,
                "Bounds center %s\nHalf extent %s\nSize %.2f × %.2f × %.2f\n",
                Arrays.toString(center), Arrays.toString(half),
                half[0] * 2f, half[1] * 2f, half[2] * 2f));
        details.append(String.format(Locale.US,
                "Camera distance %.2f | near %.2f | far %.2f\n",
                distance, near, far));
        details.append("Renderable entities: ").append(renderableEntities.length).append('\n');

        for (int entity : renderableEntities) {
            try {
                String name = asset.getName(entity);
                RenderableManager.Instance instance = renderables.getInstance(entity);
                int primitiveCount = renderables.getPrimitiveCount(instance);
                details.append("Node ").append(name == null ? "(unnamed)" : name)
                        .append(" | primitives ").append(primitiveCount).append('\n');
                for (int p = 0; p < primitiveCount; p++) {
                    MaterialInstance material = renderables.getMaterialInstanceAt(instance, p);
                    details.append("  [").append(p).append("] material ")
                            .append(material == null ? "(none)" : material.getName())
                            .append('\n');
                }
            } catch (Throwable e) {
                details.append("Entity ").append(entity)
                        .append(" inspection failed: ").append(e.getClass().getSimpleName())
                        .append('\n');
                Log.w("NeonRushFilament", "Renderable inspection failed for " + entity, e);
            }
        }

        String result = details.toString();
        Log.i("NeonRushFilament", result);
        return result;
    }

    private void updateStatus(String heading) {
        status.setText(heading + "\nFixed bounds-fit camera\n"
                + "FPS accepted: waiting\n" + modelInfo);
    }

    private void showFailure(String stage, Throwable e) {
        running = false;
        String detail = Log.getStackTraceString(e);
        status.setText("FILAMENT DIAGNOSTIC — " + stage + " FAILED\n\n"
                + e.getClass().getName() + ": " + String.valueOf(e.getMessage())
                + "\n\n" + detail);
        Log.e("NeonRushFilament", stage + " failed", e);
    }

    private byte[] readAsset(String n) throws Exception {
        try (InputStream in = getAssets().open(n);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[4096];
            int k;
            while ((k = in.read(b)) > 0) out.write(b, 0, k);
            return out.toByteArray();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewer != null && !running) {
            running = true;
            windowStart = System.nanoTime();
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    @Override
    public void doFrame(long t) {
        if (!running || viewer == null) return;
        try {
            callbacks++;
            if (viewer.render(t)) renderedFrames++;
            else skippedFrames++;

            long elapsed = t - windowStart;
            if (elapsed >= 1_000_000_000L) {
                int callbackFps = Math.round(callbacks * 1_000_000_000f / elapsed);
                int acceptedFps = Math.round(renderedFrames * 1_000_000_000f / elapsed);
                int skippedFps = Math.round(skippedFrames * 1_000_000_000f / elapsed);
                status.setText("NEON RUSH • FILAMENT DIAGNOSTIC\n"
                        + String.format(Locale.US,
                        "Fixed bounds-fit camera • accepted %d FPS\nChoreographer %d FPS | skipped %d/s\n",
                        acceptedFps, callbackFps, skippedFps)
                        + modelInfo);
                callbacks = 0;
                renderedFrames = 0;
                skippedFrames = 0;
                windowStart = t;
            }
            Choreographer.getInstance().postFrameCallback(this);
        } catch (Throwable e) {
            showFailure("RENDER LOOP", e);
        }
    }

    @Override
    protected void onPause() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        running = false;
        if (viewer != null) {
            try {
                viewer.destroy();
            } catch (Throwable e) {
                Log.e("NeonRushFilament", "Destroy failed", e);
            }
            viewer = null;
        }
        super.onDestroy();
    }
}
