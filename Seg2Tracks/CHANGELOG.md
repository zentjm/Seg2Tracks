# Changelog

All notable changes to Seg2Tracks are documented in this file.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Version numbers follow `MAJOR.MINOR.PATCH`.

---

## [0.5.4] — 2026-07-06

### Added

- **Multi-Way Reduction Contraction (MRC)** — a new SARN external-segmentation method
  (registered in `seg2tracks.config`, selectable from the SARN dropdown). Resolves a cell's
  boundary sector by sector instead of with a single nearest-neighbour radius: each angular
  region is governed by its most spatially relevant neighbour, so boundaries stay locally
  correct in every direction. Supports standard and recursive (subsegmentation) modes.
  Implemented on a new shared `OneWayContractionBase` extracted from the three existing
  One-Way Contraction variants (which now inherit it); the extraction also fixed a latent
  missing null-guard in `OneWayContraction_Exclusion`. The per-segment perimeter computation
  in `Sarn.run()` (steps 1–6) is now its own `protected computeSegmentPerimeter()` method,
  reused by MRC's sector-refinement pass instead of being duplicated.
- **Redraw Segment** — in both Manual Segmentation and Recursive Manual Segmentation, correcting
  a single frame's outline no longer requires deleting and re-drawing the entire track. Select
  the object (same click-to-select used by Delete/Merge), navigate to the frame that needs
  correcting, and click the new **Redraw Segment** button to draw a replacement outline for just
  that frame; every other frame in the track, and its link to the frames before/after it, is
  untouched.
- **Boundary Cleanup Settings** — a new **Boundary Cleanup Settings** button in the SARN
  Preprocessing row's Settings dialog (`ExternalSegmentationSettings`) exposes the
  `removeLoops`/`douglasPeucker` boundary-generation parameters (Search Distance %, Search Distance
  Ceiling, Simplification Epsilon) for manual entry or, via its own **Guided Calibration**
  live-preview tool, interactive slider tuning against a real object from the loaded dataset
  (before/after boundary overlay) with an **Auto-calibrate from DataSet** starting-point estimate.
  Mirrors the existing Object ID Settings / Guided Calibration workflow. Note the parameters affect
  internal-segmentation boundary generation too, not just SARN — the Settings dialog it's nested
  under is organizationally about the SARN Preprocessing row, but the scope is broader.
  Persists per-panel via the same `Preferences` mechanism as every other segmentation setting.

### Changed

- **Perimeter simplification replaced with Douglas-Peucker** — the `shortcutPerimeter()` pipeline
  (used by every SARN method, MRC, and both internal-perimeter segmentation passes) conflated two
  unrelated jobs: collapsing loop/revisit artifacts, and, as an incidental side effect, reducing
  point count. These are now two separate, deliberate passes: `removeLoops()` (the extracted
  loop-collapse logic, unchanged otherwise) followed by a new `douglasPeucker()` simplification
  pass (`GeometricCalculations.DEFAULT_SIMPLIFICATION_EPSILON = 1.5` pixels) that removes points
  that don't meaningfully change the boundary's shape, rather than only points involved in a loop.
  Straight boundary runs are now reduced to just their endpoints; corners and genuine curvature are
  preserved. The manual-segmentation merge-dedup path (`ManualSegmentationController.mergeSegments()`)
  keeps `removeLoops()` only, without Douglas-Peucker, since that's cleanup of a freshly-combined
  boundary rather than deliberate simplification. `removeLoops`'s search-distance window (how far
  ahead it looks for a loop artifact) is no longer a flat constant — it now scales with each
  boundary's own point count (`GeometricCalculations.scaledSearchDistance()`), tunable per dataset
  via the new **Boundary Cleanup Settings** (see Added, above).
  - **"External Perimeter" and "Perimeter" now report true geometric length, not point count.**
    Both were previously computed as `segment.get...Perimeter().length` — the number of points
    in the stored boundary array, not an actual length measurement. This was already a fragile
    proxy (any change to point-count-affecting parameters silently shifted the reported value for
    unrelated reasons); Douglas-Peucker's deliberate, tunable point reduction would have made it
    far worse. Both calculations now sum the Euclidean distance between consecutive perimeter
    points (`GeometricCalculations.arcLength()`). **Values for existing datasets will differ from
    prior versions** — this is a one-time, intentional correction, not regression.
  - **Major Axis Angle / Major Axis Length now re-densify before PCA.** `MatrixFunctions.getEigenVectors()`
    performs unweighted PCA directly on the perimeter point list, so point *density* along the
    boundary (not just position) affected the computed axis — a pre-existing but minor bias that
    Douglas-Peucker's more aggressive, uneven point reduction would have made much more visible.
    `getEigenVectors()` now re-densifies via `straightPerimeter()` before the covariance step, so
    the computed axis reflects the shape, not how sparsely its boundary happens to be represented.
    **Values for existing datasets may shift slightly** — this is an accuracy fix, not a regression.

### Fixed

- **Analysis failures were silent (no export, no error)** — `AnalysisController.runAnalysisThread()`
  never called `get()` on its `SwingWorker`, so any exception thrown during analysis was
  swallowed and the user just saw no export. It now overrides `done()`, prints the stack
  trace, sets the progress bar to "Analysis Failed" and shows an error dialog with the cause.
- **General Recursive Segmentation Data crashed on subsegments without internal segmentation** —
  `Area.calculate()` threw an NPE (`getAreaByRoi(null)`) when subsegments had only a SARN
  (external) perimeter. `GeneralRecursiveAnalysis.initialize()` now checks up front and fails
  with a clear message naming how many subsegments are missing an internal boundary and what
  to do. There is deliberately **no** silent fallback to the external perimeter, since that
  would hide that internal segmentation was skipped; "Convert SARN to Segmentation" remains
  the explicit way to use the SARN outline as the final boundary.
- **Convert SARN to Segmentation shared one array for both perimeters** —
  `SARNtoSegmentationConversion.segmentation()` assigned the external perimeter array itself as
  the internal perimeter. `RecursionOperationModel.translateAndAccumulate()` offsets both
  perimeters in place, so in subsegmentation the shared points were shifted twice and
  subsegments landed in the wrong place. It now stores a deep copy.
- **Frame-level means computed only the last segment** — `FrameSetMean.calculate()` used
  `total =+ x` (assignment of unary plus) instead of `total += x`, so every `FrameSetMean_*`
  column in the Frame Data sheet reported the last segment's value divided by the frame's
  segment count. Now accumulates correctly, matching `LinkSetMean`.
- **Manual merge on internal segmentation committed as external** — `mergeObject()` hardcoded
  `setModifyData(0, …)`; it now passes `runType` like every other manual-edit path, so merges
  land on the channel that was actually edited.
- **NullPointerException exporting datasets with empty frames** — the FrameSet wiring and
  Frame Data retrieval loops in `OperationMethod` now skip null slots (frames containing no
  segments), consistent with the guard in `DataSet.removeSegmentationType()`.
- **Stale selection after stepping back past the first sub-segment** — `RecursionManualController.previousFrame()`
  now clears `segment` when the track empties, so a removed segment's ROI is no longer restored.
- **Consistent missing-value sentinel** — `FrameSet` and `LinkSet` `getCalculation()`/`getStatistic()`
  now return `Double.NaN` for an absent value, matching `SegmentModel`, superseding the `0.0`
  behavior shipped in 0.5.3. `0.0` was indistinguishable from a genuine zero measurement in
  exported spreadsheets.
- **Datasets saved before v0.5.1 would not load** — adding `serialVersionUID = 1L` to
  `LinkSetModel` in v0.5.1 changed its identity from the JVM's auto-computed value
  (`-3943624695395302627`), so every dataset saved by an earlier build failed with
  `InvalidClassException`. `FileResourcesUtil.loadDataSet()` now reads through a
  `CompatObjectInputStream` that substitutes the local class descriptor on a UID mismatch
  for `dataStructure.*` classes (whose field layouts are unchanged), so old and new save
  files both load. Verified against a pre-0.5.1 file (1 track × 206 frames) and current autosaves.
- **`shortcutPerimeter()` skipped small perimeters entirely** — a `< 200`-point gate (plus an
  internal `< 50` gate) meant recursive/subsegmentation void perimeters, which are geometrically
  much smaller than full external cell envelopes, almost never got their loop-artifact cleanup
  pass, while full-size perimeters did — an inconsistency that would have surfaced as soon as
  recursive editing was finished. Both gates are removed; the search window is now dynamically
  clamped to half the current list size on every pass instead, which is what actually makes
  small lists safe to process (a fixed search window larger than the list would otherwise wrap
  around and misread a point as a "shortcut" of its own near-neighbour, deleting most of the
  contour). Also switched the proximity check from Manhattan to Euclidean distance — Manhattan
  distance is anisotropic and silently missed diagonal-adjacent points that were geometrically
  just as close as orthogonal ones.
- **Robustness / cleanup** — `Identification.run()` null-guards the progress bar consistently;
  `RecursionManualController.modifyMenu()` restores sidebar cell-switching like `mainMenu()`;
  the EDT-dispatch idiom in `OperationController` is centralized in a `runOnEdt()` helper;
  `GuidedCalibration`'s duplicated detection pipeline is factored into shared helpers;
  `FileResourcesUtil.loadDataSet()` uses try-with-resources so a failed read can't leak the
  file handle; and `getDataSetFileLocation()` now logs a warning if the data directory
  cannot be created instead of silently ignoring the failure.

---

## [0.5.3] — 2026-07-06

### Fixed

- **Link / Unlink / Split producing tracks with ID 0** — all newly created LinkSets now
  receive a proper unique numeric ID via `setName(dataSet.getLinkSetNameIterator())`.
  Previously, linking two tracks or splitting/unlinking a track caused all resulting
  tracks to share ID 0, silently overwriting each other in analysis CSV exports.
- **Restore Selection offering an out-of-bounds ROI** — the bounds check in
  `ManualSegmentationController.nextFrame()` now runs before `setUserRoi()` is called,
  so a rejected draw is never stored and cannot be offered back via Restore Selection.
- **Settings → Back resetting button states mid-draw** — returning from the Settings
  sub-panel while an object is actively being drawn no longer re-applies the initial
  button state, which was incorrectly disabling Next Frame / End Object.
- **Settings → Back from the Modify panel routing to the wrong panel** — `modifyMenu()`
  now clears `inSegmentMode`, so pressing Back after entering Settings from the
  modification panel correctly returns to the modification panel rather than the main menu.
- **Modify panel window not resizing in Recursive Manual window** — `setModificationPanel()`
  now calls `setLayout(GridLayout(4, 1))` and `pack()`, consistent with every other panel
  transition. Previously the window stayed at the taller 8-button segmentation height.
- **Merge perimeter off-by-one in Recursive Manual Segmentation** — the loop condition
  `while (n + 1 != end)` in `mergeSegments()` was corrected to `while (n != end)`,
  which was always omitting the last perimeter point of the merged outline.
- **NullPointerException in analysis output** — `LinkSet.getCalculation()`,
  `LinkSet.getStatistic()`, `FrameSet.getCalculation()`, and `FrameSet.getStatistic()`
  now return `0.0` when the internal map has not yet been populated, instead of
  throwing a NullPointerException.
- **NullPointerException in OWC Gradient Descent** — added the same `getCenterPoint() == null`
  null guard in `OneWayContraction_GradientDecent.outerPoints()` that was already present
  in the base `OneWayContraction` class.

---

## [0.5.2] — 2026-06

### Added

- **Split command** — draw a bisecting line across a selected object to divide it into
  two independently tracked objects. Both sides are re-segmented from intensity maxima
  found on each side of the line using the current SARN method. The split propagates
  forward and backward through the full track. A valley-check warning is shown when
  no clear intensity boundary is detected between the two seeds.
- **Link command** — merge two selected tracks into one sorted track. Blocked if the
  two tracks have any overlapping frames.
- **Unlink command** — split one track into two at the current frame boundary. Segments
  in frames ≤ current frame form one track; segments in later frames form a second.
- **Settings submenu** — a context-sensitive Settings button added to both the main menu
  and the segmentation sub-panel in Manual Segmentation and Recursive Manual Segmentation:
  - *ROI Color…* (available from all contexts) — opens a colour picker; applies
    immediately to all overlay outlines and the active draw-tool rubberband. Persists
    across sessions via `ij.Prefs`.
  - *Draw Tool* (segmentation sub-panel only) — toggles between Polygon and Freehand
    tools. The last-used tool is remembered across sessions and re-selected on Start Object.
- **User Manual button** — renamed from "Help" to "User Manual" for clarity.

### Fixed

- Fixed autosave not propagating from the Recursive Manual window to its parent
  `OperationController`.
- Fixed `mergeSelectedObjects()` in `RecursionManualController` not updating
  `canvasToChildLS` after a merge — old child DataSet entries were not removed and the
  new merged full-image-space entry was not registered, leaving the child DataSet
  desynchronised after merging two objects in the recursive panel.
- Fixed Help / User Manual button registering duplicate `ActionListener` instances when
  `switchToOperation()` was called multiple times per session, causing the manual to open
  multiple times per click.
- Fixed a flag inversion that could incorrectly report a successful load on failure.
- Improved Swing EDT safety for overlay updates across all segmentation windows.
- Fixed resource leaks in dataset save and Excel export functions.
- Fixed OWC Gradient Descent edge fallback to use farthest-edge rather than
  nearest-edge, matching the standard OWC behaviour.

---

## [0.5.1] — 2026-05

### Fixed

Full code-audit pass. All findings resolved:

- **Inverted success flag on load failure** (`OperationController`) — `= true` corrected
  to `= false`.
- **NPE in `switchToAnalysis()`** (`Seg2TracksController`) — null check added before
  `setDataSetName()`.
- **NPE in autosave** (`FileResourcesUtil`) — `listFiles()` null check added.
- **Resource leak in `exportResults()`** (`Seg2TracksController`) — try-with-resources
  added; workbook is now closed on all paths.
- **Resource leaks in `saveDataSet()` / `autosaveDataSet()`** (`FileResourcesUtil`) —
  converted to try-with-resources.
- **NPE in `previousFrame()`** (`ManualSegmentationController`) — segment null guard added.
- **NPE in `restoreSelection()`** (`ManualSegmentationController`) — `previousSegment`
  null guard added.
- **NPE on null FrameSet entries** (`DataSet`) — null skip added in
  `removeSegmentationType()`.
- **NPE in `adjustCenterPoint()` with null perimeter** (`Identification`) — null perimeter
  skip added.
- **NPE in `OneWayContraction` neighbour search** — null center-point guard added to the
  neighbour search loop.
- **Swing EDT violations** (`OperationController`) — `setRunData()`, `setModifyData()`,
  `setOverlayData()` now dispatch via `SwingUtilities.invokeLater()` when called off the EDT.
- **`adjustCenterPoint()` nulling out center point** (`Identification`) — falls back to
  existing center point when the maxima search yields no result.
- **Degenerate merge perimeter** (`ManualSegmentationController`) — guard added; user is
  alerted if fewer than 2 perimeter contact points are found.
- **Latent NPE in `Identification.run()`** — `progressBar` null guard added.
- **`FrameSet.removeSegment()` no-op stub** — implemented via `remove(segment)`.
- **Dead no-op cancellation check** (`OperationController`) — removed.
- **`LinkSetModel` missing `serialVersionUID`** — `serialVersionUID = 1L` added.

---

## [0.5.0] — 2026-04

### Added

- **User Manual** — scrollable Markdown renderer wired to `UserManual.md` resource;
  accessible from the Help (now User Manual) button in the main window header.
- **Recursive segmentation pipeline** — `commitCellData()`, `finishSession()`, and
  `accumulatedDataSet` fully implemented in `RecursionManualController`; drawn sub-segments
  are now accumulated into a `RecursiveDataSet` and attached to each parent LinkSet.
- **Recursive tolerance control** — `recursiveTolerancePct` field exposed in
  `OperationController` / `CalibrationPanel` / `GuidedCalibration` so the maxima-finding
  tolerance for recursive sub-segmentation can be overridden per image without recompiling.
- **Guided Calibration improvements** — LoG scale-space + DataSet-supervised auto-calibration;
  stepwise adjustment buttons with count and time guards.
- **Overlay shift fix** — corrected a coordinate offset introduced when displaying SARN
  overlays on cropped sub-images in the recursive panel.

---

## [0.4.x and earlier]

Core segmentation and tracking functionality:

- SARN external segmentation (OWC, OWC Gradient Descent, OWC Exclusion, Minimal Boundary).
- Object identification via Gaussian blur + local maxima (Modified Maximum Finder).
- Frame-by-frame linkage (GNN, Modified Hungarian).
- Manual segmentation editing window — draw, delete, and merge objects.
- Recursive (sub-segmentation) panel — segment internal structures cell by cell.
- Multi-channel interaction analysis, internal segmentation, confluency/area/circularity
  calculations, Excel export.
- `SegmentationComparer` for pairwise segmentation evaluation.
- Autosave and session persistence.
