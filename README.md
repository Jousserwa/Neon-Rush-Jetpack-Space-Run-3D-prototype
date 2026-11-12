# Neon Rush: Jetpack Space Run — 3D Prototype

A standalone Android prototype for testing a performant third-person 3D gameplay runtime. This repository is separate from the production 2D game; the existing game repository remains untouched.

## Direction
- Android-first, Kotlin-based prototype.
- Evaluate Google Filament as the real-time 3D renderer.
- Keep gameplay simulation, movement, camera, collisions, and level logic in Kotlin.
- Use glTF/GLB for 3D models and scenes.
- Preserve the production game's progression, economy, accounts, and services for a later integration phase; do not copy or replace those systems during this prototype.

## Prototype milestones
1. Establish a clean Android project and verify the build.
2. Render a lit 3D test scene using Filament.
3. Build a short corridor and interactive door.
4. Add a controllable pilot and wide third-person camera.
5. Add a Wingman, 3D obstacles, pickups, and collision tests.
6. Profile frame time, memory, thermal behavior, and asset size on ordinary Android hardware.

## Constraints
- Keep this experiment isolated from the production repository.
- Do not commit secrets, signing keys, private credentials, or production user data.
- Do not claim device performance until measured on physical devices.
- Prefer scalable assets, level-of-detail, texture compression, and controlled draw calls.

## Status
Project preparation started. Renderer integration and device profiling are not yet complete.
