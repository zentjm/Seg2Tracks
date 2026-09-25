# Seg2Tracks — Claude Code Reference

Seg2Tracks is a Fiji/ImageJ plugin (Java/Maven) for segmenting and tracking objects in
fluorescence microscopy image stacks. Supports two-level (recursive) segmentation:
primary objects are called **segments**; objects found inside segments are **subsegments**.
The primary intended use case is tracking labeled cells with internal fluorescent structures,
but the design is general.

Full narrative context: `Seg2Tracks_Project_Context.md` in this directory.

---

## Build & Deploy

```bash
# Deploy to Fiji and test (Eclipse launch configs also available):
mvn install                           # copies built jar to /Applications/Fiji/plugins
# Then launch Fiji separately via "Open Fiji.launch" Eclipse config
```

`scijava.app.directory` in `pom.xml` points to `/Applications/Fiji` (no `.app`).
Eclipse's Seg2Tracks project location is this folder (`/Users/jmz0000/git/repository/Seg2Tracks`);
the Desktop copy is an old snapshot.

**Versioning:** a version is closed only once its jar has been **built AND deployed (uploaded)
to the Fiji update site server**. Local rebuilds and `mvn install` into the local Fiji do not
close a version; keep adding to it until it is uploaded. After an upload, new changes go under
the next patch version: bump `pom.xml`, the header line of both `UserManual.md` copies and both
`ChangeLog.md` copies (`src/main/resources/` and `docs/`, kept identical), and add a new section
to `CHANGELOG.md`. Latest uploaded to the update site: **0.5.3**. Current in-progress version: **0.5.4**.
**Manual segmentation cannot be tested in the IDE** — requires a live Fiji instance
(ImageJ's `IJ` singleton and `WindowManager` are not available headless).

**Working Maven invocation (verified 2026-09-23 on the new Mac):** the Homebrew `mvn` wrapper
forces the x86 JDK, but calling Maven's own script with Fiji's ARM JDK works for a full build:
```bash
export JAVA_HOME="/Applications/Fiji/java/macos-arm64/zulu21.42.19-ca-jdk21.0.7-macosx_aarch64/zulu-21.jdk/Contents/Home"
/usr/local/Cellar/maven/3.6.3_1/libexec/bin/mvn -B compile   # or install to deploy to Fiji
```
The older per-file javac workaround below is kept for reference.

**System Maven is broken on this machine** (Apple Silicon; the configured JDK
under `/usr/local/opt/openjdk` is x86-only — `mvn` fails with "Bad CPU type in
executable", and there's no Rosetta). Workaround used throughout this session,
compiles individual files/packages directly against the installed plugin jar
as the classpath (works because the installed jar already has every class this
session's isolated edits don't touch):
```bash
JAVAC="/Applications/Fiji/java/macos-arm64/zulu21.42.19-ca-jdk21.0.7-macosx_aarch64/zulu-21.jdk/Contents/Home/bin/javac"
PLUGIN="/Applications/Fiji/plugins/Seg2Tracks_-<current-version>.jar"   # ls /Applications/Fiji/plugins/ to confirm
IJ="/Applications/Fiji/jars/ij-1.54p.jar"
DEPS=$(find /Applications/Fiji -iname "commons-lang3*.jar" -o -iname "poi*.jar" -o -iname "joml*.jar" -o -iname "gson*.jar" | tr '\n' ':')
"$JAVAC" -cp "$PLUGIN:$IJ:$DEPS" -d <output-dir> -sourcepath "Seg2Tracks/src/main/java" <files-to-compile...>
```
This does **not** produce a deployable jar (`mvn install` is still required for
that, and still needs the CPU-arch issue solved some other way — e.g. running
Maven under Rosetta if it's ever installed, or fixing the JDK path). It's only
for verifying that edited files compile clean before asking the user to build
and test in Fiji themselves.

---

## Source Layout (`src/main/java/`)

| Package | Key Classes | Role |
|---|---|---|
| `gui` | `Seg2TracksController`, `OperationController/Panel/Model`, `RecursionOperationModel`, `CalibrationPanel` | MVC for main UI and per-panel operation |
| `identification` | `Identification` | Gaussian blur → MaximumFinder → intensity filter → Segment detection |
| `sarn` | `Sarn` (abstract) → `OneWayContractionBase` (abstract, shared OWC logic) → `OneWayContraction`, `OneWayContraction_GradientDecent`, `OneWayContraction_Exclusion`, `MultiWayReductionContraction`; plus `MinimalBoundary` (direct `Sarn` subclass) | External segmentation (SARN algorithm) |
| `segmentation` | `Segmentation` (abstract), `TriangleMethod`, `OtsuMethod`, `LiMethod`, `Restricted*` variants, `ActiveContour` | Internal segmentation (threshold-based) |
| `linkage` | `Linkage` (abstract), `ModifiedHungarian`, `GlobalNearestNeighbor` | Cell tracking between frames |
| `manualSegmentation` | `ManualSegmentationController/Panel`, `RecursionManualController/Panel` | Manual SARN editing and recursive void segmentation UI |
| `dataStructure` | `DataSet`, `RecursiveDataSet`, `LinkSet`, `FrameSet`, `Segment` | Core data model |
| `geometricTools` | `GeometricCalculations`, `ModifiedWand`, `ModifiedAutoThresholder`, `ModifiedMaximumFinder`, `PolarPoint` | Geometric utilities and modified ImageJ tools |
| `util` | `FileResourcesUtil`, `FileSelectionPanel`, `Seg2TracksClassLoader` | Utilities |

---

## Data Model

```
DataSet
  └── LinkSet (one per tracked object across all frames)
        └── Segment (one per frame the object appears in)
              ├── getCenterPoint()          — detection marker (capital P — never getCenterpoint())
              ├── getExternalPerimeter()    — SARN outer boundary (Point[])
              ├── getInternalPerimeter()    — threshold segmentation boundary (Point[])
              ├── getExternalBoundaryContact() / getInternalBoundaryContact()
              └── getFrame()               — 0-based full-stack frame index
```

- `RecursiveDataSet extends DataSet` — holds void segmentation results; stores child→parent
  `LinkSet` mappings. Constructor: `new RecursiveDataSet(width, height, numFrames, parentDataSet)`.
- `FrameSet` — flat list of all `Segment`s detected in one frame; stored in `DataSet`.
- `LinkSet` — iterable `ArrayList<Segment>`; no frame-lookup method — iterate to find by frame.
- `LinkSet.setChildDataSet(childDataSet)` / `getChildDataSet()` — attaches recursive void
  results to a parent cell.

---

## Key Pipelines

### Identification
`Identification.run()`:
1. Duplicate frame processor
2. Gaussian blur
3. `ModifiedMaximumFinder.getMaxima()` — finds local intensity maxima
4. `filterLowPoints()` — keeps only maxima above a kernel-averaged intensity percentile
5. Creates `Segment` at each surviving point

**Recursive mode**: call `id.setRecursionPerimeterMap(parentPerimeterMap)` after `setFinder()`.
Per frame, this restricts the `ModifiedMaximumFinder` scan to the parent cell bounding box and
excludes candidate maxima adjacent to zero-valued (masked) pixels.

### SARN (External Segmentation) — `Sarn.run()`
Steps 1–6 (the per-segment perimeter computation) live in `Sarn.computeSegmentPerimeter(int n)`,
a `protected` method `run()` calls once per segment. It is also called directly by
`MultiWayReductionContraction`'s own custom `run()` to get every cell's unclipped baseline
perimeter for its sector-refinement pass — do not duplicate this body into a subclass again.
For each frame and each detected cell:
1. Compute inner reference point (cell marker center)
2. Compute outer constraint → circle of Bresenham line endpoints
3. `clean()` — clamps all boundary points to image bounds; sets `externalBoundaryContact`
4. `boundaryMatch()` — pair each inner point to each outer circle point
5. `contractor()` — for each pair, walk Bresenham line, find darkest (threshold) pixel
6. Geometric refinements: `straightPerimeter → shortcutPerimeter → straightPerimeter`
7. (back in `run()`) recursive-mode `clipAndStitch()`, then `segment.setExternalPerimeter()`

**Non-recursive outer boundary**: nearest neighboring cell center (circle radius = that distance).
Fallback: OWC → farthest image-cardinal edge; OWC_GD → nearest image-cardinal edge.
Image edge is the hard stop via `clean()` clamping.

**Recursive outer boundary** (when `parentPerimeterMap` is set):
- Step 1: `getNearestVoidCenter()` finds the nearest **sibling void** center in the same
  frame (analogous to nearest neighboring cell in non-recursive mode). If found, that is
  the outer constraint. If this is the only void in the parent, falls back to
  `getParentOuterPoint()` (farthest parent perimeter point), analogous to the image-edge
  fallback in non-recursive mode.
- Step 3.5: each matched outer point is clipped to the last Bresenham point inside the
  parent perimeter polygon (`PolygonRoi.contains()`).
- Step 5 (post-refinement): `clipAndStitch()` removes perimeter points outside the parent
  boundary and replaces each gap with the parent perimeter arc going around the outside of
  the void (direction chosen by void-center distance heuristic, not shorter vertex count).

**`parentPerimeterMap`**: `HashMap<Integer, Point[]>` mapping 0-based frame index to the
parent cell's **internal perimeter** (same perimeter used in `buildCroppedMaskedStack()`). These
must always match — if one is changed, update both.

**`MinimalBoundary`**: uses multiple angular outer points, not a single radius constraint.
The farthest-point substitution in recursive mode is **not applied** to it (deferred).

### Internal Segmentation — `Segmentation`
- **Global** (`globalSegmentation()`): threshold from full-frame histogram → binarize →
  `ModifiedWand` traces from cell center with no ROI constraint. Hard stop = image edge.
  `skipZeroBin` flag: when true, `histogram[0]` is zeroed before threshold computation
  to exclude masked exterior from biasing the threshold algorithm.
- **Restricted** (`restrictedSegmentation()`): threshold from histogram of pixels inside
  the cell's SARN external perimeter only → `ModifiedWand` constrained to that ROI.

### Recursive Segmentation — `RecursionOperationModel.runIt()`
For each parent `LinkSet`:
1. Compute union bounding box of parent internal perimeter across all frames (+3px padding).
2. `buildCroppedMaskedStack()` — cropped stack (cropW×cropH); pixels outside parent's
   **internal** perimeter zeroed via `fillOutside()` + 1-pixel erosion.
3. Build `parentPerimeterMap` in **cropped coordinates**.
4. Create `childDataSet` with cropped dimensions.
5. Run `Identification` with `setRecursionPerimeterMap(parentPerimeterMap)`.
6. Run SARN or internal segmentation (with `skipZeroBin=true` for internal).
7. Run linkage. Apply `SegmentationFilters` if internal segmentation was used.
8. Translate child segment coordinates (center points, perimeters) back to full-image
   space via `offsetPerimeter()` before accumulating into `combinedDataSet`.

---

## Boundary Constraint Summary

| | Identification | SARN | Internal seg (global) | Internal seg (restricted) |
|---|---|---|---|---|
| **Non-recursive** | Image extent (full frame scan) | Nearest-neighbor circle; image edge via `clean()` | Image edge (wand) | SARN external perimeter ROI |
| **Recursive** | Parent perimeter bounding box (cropped stack) + zero-adjacency exclusion | Nearest sibling void center (fallback: farthest parent point) → Bresenham clip → clipAndStitch | Zeroed exterior + `skipZeroBin` histogram fix | SARN external perimeter ROI |

---

## `ModifiedMaximumFinder` (in `geometricTools`)

Custom fork of ImageJ's `MaximumFinder`. Only this class should be used — `MaximumFinder`
from `ij.plugin.filter` is not imported anywhere in this project.

Recursive-mode additions:
- `setRecursionParentRoi(PolygonRoi roi)` — set per frame by `Identification.run()`.
  When set: restricts raster scan to ROI bounding box via `ip.setRoi()`; in
  `getSortedMaxPoints()`, any pixel with a zero-valued 8-neighbor is treated as an edge
  pixel and excluded from the candidate list (mirrors the `excludeOnEdges` logic).
- Clear by calling `setRecursionParentRoi(null)` for frames with no parent segment.

---

## Subsegmentation UI State

The subsegmentation checkbox and combobox on `OperationPanel` (panels > 0 only) have four states:
1. **Absent** — panel 0 only
2. **Greyed out** — no datasets exist yet; both controls disabled
3. **Checkbox enabled, combobox disabled** — datasets exist, checkbox unticked
4. **Both enabled** — datasets exist AND checkbox ticked

Plus a **locked** state (`subsegmentationLocked = true`) that prevents re-enabling until
`clearData(2)` is called. `refreshSubsegmentation(String[] dataSetNames)` handles transitions
2–3 without touching the locked state. `subsegmentOption` is set by the checkbox only, with
one sanctioned exception: the Subsegment-button constructor
`OperationController(..., OperationController parentController)` presets it to `true` because
that panel is created already linked and recursive (no checkbox/combobox selection is offered).

`allSegmentationLoaded()` in `Seg2TracksController` is the central hub called after every
data-changing event. `refreshSubsegmentationOnAllPanels()` is always called from there.

---

## Critical Invariants / Gotchas

- **`getCenterPoint()`** — capital P. `getCenterpoint()` does not exist.
- **`JComboBox<String>`** not `JComboBox<String[]>`. The latter compiles but breaks `addItem()`.
- **Refreshing a `JComboBox` in a layout**: always `removeAllItems()` + `addItem()` on the
  existing instance. Never replace the field with `new JComboBox(...)` — layout holds a stale ref.
- **`subsegmentOption` / `checkBoxSubsegmentation`**: checkbox-only, with the single exception
  of the Subsegment-button constructor (`OperationController(..., OperationController parent)`),
  which presets `subsegmentOption = true` for the pre-linked recursive panel.
- **Serialized `dataStructure` classes (`DataSet`, `FrameSet`, `LinkSet`, `LinkSetModel`,
  `Segment`, `SegmentModel`)**: never add, remove, or change a `serialVersionUID` to "clean up."
  Doing so changed `LinkSetModel`'s identity in v0.5.1 and broke loading of every dataset saved
  before then. The save format has no version field, so the class UIDs *are* the compatibility
  contract. `FileResourcesUtil.loadDataSet()` reads through `CompatObjectInputStream`, which
  tolerates UID drift for `dataStructure.*` classes **only because their field layouts match** —
  it does not rescue genuine field changes. If you must change fields, add a real migration path.
- **`calculateFrameOffsets()`**: do not add clamping back. Negative offsets (cell near image
  border) are intentional; the pixel-copy loop handles OOB with a `continue` guard. Clamping
  causes axis-specific off-centering.
- **Constrained stack slice indexing** (`RecursionManualController`):
  `fullFrameIndex = slice - 1 + cellFirstFrame` — not `slice - 1`.
- **`parentPerimeterMap` and `buildCroppedMaskedStack()`** must use the same perimeter type.
  Currently both use `getInternalPerimeter()`. If either is changed, update both.
- **Cropped-to-full coordinate translation**: `offsetPerimeter()` in `RecursionOperationModel`
  translates child segment coordinates back to full-image space. All processing inside the
  per-parent loop uses cropped coordinates; translation happens before `combinedDataSet` accumulation.
- **Exception catch blocks** in `OperationController`: both `runExternalSegmentationThread()` and
  `runInternalSegmentationThread()` now log exceptions and call `controller.allSegmentationLoaded()`
  to recover the UI from loading state.
- **Analysis errors must reach the user**: `AnalysisController.runAnalysisThread()`'s
  `SwingWorker.done()` calls `get()` and shows an error dialog. Keep it: without it, any
  exception in an analysis is swallowed and users just see "no export".
- **No silent perimeter fallbacks in analysis**: `GeneralRecursiveAnalysis.initialize()` refuses
  to run if any subsegment lacks an internal perimeter (user decision, 2026-09-23). Do not add a
  fallback to the external/SARN perimeter; "Convert SARN to Segmentation" is the explicit path.
- **Never share a perimeter array between internal and external**: `translateAndAccumulate()`
  offsets both in place, so an aliased array is shifted twice. Always deep-copy (see
  `SARNtoSegmentationConversion.segmentation()`).
- **Tab encoding** — some source files use hard tabs. The Edit tool may fail to match indented
  code. Use `sed -i` for single-line substitutions in those files.
- **`allSegmentationLoaded()`** is the correct hook for any logic that must run after any
  data-changing event (run, load, clear, modify).

---

## Known Bugs / Pinned Issues

- **False split bug** — `ModifiedHungarian` Case 2 fires spurious daughter splits. Root cause
  not resolved. Do not touch split/daughter logic without discussing first.
- **`externalBoundaryContact` always false in recursive mode** — `Sarn.clean()` checks image
  edges only; parent perimeter is never at an image edge. Fix requires inside-polygon test
  against the parent perimeter in `clean()`. Pinned.
- **`withinBounds()` off-by-one** — `RecursionManualController` allows
  `r.x + r.width == canvasWidth`; `ManualSegmentationController` uses a `-1` guard.
- **Post-translation bounds check** — `RecursionManualController`'s canvas→full-image coordinate
  translation (`convertToOffset()`, used in `endObject()` and `applyRedrawSegment()`) has no
  validation that the translated point lands within the full stack's bounds. (Note: there is no
  method literally named `commitCellData()` — that name never existed in the codebase; this note
  previously referenced it in error.)
- **`MinimalBoundary` in recursive mode** — farthest-point outer constraint substitution not
  applied (its `outerPoints()` returns multiple angular points, incompatible with single-point
  substitution). Deferred.
- **`Roi.equals()` is not identity** — ImageJ compares type, bounds and length only, so
  `Overlay.contains()`/`Overlay.remove(Roi)` can match a *different* but "equal" ROI (e.g. a clone).
  This made Cancel Redraw drop the outline from the display (fixed 2026-09-24 via
  `RedrawReference.indexOfExact()/removeExact()`). Delete/merge in both manual controllers still use
  `overlay.remove(seg.getRoi())`; only a risk if two outlines share identical bounds and length.
- **Segmentation Comparer Settings likely NPEs** — `CompareMethod.getCalculations()` returns
  `null` (TODO stub), and `AnalysisController.openAnalysisSettings()` passes that straight to
  `AnalysisSettings`, which does `for (Data data : dataList)`. Opening Settings with
  SegmentationComparer/SegmentationComparer2 selected should throw. (Found by reading the code, not run.)
- **`ModifiedHungarian.linkAssist()` Javadoc is stale** — says it returns null / is non-functional,
  but it now returns the matrix or throws. Class-level note still calls the implementation incomplete.

### Open items carried over from the March 2026 audit (checked against code 2026-09-23)
Resolved since then: `Sarn.clean()` (returns deduped array and sets image-edge contact),
`Identification` kernel loop, Hungarian loop dimension, Otsu/RestrictedOtsu stubs, ChannelMerger
(rewritten as an overlay-only method), `AreaDistribution` (now std dev of area), GradientDecent
missing from config, commented-out binary segmentation. Still open, all low impact:
- `MinimalBoundary` hull TODOs: fewer than 3 points / "square problem", angle special cases.
- `ActiveContour` (all energy/filter methods stubbed) and `ExternalAnalysis` (all calcs null):
  neither is in `seg2tracks.config`, so unreachable from the UI. Finish or delete.
- `Sarn.consolidate()` returns null (unused); `Sarn.updateStatus()` empty.
- `getPolygonRoi()` duplicated in 5 classes although `GeometricCalculations.getPolygonRoi()` exists.
- `FileResourcesUtil` TODO to use AppDirs instead of manual OS detection.

---

## Pre-v1.0 TODO

- [x] ~~`commitCellData()` and `finishSession()` — currently fully stubbed~~ — **stale, was already
  done.** There is no method named `commitCellData()` (never existed under that name); the actual
  implementation went through `endObject()`/`finishSession()` instead, and was already complete:
  `accumulatedDataSet` accumulation, per-cell `childDataSet` attachment to the parent `LinkSet`,
  and the `controller.setRunData(0, accumulatedDataSet)` call are all present and compile clean.
  Verified directly (not just by re-reading this stale checklist) before building the next item.
- [x] ~~`accumulatedDataSet` initialization in `RecursionManualController.run()`~~ — also already
  done, exactly as specified: `new RecursiveDataSet(fullStack.getWidth(), fullStack.getHeight(), fullStack.getSize(), parentDataSet)`.
- [x] **Redraw Segment** — the standing in-code TODO in `RecursionManualController.java` (per-frame
  boundary correction without deleting/redrawing a whole void track) is now implemented:
  `startRedrawSegment()`/`applyRedrawSegment()`/`cancelRedrawSegment()`, a new "Redraw Segment"
  button on the modification panel next to Delete/Merge, and a new `setRedrawSegmentPanel()`
  Apply/Cancel sub-panel in `RecursionManualPanel.java` (modeled on
  `ManualSegmentationPanel.setSplitLinePanel()`). Reuses the existing whole-track click-select
  mechanism (`selectObject()`/`getRoiSelected()`) — requires exactly one track selected, then
  redraws whichever frame is currently displayed within it. The edit replaces the `Segment` object
  at that frame/position across all three lists that reference it by identity (the track `LinkSet`,
  `currentChildDS`'s `FrameSet`, `accumulatedDataSet`'s `FrameSet` — confirmed these are separate
  `List`s holding the same object reference, not copies, by direct test). No automatic internal-
  segmentation re-run — deferred, with an in-code TODO to warn the user if internal segmentation
  already exists for the cell (a redraw can invalidate it).
  Also ported to the regular (non-recursive) `ManualSegmentationController.java`/
  `ManualSegmentationPanel.java` — same button/sub-panel pattern, adapted to that controller's own
  conventions (`deleteObject()`/`mergeObject()` naming, `getSegment()`'s bounding-box-centroid +
  `straightPerimeter` densification, `setModifyData(runType, dataSet)` + `autosave()` finalization).
  Structurally simpler there: one flat `DataSet`, no canvas/full-image coordinate split, so only
  two lists need the object swap (the `LinkSet`, `dataSet`'s `FrameSet`) instead of five.
  **UI polish gaps in both implementations are tracked separately below ("Redraw UI fixes").**
- [x] **Redraw UI fixes** — reworked 2026-09-24 after the first smoke test; user re-tested OK 2026-09-24:
  instead of preloading the old outline as an editable ROI, `startRedrawSegment()` now takes the
  real outline out of the overlay and shows a dashed, non-editable copy in a contrasting colour
  (`RedrawReference`); the user draws a fresh outline. Apply removes the copy; Cancel puts the
  original back (data is never touched until Apply). This resolves the old items 1 (outline
  colour on arm), 2 (stale outline cluttering the draft), and 4 (dense perimeter = too many
  grabbable vertices). Item 3 (lock the stack) is resolved by `FrameLock`, an `ImageListener`
  that snaps the image back to the locked slice on any navigation (scrollbar, arrow keys, wheel);
  used during redraw and while drawing a new object, in both controllers. Next/Previous Frame move
  via `frameLock.moveTo()`. Earlier same-day fixes: the drawn ROI is cleared after Apply/Cancel
  (it used to show on every frame), Cancel deselects the track, Apply uses the frame fixed at arm
  time (`redrawFrame`/`redrawRelFrame`), and the recursive overlay ROI gets its slice position.
- [x] **Manual drawing usability (from 2026-09-23 smoke test)** — user re-tested OK 2026-09-24:
  - Polygon/Freehand toggle now takes effect immediately (`toggleDrawTool()` re-arms if armed).
  - **Cancel Object** (`cancelObject()`, both controllers; confirms if frames were drawn; removes
    the LinkSet that the `LinkSet(DataSet)` constructor auto-registered).
  - Freehand is now **click-to-trace** (`ClickTraceTool`), replacing ImageJ's hold-to-draw
    freehand (user decision): click to start, the outline follows the cursor with the button up,
    click again to finish, or it auto-closes within ~6 screen px of the start after first moving
    away. Esc discards the trace. While armed, ImageJ's tool is set to "hand" so clicks don't also
    start another selection; cursor forced to crosshair. Controllers call
    `traceTool.commitPending()` before reading the ROI (Next Frame, End Object, Apply Redraw).
    A finished trace is Douglas-Peucker-simplified (~2 screen px) to a POLYGON Roi and ImageJ's
    tool switches to "polygon", so ImageJ's native node editing applies (drag node, shift-click
    add, alt-click delete, drag inside to move); clicking away from the outline starts a new trace.
    Next/Previous Frame call `traceTool.resetForNewFrame()` so the carried-over outline is editable
    on the new frame (frame-by-frame adjust-by-dragging, as with the polygon tool).
    The in-progress trace is shown as a FREELINE in the ROI colour: a POLYLINE draws a handle on
    every point, which on a dense trace looks solid black.
    Arm/disarm goes through `armDrawTool()`/`disarmDrawTool()` in both controllers.
- [x] ~~Remove the `< 200`/`< 50` thresholds so recursive void perimeters are processed~~ and
  ~~widen loop-removal proximity check from Manhattan to Euclidean~~ — done. `shortcutPerimeter`
  no longer skips small contours; the search window is now dynamically clamped to **half** the
  current (shrinking) list size each pass — not just "less than the list size" — since the
  wraparound indexing otherwise lets a large `searchDistance` walk back around and land next to
  its own starting point, which is always "close" on a dense perimeter and gets misread as a
  shortcut spanning nearly the whole contour. Caught this the hard way: initially followed the
  literal "Euclidean < 3" suggested below, which turned out to be a much wider net than
  Manhattan `< 2` ever was (it matches ordinary curvature on *any* smooth boundary, not just
  loop artifacts) and collapsed clean perimeters with no defect at all — the correct isotropic
  equivalent of Manhattan `< 2` is squared-Euclidean `< 4` (`smoothing = 2`, unchanged default),
  which is the 8-connected neighbourhood instead of the axis-biased 4-connected one. Verified
  against the pre-fix algorithm (kept as a scratch copy) across 7 shape sizes (radius 5–90):
  clean perimeters are unaffected, inserted loop artifacts are removed and land within a few
  points of the true clean baseline, and large (previously-already-processed) perimeters are
  byte-for-byte unchanged except where the list transiently shrinks below ~200 mid-call — where
  the new dynamic clamp is *safer* than the old fixed-100 window, not just different.
- [x] **Perimeter simplification refactor** — done. `shortcutPerimeter` (which conflated
  loop-collapsing with incidental point reduction) is deleted; replaced with
  `straightPerimeter(douglasPeucker(removeLoops(straightPerimeter(x)), DEFAULT_SIMPLIFICATION_EPSILON))`
  at all 4 generation-pipeline call sites (`Sarn.computeSegmentPerimeter()`,
  `MultiWayReductionContraction.multiwayPointRefinement()`,
  `Segmentation.extractInternalPerimeter()` x2) and `removeLoops(straightPerimeter(x))` (no DP) at
  the 2 merge-dedup sites in `ManualSegmentationController.mergeSegments()`. New methods in
  `geometricTools/GeometricCalculations.java`: `removeLoops()` (extracted loop-collapse core,
  `minimumSize` early-exit dropped — no longer needed once decoupled from incidental reduction),
  `douglasPeucker(pts, epsilon)` (standard recursive perpendicular-distance simplification, ~55
  LOC with Javadoc), `arcLength(pts)` (Euclidean sum, closed-contour), and constant
  `DEFAULT_SIMPLIFICATION_EPSILON = 1.5` (pixels — just above √2, the max single-step Bresenham
  diagonal, chosen to remove staircase noise without erasing genuine small-scale detail).

  **Do not remove either `straightPerimeter` call in the pipeline expression above — the outer
  one is a correctness requirement, not polish.** The inner call (`straightPerimeter(x)` before
  `removeLoops`) densifies raw `contractorResult`/sector-refined points so `removeLoops`'s
  index-based search window (`searchDistance` positions ahead) corresponds to a consistent pixel
  distance — unchanged reasoning from the old pipeline. The outer call matters more now than it
  did before: `computeSegmentPerimeter()`'s and `multiwayPointRefinement()`'s return values feed
  directly into `clipAndStitch()` (`Sarn.java` line ~220, `MultiWayReductionContraction.java` line
  ~155), which walks the perimeter **point by point** testing `parentRoi.contains(sarnPerim[i])`
  to find where the boundary crosses the parent perimeter (recursive/subsegmentation mode). That
  walk only detects a crossing if consecutive points are pixel-adjacent. Douglas-Peucker produces
  much sparser output than `shortcutPerimeter` ever did — a long straight run can now collapse to
  just 2 endpoints — so if a parent-boundary crossing falls in the middle of a run that got
  collapsed *before* re-densification, `clipAndStitch` would silently miss it. The final
  `straightPerimeter` call re-expands the DP output back to pixel-adjacent points specifically so
  `clipAndStitch` still sees every crossing.

  **`removeLoops`'s flat `searchDistance = 100` has been replaced with a size-scaled, per-dataset
  tunable formula, done as a follow-up feature ("Boundary Cleanup calibration").** The flat 100 was
  an unvalidated magic number inherited unchanged from the old `shortcutPerimeter` — no comment,
  commit, or test justified it, and it doesn't scale: a value that works for a small void perimeter
  under-searches on a large external cell envelope (missing loop artifacts wider than ~100 points
  along a large contour), and a value tuned for large cells over-searches wastefully on small ones.
  Now `GeometricCalculations.scaledSearchDistance(pointCount, searchFraction, searchCeiling)`
  computes `clamp(pointCount * searchFraction, SEARCH_DISTANCE_FLOOR=60, searchCeiling)` — every
  generation-pipeline call site (`Sarn.computeSegmentPerimeter()`,
  `MultiWayReductionContraction.multiwayPointRefinement()`, both
  `Segmentation.extractInternalPerimeter()` overloads) calls this instead of hardcoding 100.
  `searchFraction`/`searchCeiling` are `Sarn`/`Segmentation` instance fields set via
  `setCleanupParams(double, int, double)` (mirrors the existing `setBlur`/`blurSigma` plumbing
  pattern exactly), sourced from `OperationController` (new fields `searchFraction`,
  `searchCeiling`, `simplificationEpsilon` — persisted via `Preferences`, keyed per-panel like every
  other setting, illustrative defaults 0.15 / 400 / 1.5 unchanged from before this feature). The
  `size/2` wraparound-safety clamp inside `removeLoops` itself is unchanged and still applies on
  top of whatever `scaledSearchDistance` returns.

  Rather than hand-picking new constants without data (the same trap the old `100` fell into), this
  is now user-tunable per dataset: **Boundary Cleanup Settings** button, nested inside the SARN
  Preprocessing row's own **Settings** dialog (`gui/ExternalSegmentationSettings.java` — moved here
  from a standalone `OperationPanel.java` button per user request, since it's a sub-setting of SARN
  Preprocessing, not a peer-level row button; note the parameters affect internal-segmentation
  boundary generation too, not just SARN, despite living under the SARN-labeled dialog) opens
  `gui/BoundaryCleanupPanel.java` (manual entry, mirrors `CalibrationPanel.java`) →
  **Guided Calibration** opens `gui/BoundaryCleanupCalibration.java`
  (live-preview sliders for Search Distance %, Search Distance Ceiling, and Simplification Epsilon,
  mirrors `GuidedCalibration.java`). The live preview draws a red/green before/after boundary
  overlay on one representative segment from the loaded DataSet — **honest limitation documented in
  that class's Javadoc**: the true pre-cleanup boundary isn't persisted anywhere (only the final
  cleaned result is stored on a `Segment`), so the preview re-densifies the *already-cleaned* stored
  boundary and re-runs cleanup on that; this demonstrates Epsilon's point-reduction/shape-fidelity
  effect faithfully, but a search-fraction change showing little visible effect is expected (not a
  bug) since an already-cleaned boundary typically has no remaining loop artifacts to find.
  **Auto-calibrate** (`AutoCalibration.estimateSearchFraction(DataSet)`) derives a starting
  `searchFraction`/`searchCeiling` from the loaded dataset's own boundary point-count distribution
  (median/max), not from separate ground truth — there is none for this parameter the way there is
  for object-identification sigma/threshold, so this is explicitly a starting point for the user's
  own visual verification, not a validated final answer. Epsilon has no auto-calibrate — no
  data-driven signal exists for shape-fidelity tolerance; tune it visually.

  Range/smoothing (`GeometricCalculations.LOOP_REMOVAL_RANGE`/`LOOP_REMOVAL_SMOOTHING`, both `2`)
  and the search-distance floor stay fixed internal constants, not exposed — deliberate scope
  decision, matching `GuidedCalibration`'s own precedent of exposing only the parameters with an
  obvious real-world meaning (it doesn't expose `MAX_DETECTIONS` either). The merge-dedup
  `removeLoops()` calls in `ManualSegmentationController.mergeSegments()` are explicitly untouched
  by this feature — they don't run Douglas-Peucker and aren't part of boundary generation.

  **Downstream consumer fix applied**: `calculations/ExternalPerimeter.java` and
  `calculations/Perimeter.java` both used to report raw point count (`.length`) as perimeter
  length — now both call `GeometricCalculations.arcLength()` instead, decoupling the reported
  measurement from how many points happen to represent the boundary. Values for existing datasets
  will differ from prior versions (documented in changelog as a one-time correction).

  **A second fidelity issue was found and fixed during implementation, not anticipated in the
  original TODO**: `geometricTools/MatrixFunctions.getEigenVectors()` performs *unweighted* PCA
  directly on the raw perimeter point list to compute `MajorAxisAngle`/`MajorAxisLength` — point
  *density* along the boundary skews the computed axis, not just point position. Douglas-Peucker's
  uneven thinning (dense corners, sparse straight runs) would have made this pre-existing but mild
  bias much worse. Fixed by re-densifying via `straightPerimeter()` at the top of
  `getEigenVectors()`, before the covariance step — mirrors the "densify on demand" pattern already
  used by ~10 other perimeter consumers (e.g. `Interactions.java`). Every other perimeter consumer
  was checked and confirmed shape-only (goes through `PolygonRoi`/`ShapeRoi`/`getAreaByRoi`, all
  density-independent) — `MatrixFunctions` was the only one affected.

  **Verification**: compiled clean (Fiji ARM JDK workaround, all touched files — the original 8
  from the DP refactor plus, for the Boundary Cleanup calibration follow-up, `Sarn.java`,
  `Segmentation.java`, `OperationController.java`, `OperationModel.java`,
  `RecursionOperationModel.java`, `ManualSegmentationController.java`, `AutoCalibration.java`,
  `Tooltips.java`, `ExternalSegmentationSettings.java` (entry point, after being moved out of
  `OperationPanel.java` per user request), and the two new `gui/BoundaryCleanup*.java` classes).
  Headless scratch test (`PerimeterSimplificationTest.java`, not committed) covering `removeLoops`
  on a synthetic loop artifact and a clean dense contour, `douglasPeucker` on a collinear run and a
  sharp corner, `arcLength` against hand-calculated lengths, `MatrixFunctions.getMajorAxis`
  producing matching angles on sparse-vs-dense input of the same shape, and
  `scaledSearchDistance()` staying within `[SEARCH_DISTANCE_FLOOR, searchCeiling]` across a range
  of contour sizes (small/mid/large, floor-dominated and ceiling-dominated cases) — all 13
  assertions pass. **Live Fiji UI integration testing (SARN generation, manual-segmentation merge,
  exported spreadsheet columns, and — new — the Boundary Cleanup Settings / Guided Calibration
  panels themselves: slider-driven overlay updates, Auto-Calibrate, Apply-then-reopen persistence)
  has not been done** — same acknowledged gap as Redraw Segment; requires a live Fiji session.

  (Item 4 of "Redraw UI fixes", the grabbable vertex count of the editable starting shape, no
  longer applies: since 2026-09-24 Redraw shows the old outline as a non-editable reference.)
- [ ] Help system — `HelpMenuPanel` content + tooltips
- [ ] Batch testing macro
- [ ] ImageJ.net wiki page
- [ ] **Pass-2 structural rename** — field/method/class renames deferred from Pass-1 (Pass-1
  covered user-facing text, Javadoc only; Pass-2 requires serialization migration):
  - `Segment.externalPerimeter` → `envelopePerimeter` (SARN boundary)
  - `Segment.internalPerimeter` → `segmentPerimeter` (threshold boundary)
  - `DataSet.externalSegmentationExists` → `envelopeExists`
  - `DataSet.internalSegmentationExists` → `segmentationExists`
  - Class renames: `VoidCount` → `SubsegmentCount`, `TotalVoidArea` → `TotalSubsegmentArea`
  - Method renames: `runExternalSegmentation` → `runEnvelope`, `runInternalSegmentation` → `runSegmentation`
  - Requires serialization migration strategy for saved `.s2t` files before execution.
- [ ] v1.0 release — bump `pom.xml` version to 1.0, compile, test, deploy

---

## Test Data

- **`Phantom28_with_voids.tif`** — 990×495, 8-bit, 20 frames, 6 tracked macrophages (~24px
  radius each), each containing 4 dark circular voids. 7 total tracks (one cell splits at
  frames 6/7). Merge events at frames 5 and 8. Reference dataset for recursive segmentation.
- **`Phantom_28_Numbered.tif`** — 990×495, 8-bit, 20 frames, 6 numbered circles (~50px
  diameter). Numbers burned at intensity 0. Circles 5 and 6 exit frame by frames 4–7.
  Reference dataset for tracking label verification.
