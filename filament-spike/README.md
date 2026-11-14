# Neon Rush — Filament Renderer Spike

This is a separate Android application used to test Filament without changing the production game module.

## What this first test contains

- Filament 1.80.1 with the glTF loader.
- A small procedural GLB corridor: floor, ceiling, side walls, end wall, cyan emissive guides and amber threshold.
- A drag-to-orbit camera and a live FPS counter.
- A separate application ID: `com.neonrush.filamentspike`.

The corridor is a renderer smoke-test asset, not a final Neon Rush environment and not one of the Modular SciFi MegaKit models. The next stage is to import the selected MegaKit glTF model and its external textures, then assemble a representative corridor.

## Build

Requirements: JDK 17 and Android SDK platform 36.

From the repository root, use the existing wrapper while pointing Gradle at this independent project:

```bash
./gradlew -p filament-spike :app:assembleDebug
```

APK output:

```
filament-spike/app/build/outputs/apk/debug/app-debug.apk
```

The GitHub Actions workflow `Filament Renderer Spike` also builds this APK on pushes to the `filament-renderer-spike` branch and on manual dispatch.

## Isolation guarantee

This project does not add a module to the production app, does not change the production Gradle files, and does not touch the Kotlin simulation, game renderer, monetization, progression, or production assets.
