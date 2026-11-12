# Neon Rush 3D — Production Asset Pipeline

## Purpose

Replace code-generated placeholder geometry with authored, rigged, textured 3D assets while keeping the Android prototype and its gameplay experiments separate from the original 2D production repository.

The prototype remains on the `pilot-model-rebuild` branch. Do not merge visual experiments into the original `Neon-Rush-Jetpack-Space-Run` repository.

## Pipeline decision

- Author hero models, environments, materials, and animation in Blender or an equivalent professional DCC tool.
- Export runtime assets as **glTF 2.0 binary (.glb)**.
- Use Filament as the current Android rendering testbed and validate actual asset loading, animation, lighting, and frame time on a physical Android device.
- Do not treat Filament suitability for the complete game as proven by the current primitive scene. Reassess the renderer/engine after a representative vertical slice.
- Keep gameplay code, asset authoring files, and exported runtime assets distinct.

## First vertical-slice deliverables

Build only these two visual proofs first:

1. **Hero pilot** — a recognisable, fully modelled character with a production skeleton, PBR materials, and a small set of flight/combat animations.
2. **One forest environment segment** — a complete, layered 3D woodland scene with authored tree/foliage meshes, terrain, rocks, lighting, atmosphere, and a route through the space.

The hangar/corridor can remain a functional greybox until these two proofs pass review. Do not expand the world catalogue using placeholder cubes as if they were final art.

## Hero pilot specification

### Visual identity

- Match the approved Neon Rush pilot reference: large rounded helmet, cyan/magenta visor treatment, graphite segmented armour, broad shoulder protection, sculpted chest and abdomen, shaped forearm guards, fitted thigh/shin armour, and clear boot silhouettes.
- The character must read as an intentional designed hero at normal gameplay camera distance, not as a collection of balls or boxes.
- The base pilot is a complete character. Jetpack, weapons, shoulder pods, drones, and other upgrades are separate attachable equipment, not fused into the base body.
- Preserve the established cyan/magenta identity and readable silhouette. Use restrained emissive trim; armour detail must remain visible when emissive effects are off.

### Model and rig

- Deliver a clean, deformation-ready humanoid mesh with a named skeleton and sensible bone weights.
- Separate mesh/material regions where needed for visor, armour plates, undersuit, emissive strips, and replaceable equipment.
- Include attachment sockets/bones with stable names for at least: back/jetpack, left and right hand/weapon, left and right shoulder equipment, and wingman/companion alignment reference.
- Keep the character's root/origin and forward axis documented in the export notes. The runtime route currently travels along world -Z; establish and test one consistent character-forward convention in the GLB importer rather than guessing or applying ad-hoc rotations per asset.
- Avoid intersecting plates, detached-looking joints, paper-thin armour, and silhouette gaps that appear at gameplay distance.

### Required animation clips

Export named clips in the same GLB:
- `idle_flight` — stable airborne flight pose with subtle body/gear motion.
- `fly_forward` — forward flight cycle.
- `bank_left` and `bank_right` — readable lateral banking.
- `ascend` and `descend` — controlled vertical movement.
- `hit_react` — short impact response.
- `boost` — acceleration/boost pose.
- `victory` — short celebration for menus/rewards.

The first integration milestone only needs `idle_flight`, `fly_forward`, and the two bank clips, but the rig should not need redesign to add the remaining clips.

## Forest environment specification

- Deliver a real forest composition, not scaled cubes standing in for trunks, branches, leaves, rocks, or ground.
- Include a readable foreground/midground/background, varied tree silhouettes and species, authored foliage clusters, roots, rocks, undergrowth, terrain variation, and a navigable flight corridor.
- Use modular segments with matching end/start boundaries so the route can stream continuously without visible seams or sudden repeated patterns.
- Include collision/obstacle proxy meshes separately from decorative foliage where practical.
- Supply at least two visual depth layers beyond the immediate route so the scene does not look like a flat wall.
- Provide a lighting setup that keeps the pilot and environment readable on a phone display. Do not rely on a black background or excessive bloom to create contrast.

## Materials and texture delivery

Use glTF 2.0 PBR materials. Supply base colour, normal, metallic/roughness, and ambient-occlusion information where useful. Emissive maps should be reserved for intentional lights and energy details.

Provisional mobile budgets for the first slice (adjust after device profiling):
- Hero pilot: one 2K texture set where needed; avoid many tiny unique materials.
- Environment: shared atlases and material reuse; mostly 1K maps, with 2K only for close/high-value surfaces.
- Provide sensible LODs for hero/environment meshes where the asset pipeline supports them.
- Keep transparent materials limited; use opaque/masked foliage where visually acceptable.
- Prefer compressed GPU texture formats supported by the chosen Android device/render path; verify fallback behaviour.

These are starting budgets, not permission to sacrifice silhouette, readability, or authored detail. Profile memory, draw calls, GPU time, and frame pacing on representative lower- and mid-range Android devices.

## Export and integration contract

- Runtime format: `.glb`, glTF 2.0.
- Keep source `.blend` files outside the APK; retain them in the project asset-source location or the team's source-control/LFS storage when available.
- Export named meshes, materials, skeleton, animation clips, and attachment nodes.
- Avoid external file-path dependencies in exported GLBs; package referenced resources or embed them.
- Add each approved runtime GLB under `app/src/main/assets/models/` (or a clearly documented equivalent).
- Integrate assets through a dedicated loader/scene layer. Do not embed generated character geometry in `CorridorGltf.kt` as the production asset source.
- Keep scene/gameplay transforms separate from imported model-local transforms.
- The importer must handle missing optional clips or sockets safely and report a useful error for a missing required model.
- The current procedural pilot and forest remain temporary test placeholders until the replacement assets pass the acceptance checks below.

## Acceptance checks before expanding production art

### Pilot
- Recognisable against the approved reference at the actual gameplay camera distance.
- Correct proportions and silhouette from front, side, and three-quarter views.
- No visible mesh gaps or broken deformation during flight, banking, and vertical movement.
- Helmet/visor orientation and head movement are controlled by the rig, not a fragile manually edited transform matrix.
- Jetpack and weapons can be attached/removed independently without rebuilding the body model.

### Forest
- Immediately reads as a forest on the target phone, with visible depth and authored foliage.
- Pilot remains clearly visible against bright and dark parts of the scene.
- Continuous streaming shows no gaps, abrupt segment seams, or obvious short-loop repetition.
- Route, obstacles, and collision proxies remain legible during movement.

### Device/build
- A clean GitHub Actions Android build succeeds.
- Install and run the exact resulting APK on a physical Android device.
- Record frame pacing, memory use, load time, and thermal behaviour during a representative run.
- Do not call the slice production-ready until visual review and device profiling both pass.

## Current known limitation

The current `CorridorGltf.kt` creates a reusable cube and a superellipsoid-like mesh, then assembles the pilot and forest from scaled instances. The head pivot adds a transform hierarchy but does not create a true skeletal rig. This is useful for testing movement and transitions only. It is not the final character or environment asset pipeline.

## Work order

1. Lock the asset contract and visual acceptance criteria (this document).
2. Produce and review the hero pilot model/rig as a standalone asset.
3. Integrate it into the prototype and verify animation and equipment sockets.
4. Produce and integrate one forest segment using the same review process.
5. Profile the combined slice on Android and decide whether the current renderer is suitable for the full game's scope.
6. Only then expand environments, Wingman, enemies, aircraft, naval assets, bosses, and the remaining worlds.
