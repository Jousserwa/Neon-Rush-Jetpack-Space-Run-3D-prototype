package com.neonrush.filamentspike;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.filament.Box;
import com.google.android.filament.Engine;
import com.google.android.filament.IndirectLight;
import com.google.android.filament.LightManager;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.Skybox;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.Gltfio;
import com.google.android.filament.utils.ModelViewer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public final class MainActivity extends Activity implements Choreographer.FrameCallback {
    private ModelViewer viewer;
    private IndirectLight ambientLight;
    private TextView status;
    private ScrollView detailsPanel;
    private TextView compactStatus;
    private TextView detailsToggle;
    private TextView sunToggle;
    private boolean detailsExpanded = false;
    private boolean sunEnabled = true;
    private int sunInstance = -1;
    private final float sunIntensityLux = 100000.0f;
    private boolean running;
    private long windowStart;
    private int callbacks;
    private int renderedFrames;
    private int skippedFrames;
    private long renderCpuNanosSum;
    private long renderCpuNanosMax;
    private int renderCpuSamples;
    private String ceilingTestMessage = "P2 ceiling test not applied";
    private String modelInfo = "Waiting for GLB diagnostics…";
    private SurfaceView renderSurface;
    private final float[] skyboxColor = {0.035f, 0.075f, 0.14f, 1.0f};
    private final float[] sunDirection = {-0.324f, -0.811f, -0.487f};
    private final float ambientIntensity = 3000.0f;
    private final double eyeX = 0.0, eyeY = 1.6, eyeZ = 2.0;
    private final double targetX = 0.0, targetY = 1.6, targetZ = -9.0;
    private final double upX = 0.0, upY = 1.0, upZ = 0.0;
    private final double verticalFov = 60.0;
    private final double nearPlane = 0.1, farPlane = 50.0;
    private volatile int surfaceWidth, surfaceHeight;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        FrameLayout root = new FrameLayout(this);
        SurfaceView surface = new SurfaceView(this);
        renderSurface = surface;
        root.addView(surface, new FrameLayout.LayoutParams(-1, -1));
        surface.addOnLayoutChangeListener((v, left, top, right, bottom,
                                           oldLeft, oldTop, oldRight, oldBottom) -> {
            surfaceWidth = right - left;
            surfaceHeight = bottom - top;
        });

        LinearLayout overlay = new LinearLayout(this);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setPadding(dp(5), dp(5), dp(5), dp(5));
        overlay.setBackgroundColor(0x99050A12);
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        compactStatus = new TextView(this);
        compactStatus.setTextColor(-1);
        compactStatus.setTextSize(10);
        compactStatus.setPadding(dp(7), dp(7), dp(7), dp(7));
        compactStatus.setText("BUILD 35 | FPS --");
        detailsToggle = new TextView(this);
        detailsToggle.setTextColor(0xFF7FEAFF);
        detailsToggle.setTextSize(10);
        detailsToggle.setPadding(dp(8), dp(7), dp(8), dp(7));
        detailsToggle.setText("DETAILS +");
        detailsToggle.setOnClickListener(v -> {
            detailsExpanded = !detailsExpanded;
            detailsToggle.setText(detailsExpanded ? "DETAILS −" : "DETAILS +");
            detailsPanelVisibility();
        });
        sunToggle = new TextView(this);
        sunToggle.setTextColor(0xFFFFD18A);
        sunToggle.setTextSize(10);
        sunToggle.setPadding(dp(8), dp(7), dp(8), dp(7));
        sunToggle.setText("SUN ON");
        sunToggle.setOnClickListener(v -> setSunEnabled(!sunEnabled));
        controls.addView(compactStatus, new LinearLayout.LayoutParams(0, -2, 1f));
        controls.addView(detailsToggle, new LinearLayout.LayoutParams(-2, -2));
        controls.addView(sunToggle, new LinearLayout.LayoutParams(-2, -2));
        overlay.addView(controls, new LinearLayout.LayoutParams(-1, -2));
        status = new TextView(this);
        status.setTextColor(-1);
        status.setTextSize(9);
        status.setPadding(dp(10), dp(8), dp(10), dp(8));
        status.setText("NEON RUSH • FILAMENT DIAGNOSTIC\\nLoading corridor…");
        detailsPanel = new ScrollView(this);
        detailsPanel.setFillViewport(false);
        detailsPanel.addView(status);
        detailsPanel.setVisibility(android.view.View.GONE);
        overlay.addView(detailsPanel, new LinearLayout.LayoutParams(-1, dp(260)));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dp(340), -2, Gravity.TOP | Gravity.END);
        lp.topMargin = dp(10);
        lp.rightMargin = dp(10);
        root.addView(overlay, lp);
        setContentView(root);

        try {
            Gltfio.init();
            viewer = new ModelViewer(surface, Engine.create(),
                    new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK), null);
            Engine engine = viewer.getEngine();
            viewer.getScene().setSkybox(new Skybox.Builder().build(engine));
            viewer.getScene().getSkybox().setColor(
                    skyboxColor[0], skyboxColor[1], skyboxColor[2], skyboxColor[3]);

            // Explicit sunny-f/16 exposure for the existing 100,000-lux ModelViewer SUN.
            viewer.getCamera().setExposure(16.0f, 1.0f / 125.0f, 100.0f);

            // Aim the existing SUN down and forward through the corridor, not straight down.
            LightManager lightManager = engine.getLightManager();
            sunInstance = lightManager.getInstance(viewer.getLight());
            lightManager.setDirection(sunInstance,
                    sunDirection[0], sunDirection[1], sunDirection[2]);

            // Constant low-blue ambient SH: diffuse fill only, not a reflection cubemap.
            float sh0 = 0.282095f;
            ambientLight = new IndirectLight.Builder()
                    .irradiance(1, new float[] { sh0 * 0.55f, sh0 * 0.68f, sh0 * 0.85f })
                    .intensity(ambientIntensity)
                    .build(engine);
            viewer.getScene().setIndirectLight(ambientLight);

            byte[] encoded = readAsset("corridor.glb.b64");
            byte[] glb = Base64.decode(
                    new String(encoded, StandardCharsets.US_ASCII), Base64.DEFAULT);
            viewer.loadModelGlb(ByteBuffer.wrap(glb));

            FilamentAsset asset = viewer.getAsset();
            if (asset == null) {
                throw new IllegalStateException("Filament could not create the GLB asset");
            }
            String glbInfo = inspectGlbJson(glb);
            modelInfo = diagnoseAndFitCamera(asset) + glbInfo;
            updateStatus("NEON RUSH • FILAMENT DIAGNOSTIC");
        } catch (Throwable e) {
            showFailure("INITIALIZATION", e);
        }
    }

    private String diagnoseAndFitCamera(FilamentAsset asset) {
        Box bounds = asset.getBoundingBox();
        float[] center = bounds.getCenter();
        float[] half = bounds.getHalfExtent();

        viewer.getCamera().lookAt(
                eyeX, eyeY, eyeZ, targetX, targetY, targetZ, upX, upY, upZ);
        viewer.setCameraNear((float) nearPlane);
        viewer.setCameraFar((float) farPlane);
        if (renderSurface != null) {
            renderSurface.post(() -> {
                int w = renderSurface.getWidth(), h = renderSurface.getHeight();
                if (w > 0 && h > 0 && viewer != null) {
                    viewer.getCamera().setProjection(verticalFov, (double) w / h,
                            nearPlane, farPlane,
                            com.google.android.filament.Camera.Fov.VERTICAL);
                    surfaceWidth = w;
                    surfaceHeight = h;
                }
            });
        }

        int[] entities = asset.getRenderableEntities();
        RenderableManager rm = viewer.getEngine().getRenderableManager();
        StringBuilder out = new StringBuilder();
        out.append(String.format(Locale.US,
                "Bounds center %s | half %s\nSize %.2f × %.2f × %.2f\n",
                Arrays.toString(center), Arrays.toString(half),
                half[0] * 2f, half[1] * 2f, half[2] * 2f));
        out.append(String.format(Locale.US,
                "Camera Z %.2f → %.2f | near %.2f | far %.2f\n",
                eyeZ, targetZ, nearPlane, farPlane));
        out.append("Renderable entities: ").append(entities.length).append('\n');

        for (int entity : entities) {
            try {
                int instance = rm.getInstance(entity);
                int count = rm.getPrimitiveCount(instance);
                out.append("Node ").append(asset.getName(entity))
                        .append(" | primitives ").append(count).append('\n');
                for (int p = 0; p < count; p++) {
                    MaterialInstance material = rm.getMaterialInstanceAt(instance, p);
                    out.append("  [").append(p).append("] ")
                            .append(material == null ? "(no material)" : material.getName());
                    if (material != null && p <= 2) {
                        try {
                            // Bright matte test: isolate lighting from dark-metallic material response.
                            float[][] colors = {
                                    {1.0f, 0.025f, 0.025f, 1.0f},
                                    {0.025f, 1.0f, 0.025f, 1.0f},
                                    {1.0f, 0.0f, 1.0f, 1.0f}
                            };
                            float[] c = colors[p];
                            material.setParameter("baseColorFactor", c[0], c[1], c[2], c[3]);
                            material.setParameter("metallicFactor", 0.0f);
                            material.setParameter("roughnessFactor", 1.0f);
                            if (p == 2) {
                                // Diagnostic only: P2 magenta emissive is independent of SUN lighting.
                                material.setParameter("emissiveFactor", 4.0f, 0.0f, 4.0f);
                                ceilingTestMessage = "P2 MAGENTA + EMISSIVE OVERRIDE APPLIED";
                                out.append(" | P2 MAGENTA EMISSIVE TEST");
                            } else {
                                out.append(" | MATTE PBR OVERRIDE APPLIED");
                            }
                        } catch (Throwable ex) {
                            if (p == 2) {
                                ceilingTestMessage = "P2 EMISSIVE OVERRIDE FAILED: "
                                        + ex.getClass().getSimpleName() + ": "
                                        + String.valueOf(ex.getMessage());
                            }
                            out.append(" | PBR override FAILED: ")
                                    .append(ex.getClass().getSimpleName()).append(": ")
                                    .append(String.valueOf(ex.getMessage()));
                            Log.w("NeonRushFilament", "PBR override failed for primitive " + p, ex);
                        }
                    }
                    out.append('\n');
                }
            } catch (Throwable e) {
                out.append("Entity ").append(entity).append(" inspection failed: ")
                        .append(e.getClass().getSimpleName()).append('\n');
                Log.w("NeonRushFilament", "Renderable inspection failed", e);
            }
        }
        Log.i("NeonRushFilament", out.toString());
        return out.toString();
    }

    // Reads the JSON chunk embedded in the GLB; no external parser or file upload needed.
    private String inspectGlbJson(byte[] glb) {
        StringBuilder out = new StringBuilder("\nGLB GEOMETRY / MATERIAL DATA\n");
        try {
            ByteBuffer data = ByteBuffer.wrap(glb).order(ByteOrder.LITTLE_ENDIAN);
            if (data.getInt(0) != 0x46546C67) {
                return out.append("Invalid GLB magic\n").toString();
            }
            int offset = 12;
            JSONObject root = null;
            while (offset + 8 <= glb.length) {
                int length = data.getInt(offset);
                int type = data.getInt(offset + 4);
                offset += 8;
                if (length < 0 || offset + length > glb.length) {
                    throw new IllegalArgumentException("Invalid GLB chunk length");
                }
                if (type == 0x4E4F534A) {
                    byte[] jsonBytes = Arrays.copyOfRange(glb, offset, offset + length);
                    String json = new String(jsonBytes, StandardCharsets.UTF_8)
                            .replace("\u0000", "").trim();
                    root = new JSONObject(json);
                    break;
                }
                offset += length;
            }
            if (root == null) return out.append("GLB JSON chunk not found\n").toString();

            JSONArray accessors = root.optJSONArray("accessors");
            JSONArray meshes = root.optJSONArray("meshes");
            JSONArray materials = root.optJSONArray("materials");
            out.append("Accessors: ").append(accessors == null ? 0 : accessors.length())
                    .append(" | meshes: ").append(meshes == null ? 0 : meshes.length())
                    .append(" | materials: ").append(materials == null ? 0 : materials.length())
                    .append('\n');

            if (meshes != null) {
                for (int m = 0; m < meshes.length(); m++) {
                    JSONObject mesh = meshes.optJSONObject(m);
                    JSONArray primitives = mesh == null ? null : mesh.optJSONArray("primitives");
                    out.append("Mesh ").append(m).append(" ")
                            .append(mesh == null ? "" : mesh.optString("name", "(unnamed)"))
                            .append(" | primitives ").append(primitives == null ? 0 : primitives.length())
                            .append('\n');
                    if (primitives == null) continue;
                    for (int p = 0; p < primitives.length(); p++) {
                        JSONObject primitive = primitives.optJSONObject(p);
                        JSONObject attrs = primitive == null ? null : primitive.optJSONObject("attributes");
                        int pos = attrs == null ? -1 : attrs.optInt("POSITION", -1);
                        int norm = attrs == null ? -1 : attrs.optInt("NORMAL", -1);
                        int indices = primitive == null ? -1 : primitive.optInt("indices", -1);
                        out.append("  P").append(p).append(" POSITION ");
                        out.append(accessorSummary(accessors, pos));
                        out.append(" | NORMAL ").append(accessorSummary(accessors, norm));
                        out.append(" | INDEX ").append(accessorSummary(accessors, indices));
                        int mat = primitive == null ? -1 : primitive.optInt("material", -1);
                        out.append(" | material ").append(mat).append('\n');
                    }
                }
            }

            if (materials != null) {
                for (int i = 0; i < materials.length(); i++) {
                    JSONObject mat = materials.optJSONObject(i);
                    if (mat == null) continue;
                    JSONObject pbr = mat.optJSONObject("pbrMetallicRoughness");
                    out.append("Material ").append(i).append(" ")
                            .append(mat.optString("name", "(unnamed)"))
                            .append(" | baseColor ")
                            .append(arrayString(pbr == null ? null : pbr.optJSONArray("baseColorFactor")))
                            .append(" | metallic ")
                            .append(pbr == null ? "default" : pbr.opt("metallicFactor"))
                            .append(" | roughness ")
                            .append(pbr == null ? "default" : pbr.opt("roughnessFactor"))
                            .append(" | emissive ")
                            .append(arrayString(mat.optJSONArray("emissiveFactor")))
                            .append('\n');
                }
            }
        } catch (Throwable e) {
            out.append("GLB JSON inspection failed: ").append(e.getClass().getSimpleName())
                    .append(": ").append(String.valueOf(e.getMessage())).append('\n');
            Log.w("NeonRushFilament", "GLB JSON inspection failed", e);
        }
        String result = out.toString();
        Log.i("NeonRushFilament", result);
        return result;
    }

    private String accessorSummary(JSONArray accessors, int index) {
        if (index < 0 || accessors == null || index >= accessors.length()) return "missing";
        JSONObject a = accessors.optJSONObject(index);
        if (a == null) return "invalid";
        return "#" + index + " count=" + a.optInt("count", -1)
                + " type=" + a.optString("type", "?")
                + " component=" + a.optInt("componentType", -1)
                + " min=" + arrayString(a.optJSONArray("min"))
                + " max=" + arrayString(a.optJSONArray("max"));
    }

    private String arrayString(JSONArray a) {
        return a == null ? "(not set)" : a.toString();
    }

    private void detailsPanelVisibility() {
        if (detailsPanel != null) {
            detailsPanel.setVisibility(detailsExpanded
                    ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }

    private void setSunEnabled(boolean enabled) {
        if (viewer == null || sunInstance < 0) return;
        try {
            viewer.getEngine().getLightManager().setIntensity(
                    sunInstance, enabled ? sunIntensityLux : 0.0f);
            sunEnabled = enabled;
            if (sunToggle != null) sunToggle.setText(enabled ? "SUN ON" : "SUN OFF");
            updateStatus("NEON RUSH • FILAMENT DIAGNOSTIC");
        } catch (Throwable e) {
            Log.e("NeonRushFilament", "SUN toggle failed", e);
            if (sunToggle != null) sunToggle.setText("SUN ERROR");
        }
    }

    private String lightingSummary() {
        try {
            LightManager lm = viewer.getEngine().getLightManager();
            int entity = viewer.getLight();
            int instance = lm.getInstance(entity);
            float[] direction = lm.getDirection(instance, new float[3]);
            int w = renderSurface == null ? surfaceWidth : renderSurface.getWidth();
            int h = renderSurface == null ? surfaceHeight : renderSurface.getHeight();
            float aspect = h > 0 ? (float) w / h : 0.0f;
            return String.format(Locale.US,
                    "BUILD 34 • SUN ON/OFF TEST\nSUN %s | %.0f lux | dir %s\n" +
                    "IndirectLight %s | %.0f | SH diffuse only\n" +
                    "Skybox %s | color %.3f %.3f %.3f\n" +
                    "Exposure f/16 | 1/125s | ISO100\n" +
                    "Eye (%.1f, %.1f, %.1f) target (%.1f, %.1f, %.1f)\n" +
                    "Up (%.1f, %.1f, %.1f) | vertical FOV %.1f°\n" +
                    "Near %.2f | far %.1f | surface %dx%d | aspect %.4f\n" +
                    "%s\n",
                    lm.getIntensity(instance) > 0.5f, lm.getIntensity(instance), Arrays.toString(direction),
                    viewer.getScene().getIndirectLight() != null, ambientIntensity,
                    viewer.getScene().getSkybox() != null,
                    skyboxColor[0], skyboxColor[1], skyboxColor[2],
                    eyeX, eyeY, eyeZ, targetX, targetY, targetZ,
                    upX, upY, upZ, verticalFov, nearPlane, farPlane, w, h, aspect,
                    ceilingTestMessage);
        } catch (Throwable e) {
            return "BUILD 30 • diagnostic error: " + e.getClass().getSimpleName() + "\n";
        }
    }

    private void updateStatus(String heading) {
        status.setText(heading + "\n" + lightingSummary() + modelInfo);
    }

    private void showFailure(String stage, Throwable e) {
        running = false;
        String detail = Log.getStackTraceString(e);
        status.setText("FILAMENT DIAGNOSTIC — " + stage + " FAILED\n\n"
                + e.getClass().getName() + ": " + String.valueOf(e.getMessage())
                + "\n\n" + detail);
        Log.e("NeonRushFilament", stage + " failed", e);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private byte[] readAsset(String name) throws Exception {
        try (InputStream in = getAssets().open(name);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
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
    public void doFrame(long frameTimeNanos) {
        if (!running || viewer == null) return;
        try {
            callbacks++;
            long renderStartNanos = System.nanoTime();
            boolean accepted = viewer.render(frameTimeNanos);
            long renderCostNanos = System.nanoTime() - renderStartNanos;
            renderCpuNanosSum += renderCostNanos;
            renderCpuSamples++;
            if (renderCostNanos > renderCpuNanosMax) renderCpuNanosMax = renderCostNanos;
            if (accepted) renderedFrames++;
            else skippedFrames++;

            long elapsed = frameTimeNanos - windowStart;
            if (elapsed >= 1_000_000_000L) {
                int callbackFps = Math.round(callbacks * 1_000_000_000f / elapsed);
                int acceptedFps = Math.round(renderedFrames * 1_000_000_000f / elapsed);
                int falseReturns = Math.round(skippedFrames * 1_000_000_000f / elapsed);
                compactStatus.setText(String.format(Locale.US,
                        "BUILD 34 | accepted %d/s | callbacks %d/s", acceptedFps, callbackFps));
                status.setText("NEON RUSH • FILAMENT DIAGNOSTIC\n"
                        + String.format(Locale.US,
                        "render() accepted %d/s | callbacks %d/s | false returns %d/s\n" +
                        "render() CPU avg %.2f ms | max %.2f ms (not GPU time)\n",
                        acceptedFps, callbackFps, falseReturns,
                        renderCpuSamples > 0 ? (renderCpuNanosSum / (double) renderCpuSamples) / 1_000_000.0 : 0.0,
                        renderCpuNanosMax / 1_000_000.0)
                        + lightingSummary()
                        + "False returns are NOT proof of GPU frame loss.\n" + modelInfo);
                callbacks = 0;
                renderedFrames = 0;
                skippedFrames = 0;
                renderCpuNanosSum = 0;
                renderCpuNanosMax = 0;
                renderCpuSamples = 0;
                windowStart = frameTimeNanos;
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
                if (ambientLight != null) {
                    viewer.getEngine().destroyIndirectLight(ambientLight);
                    ambientLight = null;
                }
                viewer.destroy();
            } catch (Throwable e) {
                Log.e("NeonRushFilament", "Destroy failed", e);
            }
            viewer = null;
        }
        super.onDestroy();
    }
}
