# Seg2Tracks — Claude Code Reference

Seg2Tracks is a Fiji/ImageJ plugin (Java/Maven) for segmenting and tracking objects in
fluorescence microscopy image stacks. Primary use case: macrophage tracking in ADCP
(antibody-dependent cellular phagocytosis) experiments, including detection of internal
phagosomal voids via recursive subsegmentation.

Full narrative context: `Seg2Tracks_Project_Context.md` in this directory.

---

## Build & Deploy

```bash
# Deploy to Fiji and test (Eclipse launch configs also available):
mvn install                           # copies built jar to /Applications/Fiji.app/plugins
# Then launch Fiji separately via "Open Fiji.launch" Eclipse config
```

`scijava.app.directory` in `pom.xml` points to `/Applications/Fiji.app`.
**Manual segmentation cannot be tested in the IDE** — requires a live Fiji instance
(ImageJ's `IJ` singleton and `WindowManager` are not available headless).

---

## Source Layout (`src/main/java/`)

| Package | Key Classes | Role |
|---|---|---|
| `gui` | `Seg2TracksController`, `OperationController/Panel/Model`, `RecursionOperationModel`, `CalibrationPanel` | MVC for main UI and per-panel operation |
| `identification` | `Identification` | Gaussian blur → MaximumFinder → intensity filter → Segment detection |
| `sarn` | `Sarn` (abstract), `OneWayContraction`, `OneWayContraction_GradientDecent`, `OneWayContraction_Exclusion`, `MinimalBoundary` | External segmentation (SARN algorithm) |
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
For each frame and each detected cell:
1. Compute inner reference point (cell marker center)
2. Compute outer constraint → circle of Bresenham line endpoints
3. `clean()` — clamps all boundary points to image bounds; sets `externalBoundaryContact`
4. `boundaryMatch()` — pair each inner point to each outer circle point
5. `contractor()` — for each pair, walk Bresenham line, find darkest (threshold) pixel
6. Geometric refinements: `straightPerimeter → shortcutPerimeter → straightPerimeter`
7. Store as `segment.setExternalPerimeter()`

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
2–3 without touching the locked state. `subsegmentOption` is **never set in any constructor**.

`allSegmentationLoaded()` in `Seg2TracksController` is the central hub called after every
data-changing event. `refreshSubsegmentationOnAllPanels()` is always called from there.

---

## Critical Invariants / Gotchas

- **`getCenterPoint()`** — capital P. `getCenterpoint()` does not exist.
- **`JComboBox<String>`** not `JComboBox<String[]>`. The latter compiles but breaks `addItem()`.
- **Refreshing a `JComboBox` in a layout**: always `removeAllItems()` + `addItem()` on the
  existing instance. Never replace the field with `new JComboBox(...)` — layout holds a stale ref.
- **`subsegmentOption` / `checkBoxSubsegmentation`**: checkbox-only, never set by constructor.
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
- **`commitCellData()` post-translation bounds check** — no validation after `p.translate(ox, oy)`.
- **`MinimalBoundary` in recursive mode** — farthest-point outer constraint substitution not
  applied (its `outerPoints()` returns multiple angular points, incompatible with single-point
  substitution). Deferred.

---

## Pre-v1.0 TODO

- [ ] `commitCellData()` and `finishSession()` — currently fully stubbed; must accumulate drawn
  segments into `accumulatedDataSet` (RecursiveDataSet), attach per-cell childDataSet to parent
  LinkSet, call `controller.setRunData(0, accumulatedDataSet)`
- [ ] `accumulatedDataSet` initialization in `RecursionManualController.run()` —
  `new RecursiveDataSet(fullStack.getWidth(), fullStack.getHeight(), fullStack.getSize(), parentDataSet)`
- [ ] Perimeter simplification refactor — replace `shortcutPerimeter` pipeline with
  `straightPerimeter(douglasPeucker(removeLoops(points), epsilon))`. Remove the `< 200`
  threshold so recursive void perimeters are processed. Implement Douglas-Peucker (~30 LOC)
  for shape-fidelity guarantee. Widen loop-removal proximity check (Euclidean < 3 instead of
  Manhattan < 2). Must be done after recursion is fully implemented, before v1.0 release.
  See conversation for full comparison of current method vs `getInterpolatedPolygon` vs
  Douglas-Peucker and downstream consumer constraints (PCA, `.length`, boundary contact).
- [ ] Help system — `HelpMenuPanel` content + tooltips
- [ ] Batch testing macro
- [ ] ImageJ.net wiki page
- [ ] v1.0 release — bump `pom.xml` version to 1.0, compile, test, deploy

---

## Test Data

- **`Phantom28_with_voids.tif`** — 990×495, 8-bit, 20 frames, 6 tracked macrophages (~24px
  radius each), each containing 4 dark circular voids. 7 total tracks (one cell splits at
  frames 6/7). Merge events at frames 5 and 8. Reference dataset for recursive segmentation.
- **`Phantom_28_Numbered.tif`** — 990×495, 8-bit, 20 frames, 6 numbered circles (~50px
  diameter). Numbers burned at intensity 0. Circles 5 and 6 exit frame by frames 4–7.
  Reference dataset for tracking label verification.
