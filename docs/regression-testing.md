# Painting placement and easel history regressions

These fixes target Minecraft 1.21.1 and NeoForge 21.1.57.

## Painting coordinates

`PaintingEntity` used the obsolete seven-argument `lerpTo` signature. Minecraft
1.21.1 invokes the six-argument override, and `BlockAttachedEntity.setPos` expects
an attachment anchor rather than the rendered entity center. Reusing center
coordinates applied even-size offsets again.

Tracking, the base spawn packet, and additional NeoForge spawn data now use the
same anchor. The spawn packet also carries the facing direction. Geometric
offsets use 16 hanging-entity units per block regardless of canvas resolution.

`PaintingEntityTest` checks 1x1, 1x2, 2x1, 2x2, 3x3, and 4x4 paintings in all
horizontal directions at positive and negative coordinates. It verifies repeated
tracking updates, actual movement, and base/additional spawn-data restoration.

## Easel drawing history

The client previously sent pending actions in reverse order. Server and client
history merging could also misorder or duplicate actions, allowing older colors
to overwrite newer ones. Actions now retain drawing order, with ID-based
deduplication and the server's order resolving equal start timestamps on clients.

Action compatibility now includes tool parameters. Changing size, intensity,
blending, dithering, or color starts a new action, including when the cursor stays
on the same pixel. Recorded parameters therefore match immediate drawing.

Historical server replay excludes actions committed after its cutoff. Snapshot
creation moves that cutoff before any crossing stroke so it captures a complete
prefix of drawing order, even when a long stroke commits after a newer short
stroke. This avoids repeated blending and reordered layers during reconstruction.

Replay and encoding use independent buffer cursors. Decoded action bytes have
their own storage, frame lengths are validated, and sub-action time offsets are
decoded with the same unsigned little-endian representation used by the writer.
The wire format is unchanged.

## Running tests

With Java 21 selected, run:

```sh
./gradlew test build
```

On Windows, use `gradlew.bat test build`.

The 14 JUnit tests run in ModDevGradle's NeoForge unit-test environment. They use
the real entity geometry, tools, pixel storage, history logic, and packet codecs.
Levels, players, networking entry points, and texture uploads are mocked.

Coverage includes parameter changes, action ordering and deduplication,
historical cutoffs, overlapping translucent strokes, undo/redo, repeated replay,
pending local frames, and NBT pixel persistence. Complete action-packet round trips
compare immediate and replayed pixel bytes at x16, x32, and x64 resolutions with
pencil, brush, and bucket tools, fractional coordinates, dithering, and full
100-frame actions.

All 14 tests and the build pass. This is automated regression validation; manual
in-game multiplayer and ZetterGallery integration have not been verified.
