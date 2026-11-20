# The Fractured Island — intro title replacement

**CyberNpc 0.44.96 | development-no-actions**

Replaces the temporary wooden `testing_version_title.png` intro with the
owner-provided **Fractured Island** Blockbench title from
`Fractured Island.zip` (uploaded 20 November 2026).

## Supplied source
- `model.obj`: extruded 3D lettering from Blockbench 5.2.2
- `materials.mtl`: references `fractured_island.png`
- `fractured_island.png`: 1000×320 purple texture atlas

**Do not display the texture atlas directly.** It consists of UV islands,
not the assembled words. The supplied OBJ was rendered front-on with a
subtle 9° horizontal and 6° vertical perspective. A transparent
1128×139 indexed PNG was then saved with preserved pixel colours and alpha,
and optimized losslessly. This pre-rendered sprite is stored at:

`assets/cybernpc/textures/gui/fractured_island_title.png`

The title's image dimensions were updated in
`SummoningCinematicScreen`. The existing centered blit, opening/closing
fade, subtle push, black background and 2.25-second title window remain
unchanged. So do the subsequent summoning camera arc, teleport, Photon
effects, player controls, skip behaviour, Pope greeting and network IDs.

## Why not render OBJ live?
The cinematic is an ordinary client-side Minecraft GUI. Converting the
static title once avoids a redundant client-side OBJ loader, texture atlas
parsing, frame-by-frame geometry uploads, and potential render-state
regressions. The exported `Fractured Island.zip` remains the editable
authoring source if the user changes the model later.

## Manual check
1. Build CyberNpc locally on `development-no-actions`:
   `cd "$HOME\CyberMods\CyberNpc"; git pull --ff-only; .\build-local.bat`
2. Check `CyberNpc.jar` is created; no duplicate older versions in mods.
3. Trigger the first-arrival intro with your existing intro test/reset controls.
4. Confirm purple 3D lettering is centered on black for 2.25 seconds,
   fades correctly, and is not blank or replaced with scattered UV textures.
5. Verify camera/summoning effects and Pope handoff still work; press Escape
   on a second test to check intro Skip and camera restoration.
6. Verify the visual is readable at your preferred Minecraft GUI scale.

Changes committed as source/assets only. A local build and in-game test
remain necessary.
