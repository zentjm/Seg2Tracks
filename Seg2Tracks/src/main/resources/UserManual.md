# Seg2Tracks User Manual

**Version 0.5.4** | *Publication pending*

---

> **About this document**
> This manual is organised by program context — each major section corresponds to a specific panel or dialog in Seg2Tracks. When the context-sensitive help menu is implemented, each section will be surfaced directly from the relevant part of the interface.

---

> **What's new?** Version history and per-release notes have moved to their own window —
> use the **Change Log** button next to **User Manual**.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Installation](#2-installation)
3. [Workflow Summary](#3-workflow-summary)
4. [Main Window — Segmentation View](#4-main-window--segmentation-view)
5. [Object Identification Settings](#5-object-identification-settings)
6. [Guided Calibration](#6-guided-calibration)
7. [Boundary Cleanup Settings](#7-boundary-cleanup-settings)
8. [Boundary Cleanup Guided Calibration](#8-boundary-cleanup-guided-calibration)
9. [External Segmentation (SARN)](#9-external-segmentation-sarn)
10. [Subsegmentation (Recursive Panel)](#10-subsegmentation-recursive-panel)
11. [Internal Segmentation](#11-internal-segmentation)
12. [Linkage](#12-linkage)
13. [Manual Segmentation Editing](#13-manual-segmentation-editing)
14. [Recursive Manual Segmentation](#14-recursive-manual-segmentation)
15. [Main Window — Analysis View](#15-main-window--analysis-view)
16. [Tips and Troubleshooting](#16-tips-and-troubleshooting)
17. [Known Limitations](#17-known-limitations)

---

## 1. Overview

Seg2Tracks is a Fiji/ImageJ plugin for detecting, segmenting, and tracking objects in fluorescence microscopy image stacks. It applies to any fluorescence time-lapse data in which discrete, trackable objects must be segmented frame by frame.

### What Seg2Tracks does

Starting from a raw image stack, Seg2Tracks:

1. **Identifies** object centres by finding local intensity maxima after Gaussian blur pre-processing.
2. **Segments** each detected object using a Segmentation Area Region by Neighborhood (SARN) algorithm that delineates an envelope around each centre.
3. **Optionally subsegments** — within each SARN envelope, a second round of identification and segmentation detects internal sub-objects or inclusions.
4. **Links** objects across frames to produce continuous tracks.
5. **Allows manual correction** of segmentation results through an interactive drawing interface.
6. **Exports** measurements and overlays to Excel workbooks for downstream analysis.

### Key concepts

| Term | Meaning |
|---|---|
| **Segment** | A single detected object in a single frame, defined by a centre point and an external perimeter (SARN envelope). |
| **LinkSet** | A sequence of Segments connected across frames — equivalent to one object track. |
| **FrameSet** | All Segments detected in one frame. |
| **DataSet** | The full collection of FrameSets and LinkSets for one channel/panel. |
| **SARN** | Segmentation Area Region by Neighborhood — the family of algorithms used to compute object boundaries. |
| **Subsegmentation** | A second-pass segmentation run *inside* each parent object's envelope, detecting internal structures. |

---

## 2. Installation

### Requirements

- **Fiji** (recommended) or ImageJ2 with the required update sites enabled.
- Java 8 or later (bundled with Fiji).

### Installing the plugin

1. Download `Seg2Tracks_-x.x.x.jar` from the release page.
2. Copy the `.jar` file into the `plugins/` folder of your Fiji installation.
3. Restart Fiji.
4. Seg2Tracks appears under **Plugins › Seg2Tracks**.

### Updating

Replace the existing `.jar` file in `plugins/` with the new version and restart Fiji.

---

## 3. Workflow Summary

A typical Seg2Tracks session follows this sequence:

```
Load image stack
        │
        ▼
Set Object ID parameters (sigma, threshold)
        │
        ▼
Run External Segmentation (SARN)     ──► Manually edit if needed
        │
        ├──► [Optional] Add Subsegmentation panel
        │            │
        │            ▼
        │    Run Internal Segmentation  ──► Manually edit if needed
        │
        ▼
Run Linkage (track objects across frames)
        │
        ▼
Switch to Analysis view
        │
        ▼
Select analysis method › Run › Generate Results (.xlsx)
```

Multiple independent panels can run in parallel (e.g., two different channels from the same experiment). Each panel maintains its own DataSet, parameters, and linkage independently.

---

## 4. Main Window — Segmentation View

### Layout

The main window contains:

- **Header bar** — citation reference and **User Manual** button.
- **One or more Operation Panels** — each representing one channel or dataset. Panels are added or removed with **Add Panel** / **Remove Panel** in the footer.
- **Footer bar** — progress bar, **DATA ANALYSIS >>** button (enabled once all required segmentation steps are complete), and panel management buttons.

### Operation Panel rows

Each Operation Panel has three functional rows:

| Row | Label | Controls |
|---|---|---|
| 1 | **Internal Segmentation** | Settings · Run · Preview · Subsegment |
| 2 | **SARN Preprocessing** (External Segmentation) | Settings · Run · Manually Edit |
| 3 | **Linkage Mechanism** | Settings · Object ID Settings |

Additionally there is an **Input** button and **DataSet name** field at the top of each panel.

### Loading input data

Click **Input** and select your image stack. Supported formats include TIFF, LSM, CZI, and any format readable by Fiji's Bio-Formats importer.

> **Important:** The image stack must be a single time-lapse sequence. Multi-channel data should be split into individual channel stacks before loading.

### Saving and loading DataSets

Use **Save** and **Load** to persist a completed DataSet (segmentation + linkage results) to disk as a `.s2t` file. Loading a saved DataSet restores all segmentation and linkage results without re-running the pipeline.

### Running the pipeline

The recommended order is:

1. Set Object ID parameters (row 3, **Object ID Settings**).
2. Run External Segmentation (row 2, **Run**).
3. Optionally: add a Subsegmentation panel, set its parameters, run Internal Segmentation (row 1).
4. Run Linkage (row 3 — linkage runs automatically on the external segmentation results unless a subsegmentation panel exists, in which case it links subsegmentation results).
5. Manually edit if needed.
6. Switch to **DATA ANALYSIS >>**.

---

## 5. Object Identification Settings

**Accessed via:** Object ID Settings button (row 3 of any Operation Panel)

This dialog sets the parameters used to detect object centres before SARN segmentation.

### Parameters

| Parameter | Description | Typical range |
|---|---|---|
| **Sigma** | Standard deviation of the Gaussian blur applied before peak detection. Larger values blur the image more, merging nearby intensity peaks. Set this to approximately the expected object radius ÷ √2. | 2 – 50 px |
| **Threshold (%)** | Fraction of the frame's intensity range that a detected peak's kernel-averaged intensity must exceed. Increase to reject dim false detections; decrease to include dimmer true objects. | 5 – 40 % |
| **Recursive Tolerance (%)** | *(Subsegmentation panels only)* Peak-separation tolerance as a percentage of the intensity range, used to distinguish adjacent sub-object peaks within a single parent object. | 5 – 20 % |
| **Invert Intensity** | Check if objects appear as dark regions on a bright background (e.g., transmitted light or phase contrast). | — |

### Auto-calibration

Click **Guided Calibration** to open the interactive preview window, which includes:

- **Auto-calibrate** — estimates sigma automatically from the image using Laplacian-of-Gaussian (LoG) scale-space analysis. No existing segmentation required. Updates the Sigma slider only.
- **Auto-calibrate from DataSet** — estimates both sigma and threshold from an already-loaded ground-truth segmentation. Enabled only when a DataSet with external segmentation data is loaded. Updates both sliders.

After auto-calibration, review the result in the preview and click **Apply to Settings** to commit.

### Applying settings

Click **Apply** to commit values to the controller. If segmentation data is already loaded, you will be warned that changing settings will clear it.

---

## 6. Guided Calibration

**Accessed via:** Object ID Settings › Guided Calibration

Guided Calibration provides an interactive live preview so you can tune detection parameters with immediate visual feedback before committing a full-stack run.

### Preview window

A separate image window opens showing one frame of the input stack with detected objects highlighted as **red circles**. The overlay updates each time a slider is released.

### Sliders

| Slider | Effect |
|---|---|
| **Frame** | Select which frame to preview (only shown for multi-frame stacks). |
| **Sigma** | Gaussian blur sigma in pixels (resolution: 0.5 px per tick). |
| **Threshold (%)** | Intensity threshold as a percentage of the frame's dynamic range. |
| **Recursive Tolerance (%)** | *(Subsegmentation panels only)* Peak-separation tolerance. |

The value label next to each slider updates continuously while dragging.

### Stepwise adjustment

The **Decrease** and **Increase** buttons beside each slider step the parameter one tick at a time until the number of detected objects changes by at least one. This helps find the exact threshold where a given object appears or disappears.

- Runs in the background — all controls are disabled during the search.
- Two automatic safety guards prevent the UI from becoming unresponsive:
  - **Count guard** — stops if the number of raw candidates exceeds ~1 000.
  - **Time guard** — stops if a single detection step takes more than 5 seconds.
  - In both cases a dialog asks whether to continue.

### Auto-calibration buttons

- **Auto-calibrate** — estimates sigma via LoG scale-space (no DataSet needed). Updates the Sigma slider and refreshes the preview.
- **Auto-calibrate from DataSet** — estimates sigma and threshold from a loaded ground-truth segmentation. Updates both sliders.

After either method, review the result and click **Apply to Settings** to push values to the controller.

### Applying results

Click **Apply to Settings** to push the current slider values back to the Object ID Settings. Click **Close** to discard and close the preview window.

---

## 7. Boundary Cleanup Settings

**Accessed via:** SARN Preprocessing row (row 2) › **Settings** › **Boundary Cleanup Settings**

This dialog sets the parameters used to clean up and simplify every generated segmentation boundary (SARN, MRC, and internal segmentation). These parameters affect boundary quality and point count, not object detection — set Object ID Settings first.

### Parameters

| Parameter | Description | Typical range |
|---|---|---|
| **Search Distance (%)** | How far the loop-cleanup pass searches for a boundary point that has drifted back close to an earlier one, as a percentage of the boundary's own point count. Larger objects need a larger window to catch the same size of defect — this scales automatically with each object's size. | 10 – 25 % |
| **Search Distance Ceiling (px)** | Hard upper limit on the search window, regardless of the percentage above. Caps processing time on very large boundaries. | 200 – 800 px |
| **Simplification Epsilon (px)** | Maximum distance a boundary point may deviate from a straightened edge before it is simplified away. Higher values produce simpler boundaries with fewer points; lower values preserve more detail. | 1 – 3 px |

### Auto-calibration

Click **Guided Calibration** to open the interactive preview window. Its **Auto-calibrate from DataSet** button estimates Search Distance % and Ceiling from the boundary sizes already present in your loaded dataset — it requires an existing external segmentation to measure. It does not estimate Simplification Epsilon; tune that visually against the preview.

### Applying settings

Click **Apply** to commit values to the controller.

---

## 8. Boundary Cleanup Guided Calibration

**Accessed via:** Boundary Cleanup Settings › Guided Calibration

Guided Calibration provides an interactive live preview so you can tune boundary-cleanup parameters with immediate visual feedback. It requires a dataset with an existing external segmentation to preview against.

### Preview window

A separate image window opens showing one object's boundary from your loaded dataset overlaid twice: **red** is the object's current boundary (re-expanded to full point resolution so the effect of your settings is visible), **green** is that boundary re-cleaned at the current slider values. The overlay updates each time a slider is released, and the status line reports point counts for both.

> **Note:** because only the final cleaned boundary is saved (not the original, uncleaned version), this preview shows the point-reduction and shape-preservation effect of your settings clearly, but it can't demonstrate loop-artifact removal on a boundary that's already been cleaned once — a Search Distance change producing little visible effect on an already-clean object is expected, not a problem with the tool.

### Sliders

| Slider | Effect |
|---|---|
| **Search Distance (%)** | Loop-cleanup search window as a percentage of the boundary's point count. |
| **Search Distance Ceiling (px)** | Hard cap on the search window in points. |
| **Simplification Epsilon (px)** | Shape-fidelity tolerance in pixels (resolution: 0.1 px per tick). |

The value label next to each slider updates continuously while dragging.

### Auto-calibration

**Auto-calibrate from DataSet** estimates Search Distance % and Ceiling from the boundary sizes already present in your loaded dataset. Review the result in the preview, then click **Apply to Settings** to commit.

### Applying results

Click **Apply to Settings** to push the current slider values back to Boundary Cleanup Settings. Click **Close** to discard and close the preview window.

---

## 9. External Segmentation (SARN)

**Accessed via:** SARN Preprocessing row, Operation Panel

SARN (Segmentation Area Region by Neighborhood) algorithms compute an envelope (external perimeter) around each detected object centre. The envelope approximates the true object boundary by searching outward from the centre along radial lines and identifying the transition from the object's interior to its background.

### Selecting a SARN method

Click **Settings** in the SARN Preprocessing row to choose a method:

| Method | Description |
|---|---|
| **One-Way Contraction** | Searches inward from an outer constraint circle (radius = nearest-neighbour distance) along Bresenham radii to find the dark-to-bright transition. Best for well-separated, roughly circular objects. |
| **One-Way Contraction — Gradient Descent** | Variant of One-Way Contraction that uses gradient descent to refine the boundary, improving accuracy for objects with soft edges. |
| **One-Way Contraction — Exclusion** | Adds exclusion constraints to prevent the envelope from encroaching on neighbouring objects. Useful in dense fields where envelopes would otherwise overlap substantially. |
| **Minimal Boundary** | Computes a minimal convex boundary enclosing all neighbouring centres and image edges. Suited to situations where the object boundary is not detectable directly from intensity. |
| **Multi-Way Reduction Contraction** | Resolves the boundary sector by sector rather than with a single nearest-neighbour radius: each angular region is governed by its most spatially relevant neighbour. Produces boundaries that stay locally correct in every direction, which helps in fields where a cell has neighbours at very different distances on different sides. |

### Running external segmentation

Click **Run** in the SARN Preprocessing row. A progress bar tracks frame-by-frame progress. Click **Run** again (now labelled **Cancel**) to abort.

> **Note:** Object Identification parameters (sigma, threshold) must be set before running, as SARN uses the detected centres as seeds.

After completion the button reads **Clear**, indicating data is loaded.

### Manual editing

Click **Manually Edit** to open the Manual Segmentation editor, where you can add, delete, or merge objects frame by frame. See [Section 13](#13-manual-segmentation-editing).

---

## 10. Subsegmentation (Recursive Panel)

**Accessed via:** Internal Segmentation row › Subsegment button

Subsegmentation enables a second round of identification and segmentation *inside* each parent SARN envelope. The outer parent envelope (from SARN) defines the search region, within which sub-object centres and sub-object boundaries are computed independently.

### Adding a subsegmentation panel

The **Subsegment** button (Internal Segmentation row) is enabled once external segmentation has been run. Clicking it adds a linked child panel below the parent panel. This panel has its own Object ID settings, SARN method, and run controls that operate within the parent envelope context.

### Subsegmentation-specific parameter: Recursive Tolerance

Within a parent object crop the image may contain multiple sub-object candidates at similar intensity levels. The **Recursive Tolerance (%)** parameter (in Object ID Settings on the subsegmentation panel) sets the minimum intensity separation required to distinguish two adjacent sub-object peaks. Increase it if adjacent sub-objects are merging into one detection; decrease it if a single sub-object is being split into two.

### Running subsegmentation

1. Set Object ID parameters on the child panel (including Recursive Tolerance).
2. Click **Run** in the SARN Preprocessing row of the child panel.
3. Review results frame by frame using the Manually Edit button.

### Recursive manual editing

Click **Manually Edit** on the child panel to enter the recursive manual segmentation interface, which shows each parent object in its own cropped canvas. See [Section 14](#14-recursive-manual-segmentation).

---

## 11. Internal Segmentation

**Accessed via:** Internal Segmentation row, Operation Panel

Internal segmentation applies intensity thresholding to each SARN envelope to produce a binary mask of the object interior. This provides a tighter, pixel-accurate boundary within the SARN envelope.

### Selecting a method

Click **Settings** to choose a thresholding method:

| Method | Description |
|---|---|
| **Otsu's Method** | Global threshold minimising intra-class intensity variance. |
| **Restricted Otsu's Method** | Applies Otsu's threshold independently within each SARN envelope region. |
| **Li Method** | Entropy-based threshold (Li & Tam, 1998). |
| **Restricted Li Method** | Per-envelope Li threshold. |
| **Triangle Method** | Threshold based on the triangle algorithm (Zack et al., 1977). |
| **Restricted Triangle Method** | Per-envelope Triangle threshold. |
| **Active Contour** | Iteratively deforms an initial contour to the object boundary using an energy minimisation approach. |
| **CenterPointID** | Uses only the centre point (no boundary computation). |
| **Convert SARN to Segmentation** | Promotes the SARN envelope directly to the internal segmentation boundary without additional thresholding. |

*Restricted* variants compute the threshold locally within each envelope rather than globally across the frame, which is more accurate when object intensities vary across the image.

### Running internal segmentation

Click **Run**. Progress is displayed in the footer progress bar. Click **Cancel** to abort.

### Preview

Click **Preview** to open a view-only display of the internal segmentation results across all frames. This uses the same object-by-object canvas layout as recursive manual editing if the panel is a subsegmentation child panel.

---

## 12. Linkage

**Accessed via:** Linkage Mechanism row, Operation Panel

Linkage connects Segments across frames into continuous tracks (LinkSets). It runs after both external segmentation and (optionally) internal segmentation are complete.

### Selecting a linkage method

Click **Settings** to choose:

| Method | Description |
|---|---|
| **Global Nearest Neighbor** | Assigns each Segment in frame N to its nearest-neighbour Segment in frame N+1 by Euclidean distance between centre points. |
| **Modified Hungarian** | Solves the assignment problem optimally using a modified Hungarian algorithm, minimising total assignment cost across all pairs in each frame transition. |

### Running linkage

Linkage is triggered automatically as part of the pipeline after segmentation is complete. Results are stored as LinkSets in the DataSet.

---

## 13. Manual Segmentation Editing

**Accessed via:** SARN Preprocessing › Manually Edit

The Manual Segmentation editor allows you to correct the automated external segmentation results frame by frame.

### Controls — Main menu state

| Button | Action |
|---|---|
| **Segmentation** | Enter object drawing mode to add new objects. |
| **Modify Objects** | Enter modification mode to delete, merge, split, link, or unlink objects. |
| **Settings** | Open the Settings sub-panel (see below). |
| **End Session** | Close the editor and return to the main window. |

### Drawing a new object

1. Click **Segmentation** to enter drawing mode.
2. Use **Start Object** to begin drawing the outline for the current frame. The active draw tool (Polygon or Freehand) is set in **Settings**.
3. Draw the outline around the object, then click **End Object** to commit it.
4. Use **Next Frame** / **Previous Frame** to advance through frames, drawing the outline for each. The object is tracked across the frames you draw it in.
5. When finished, return to the **Main Menu**.

| Button | Action |
|---|---|
| **Start Object** | Begin a new ROI for the current frame using the configured draw tool. |
| **End Object** | Commit the current outline and advance. |
| **Next Frame** | Advance to the next frame (commits the current outline). |
| **Previous Frame** | Go back one frame. |
| **Restore Selection** | Restore the ROI from the previous frame as a starting point. |
| **Settings** | Open Settings (ROI colour and draw tool). |
| **Main Menu** | Exit drawing mode, discarding any in-progress outline. |

### Modifying existing objects

Click **Modify Objects** to enter modification mode. Click any object outline to select it (it highlights in red); click again to deselect.

| Button | Action |
|---|---|
| **Delete** | Delete the selected object from this and all frames in its track. |
| **Merge** | Merge two selected objects into a single combined outline. |
| **Redraw Segment** | Correct a single frame's outline without deleting the whole track. Select the object, navigate to the frame that needs correcting, click **Redraw Segment**, draw the new outline, then **Apply Redraw** (or **Cancel Redraw** to discard). Every other frame in the track is untouched. |
| **Split** | Draw a bisecting line across the selected object to split it into two tracks. |
| **Link** | Merge two selected tracks into one (blocked if frames overlap). |
| **Unlink** | Split the selected track at the current frame boundary into two separate tracks. |

#### Using Split

1. Click **Split** and click the target object to select it.
2. The panel switches to split-line mode. Draw a straight line ROI across the object.
3. Click **Apply Split** to execute, or **Cancel Split** to abort.

The line defines which side of the object each new seed falls on. Both halves are re-segmented automatically using SARN, and the result propagates forward and backward through the original track.

#### Using Link

Select one segment from each of two separate tracks (both highlight in red), then click **Link**. The tracks are joined in frame order.

#### Using Unlink

Select any segment in the track you want to split. Navigate to the frame at which to break the track, then click **Unlink**. Segments up to and including the current frame form one track; the remainder form a second.

### Settings (Manual Segmentation)

Click **Settings** from the main menu or the segmentation sub-panel to open the Settings panel.

| Setting | Description |
|---|---|
| **ROI Color…** | Opens a colour picker. The chosen colour is applied to all non-selected overlay outlines and to the active draw-tool cursor immediately. Saved across sessions. |
| **Draw Tool** *(segmentation sub-panel only)* | Toggle between **Tool: Polygon** and **Tool: Freehand**. The selection is applied on the next **Start Object** and saved across sessions. |

Click **Back** to return to the previous panel.

### Tips

- Use the ImageJ hand tool to pan the image while in the editor.
- Change the draw tool in **Settings** before clicking **Start Object** if you prefer freehand drawing.
- If object outlines are hard to see against your image, open **Settings → ROI Color…** and pick a contrasting colour.

---

## 14. Recursive Manual Segmentation

**Accessed via:** Child panel › SARN Preprocessing › Manually Edit

The Recursive Manual Segmentation editor shows each parent object in its own cropped canvas, allowing you to draw, delete, or merge sub-objects one by one.

### Window layout

- **Left sidebar** — lists all parent objects. Click any entry to switch to it directly. The active object is highlighted.
- **Main canvas** — shows the cropped image for the current parent object, with existing sub-object outlines overlaid.
- **Bottom strip** — a miniature full-image preview showing the current object's position in context.
- **Control panel** — a floating panel (positioned above the canvas) with action buttons.

### Navigating objects

- Click an object name in the sidebar, or use **Next Segment** in the control panel to advance to the next object.
- The canvas and overlay update immediately when switching objects. Previously drawn sub-objects are preserved exactly.

### Drawing sub-objects

1. Click **Segmentation** in the control panel.
2. Use **Start Object** to begin drawing the sub-object outline for the current frame.
3. Draw the polygon around the sub-object region, then click **End Object**.
4. Use **Next Frame** / **Previous Frame** to draw the sub-object across consecutive frames.
5. Click **Main Menu** when done with this sub-object.

### Modifying sub-objects

Click **Modify** to access:

| Button | Action |
|---|---|
| **Delete Object** | Remove the selected sub-object across all its frames. |
| **Merge Objects** | Combine two sub-objects into one. |
| **Redraw Segment** | Correct a single frame's outline without deleting the whole sub-object. Select the sub-object, navigate to the frame that needs correcting, click **Redraw Segment**, draw the new outline, then **Apply Redraw** (or **Cancel Redraw** to discard). Every other frame is untouched. |

### Settings (Recursive Manual Segmentation)

Click **Settings** from either the main menu or the segmentation sub-panel.

| Setting | Description |
|---|---|
| **ROI Color…** | Opens a colour picker for overlay outlines and the draw-tool cursor. Saved across sessions and shared with the Manual Segmentation window. |
| **Draw Tool** *(segmentation sub-panel only)* | Toggle between Polygon and Freehand tools. Saved across sessions. |

Click **Back** to return.

### Ending the session

Click **End Session** to close the editor. All drawn sub-objects are committed to the child DataSet.

### Keyboard and scroll

- **Mouse wheel** — scrolls through frames within the current object's canvas.
- All navigation and drawing uses standard ImageJ tools.

---

## 15. Main Window — Analysis View

**Accessed via:** DATA ANALYSIS >> button (footer)

The Analysis view collects the DataSet from all Operation Panels and runs quantitative measurements.

### Selecting an analysis method

Each Analysis Panel has a dropdown of available methods:

| Method | Output |
|---|---|
| **General Segmentation Data** | Per-segment measurements (area, perimeter, centre coordinates, frame) for all detected objects. |
| **General Recursive Segmentation Data** | Same as above but for subsegmentation results. |
| **External Segmentation Data** | Detailed measurements derived from the SARN envelope (shape descriptors, boundary coordinates). |
| **Exterior Extraction** | Extracts pixel intensity data from within each SARN envelope. |
| **Principle Component** | PCA-based shape analysis of object boundaries. |
| **Interactions** | Spatial interaction statistics between objects across frames. |
| **Overlay Channels** | Produces overlay images combining segmentation results across channels. |
| **Segmentation Comparer / Comparer 2** | Compares two DataSets to quantify segmentation agreement. |

### Running analysis

Click **Run** in the analysis panel. Progress is shown in the footer progress bar.

### Exporting results

Click **GENERATE RESULTS** to export all analysis output to an Excel workbook (`.xlsx`). One workbook is generated per DataSet. The label "Results exported." confirms completion.

> The output file is saved to the directory specified in the output path field in the header bar.

### Returning to segmentation

Click **<< SEGMENTATION** to return to the Operation Panel view.

---

## 16. Tips and Troubleshooting

### Sigma is too high / too low

- **Too high** (over-blurred): nearby objects merge into one detection; run Guided Calibration and use the Decrease button on Sigma to find where objects separate.
- **Too low** (under-blurred): noise peaks produce many false detections; use the Increase button on Threshold or increase Sigma.

### Objects detected but SARN envelopes look wrong

- Try a different SARN method. One-Way Contraction — Exclusion works better in dense fields.
- Check that **Invert Intensity** is set correctly for your image type.

### Recursive sub-object segmentation not detecting sub-objects

- Increase **Recursive Tolerance (%)** if adjacent sub-objects are merging.
- Lower the **Threshold (%)** on the child panel — sub-objects may be dimmer than the parent object interior.

### Plugin launches slowly or crashes on large stacks

- Use virtual stacks (the plugin opens stacks virtually by default for preview and calibration).
- Avoid running the full pipeline on GPU-accelerated machines without sufficient RAM.

### Recursion panel opens on restart without data

This was a known issue in versions prior to 0.5.0. The recursion panel now only reopens automatically when a DataSet containing embedded child data is loaded.

---

## 17. Known Limitations

- **Analysis Settings** (method-specific configuration) is not yet implemented.
- Large multi-GB stacks may cause out-of-memory errors on machines with limited RAM.
- The **Confluency** metric is not yet included in General Segmentation Data output.
- Comparative analysis methods (Segmentation Comparer) have limited output in the current version.
- The **Cancel** button during segmentation sends an interrupt to the background thread. For computationally intensive SARN methods, the thread may take a moment to stop; the UI resets immediately regardless.

---

*Seg2Tracks — publication pending*
