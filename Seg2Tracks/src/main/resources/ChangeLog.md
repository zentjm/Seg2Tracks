# Seg2Tracks Change Log

**Version 0.5.4** | *Released 2026-10-06*

A running log of what changed in each version, newest first. For the full user
guide, use the **User Manual** button.

---

## What's New in v0.5.4

### New segmentation method

- Added **Multi-Way Reduction Contraction (MRC)** — a new SARN external-segmentation method, selectable from the SARN method dropdown. Instead of using a single nearest-neighbour radius for a cell's entire boundary, MRC resolves the boundary sector by sector: each angular region is governed by its most spatially relevant neighbour, producing boundaries that are locally correct in every direction. Works in both standard and subsegmentation (recursive) mode.

### New editing capability

- **Freehand drawing no longer needs the mouse held down.** Click once to start, move the mouse to trace, and click again to finish (or come back to the starting point and it closes by itself). Press Esc to throw away a trace in progress. The finished outline gets draggable nodes, like a polygon: drag a node to adjust it, shift-click to add a node, alt-click to remove one. After **Next Frame**, the outline carried over from the previous frame can be adjusted the same way instead of being traced again.
- The image now **stays on the frame you are drawing** during Redraw Segment and while drawing a new object, so it can't be scrolled away by accident. Next Frame and Previous Frame still work.
- During **Redraw Segment**, the old outline is now shown as a dashed line in a contrasting colour that can't be edited by accident. Draw the new outline, then Apply to replace it, or Cancel to keep the old one.
- Added **Cancel Object** to Manual and Recursive Manual Segmentation. If you start an object and change your mind, you can now discard it instead of being forced to draw an outline to finish it.
- Fixed **Redraw Segment** display issues: after applying, the new outline no longer appears on every frame of the track; cancelling now returns the track to its normal colour; and the redraw always applies to the frame you started it on.
- Fixed the **Polygon/Freehand tool toggle** so it takes effect immediately, including in the middle of drawing an object.
- Added **Redraw Segment**, in both Manual Segmentation and Recursive Manual Segmentation. Correcting a single frame's outline no longer requires deleting and re-drawing the whole track: select the object (same click-to-select used by Delete/Merge), navigate to the frame that needs correcting, click **Redraw Segment**, draw the replacement outline, then **Apply Redraw** (or **Cancel Redraw** to discard). Every other frame in the track, and its link to the frames before/after it, is untouched.
- Added **Boundary Cleanup Settings**, accessed from the SARN Preprocessing row's **Settings** dialog. Lets you tune how aggressively generated boundaries are cleaned up and simplified, either by entering values directly or with **Guided Calibration**: a live preview that overlays a boundary before and after cleanup on a real object from your data, with an auto-calibrate option to suggest a starting point from your dataset.

### Data compatibility

- Fixed **older datasets failing to load**. Save files created before v0.5.1 stopped opening in later versions (Load Data silently did nothing). They now load correctly again, and new save files continue to work — both old and new formats are read interchangeably.

### Progress bar

- The progress bar is now **wider**, and its text is shown in a status line beside it: what is running (for example *Set 2 — Segmentation*), the result in colour (green for complete, red for failed), and *Ready* when idle.
- The status line says **which panel** a result belongs to (for example *Set 2: Operation Complete* or *Analysis 1: Analysis Failed*), and the result **clears when you press the next button** instead of lingering.
- Subsegmentation runs now show when they are complete, and cancelled runs say *Operation Cancelled*.
- If a segmentation run fails, the progress bar says *Operation Failed* and a message explains what happened, instead of the bar silently stopping part-way. Fiji's Console window only opens for unexpected errors, and the message says so.

### Settings windows

- **Guided Calibration now fills in the settings window instead of changing settings directly.** Its button is now **Send to Settings**: it copies the values into the Object Identification (or Boundary Cleanup) Settings boxes, and nothing changes until you press **Apply** there. This also means the warning about clearing loaded segmentations is always shown when it applies. While Guided Calibration is open, the settings window steps aside and the preview image opens next to the calibration controls instead of covering them. Guided Calibration starts from the values currently in the settings boxes, even ones you have not applied yet.
- **Apply is only available when there is something to apply**: it is greyed out until a value in the boxes differs from the setting in use. A note under the buttons says *Changes not applied yet*, or shows an error in red if a value is invalid. **Invert Intensity** now also takes effect when you press Apply.

### Segmentation accuracy

- Fixed **Preview outlines appearing slightly shifted** toward the bottom-right of objects (a thin dark gap on the right and bottom edges). Only the on-screen preview was affected; measurements and exported overlays were already correct.
- Fixed **internal segmentation boundaries sitting slightly inside the true edge** (and small objects occasionally being cut badly), most noticeable with the Restricted methods such as Restricted Li. Internal segmentation now keeps the exact traced boundary again; **Boundary Cleanup Settings** now apply to SARN boundaries only.

### Analysis and export

- Fixed **analysis failing with no export and no message**. If an analysis hits an error, Seg2Tracks now shows an "Analysis failed" dialog explaining the cause instead of silently producing nothing.
- **General Recursive Segmentation Data** now tells you when subsegments have no internal segmentation (only a SARN outline), instead of failing silently. Run internal segmentation on the subsegmentation panel first, or, if the SARN outline is the boundary you want, use **Convert SARN to Segmentation**. This appears as a simple message; Fiji's Console window no longer opens for it. For General Recursive Segmentation Data, datasets that are not subsegmentations are now greyed out in the input dropdown, so the wrong one can't be chosen by mistake.
- Fixed **Convert SARN to Segmentation placing subsegments in the wrong position** in subsegmentation mode.
- Fixed **frame-level mean statistics** (e.g. per-frame mean area) that were computed incorrectly: each frame's mean reflected only the *last* object in that frame instead of averaging all of them. Every FrameSet mean column in exported results is now correct.
- Fixed **missing measurements exported as 0**. When a value has not been computed, exported spreadsheets now show `NaN` (undefined) rather than `0`, so a missing measurement can no longer be mistaken for a genuine zero.
- Fixed a **crash when exporting results for datasets containing empty frames** (frames with no detected objects).

### Manual segmentation and editing

- Fixed **merging two objects in an internal segmentation** being recorded as external segmentation data; a merge is now committed to whichever channel was actually being edited.
- Fixed a **stale selection reappearing** after stepping back past the first sub-segment in the Recursive Manual window.
- Fixed **subsegmentation (void) SARN boundary cleanup being skipped almost entirely**. A size threshold meant to skip trivial small perimeters unintentionally excluded nearly all subsegmentation results too, leaving stray boundary artifacts uncorrected on smaller objects while larger ones were cleaned normally. Boundaries are now cleaned consistently regardless of object size.
- Improved **boundary simplification** for SARN boundaries (all SARN methods, including MRC). Straight edges are now simplified down to just their endpoints, while corners and genuine curved detail are preserved — previously, straight edges kept every single pixel as a boundary point. The cleanup pass also now scales with object size instead of using one fixed setting for every object, and is tunable per dataset via the new Boundary Cleanup Settings.

### Analysis accuracy

- Fixed **"External Perimeter" and "Perimeter"** measurements to report true boundary length (in pixels) instead of the number of points used to represent the boundary — the two aren't the same thing, and the old approach could shift for reasons unrelated to the object's actual size. **Values for existing data will differ slightly from prior versions**; this is a one-time correction, not a bug.
- Fixed **"Major Axis Angle" and "Major Axis Length"** to no longer be biased by how densely a boundary happens to be sampled at different points along its edge. **Values for existing data may shift slightly**; this is an accuracy improvement.

### Stability

- Improved progress-bar and window-threading safety across the segmentation windows.
- Hardened data-directory creation and dataset loading against silent failures.

---

## What's New in v0.5.3

### Bug fixes

- Fixed **Link / Unlink / Split** producing tracks with ID 0 — all newly created LinkSets now receive a proper unique numeric ID. Previously, linking two tracks or splitting/unlinking a track would create tracks that all shared ID 0, causing data to overwrite each other in analysis exports.
- Fixed **Restore Selection** offering an out-of-bounds ROI — the bounds check in Manual Segmentation now runs before saving the user ROI, so a rejected draw is never stored for later restoration.
- Fixed **Settings → Back** resetting button states mid-draw — returning from the Settings sub-panel while an object is actively being drawn no longer disables the Next Frame / End Object buttons.
- Fixed **Settings → Back** from the Modify panel returning to the wrong panel — `modifyMenu()` now correctly clears the segment-mode flag so Back routes back to the Modify panel, not the Main Menu.
- Fixed **Modify panel window sizing** in the Recursive Manual window — switching from the 8-button segmentation panel to the 4-button modification panel now resizes the window correctly.
- Fixed **merge perimeter off-by-one** in Recursive Manual Segmentation — the last perimeter point was always excluded from merged outlines, producing a slightly incorrect merged boundary.
- Fixed **NullPointerException in analysis output** when calculations have not yet been run for a LinkSet or FrameSet — these methods now return 0.0 instead of throwing.
- Fixed **NullPointerException in OWC Gradient Descent** when a neighbour segment has no center point — the null check present in the standard OWC implementation is now also applied in the gradient-descent variant.

---

## What's New in v0.5.2

### Manual segmentation commands

- **Split** — draw a bisecting line across a selected object to divide it into two independently tracked objects. Both sides are re-segmented from new intensity maxima found on each side of the line, and the split propagates forward and backward through the full track.
- **Link** — merge two selected tracks into one. The resulting track is sorted by frame. Blocked if the two tracks have any overlapping frames.
- **Unlink** — split one track into two at the current frame boundary. The portion up to and including the current frame becomes one track; the remainder becomes a second.

### Settings submenu

A **Settings** button has been added to the bottom of the main menu and the segmentation sub-panel in both the Manual Segmentation and Recursive Manual Segmentation windows. Pressing it opens a context-sensitive sub-panel:

| Setting | Available from | Description |
|---|---|---|
| **ROI Color…** | Main menu and Segmentation sub-panel | Opens a colour picker. The chosen colour is applied immediately to all non-selected overlay outlines *and* to the active draw-tool cursor (the rubberband outline shown while drawing). Persists across sessions. |
| **Draw Tool** | Segmentation sub-panel only | Toggles between **Polygon** (click to add vertices) and **Freehand** (drag to draw) tools. The last-used tool is remembered across sessions and selected automatically on **Start Object**. |

Press **Back** to return to the previous panel.

### Bug fixes

- Fixed autosave not propagating from the recursive segmentation window to its parent controller.
- Fixed a flag inversion that could incorrectly report a successful load on failure.
- Improved Swing EDT safety for overlay updates across all segmentation windows.
- Fixed resource leaks in dataset save and Excel export functions.
- Fixed OWC Gradient Descent edge fallback to match the standard OWC behaviour (farthest edge rather than nearest).
