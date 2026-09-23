---
mediawiki: Seg2Tracks
title: Seg2Tracks
categories: [Segmentation, Tracking, Fiji]
artifact: org.zentjm:Seg2Tracks_
doi: 
---

# Seg2Tracks

**Seg2Tracks** is a [Fiji](/software/fiji) plugin for multi-step segmentation and tracking of fluorescently labelled objects in volumetric time-lapse microscopy data. It combines automated segmentation pipelines with manual correction tools and an integrated analysis layer.

---

## Installation

Seg2Tracks is distributed as a standard Fiji plugin JAR.

1. Download `Seg2Tracks_-<version>.jar` from the [releases page](https://github.com/fiji/seg2tracks/releases).
2. Copy the JAR into your `Fiji.app/plugins/` folder.
3. Restart Fiji.
4. Launch via **Plugins › Seg2Tracks**.

---

## Overview

Seg2Tracks guides users through a structured six-stage pipeline. Each stage builds on the previous, and results propagate automatically to subsequent steps. Multiple independent segmentation panels can be run in parallel, and optional sub-segmentation panels allow recursive segmentation of individual identified objects.

### Pipeline summary

| Stage | Description |
|---|---|
| **Object Identification** | Gaussian-blur + local-maxima detection locates candidate objects in each frame. |
| **External Segmentation (SARN)** | Segment-and-re-normalize (SARN) traces an intensity-driven perimeter around each candidate maximum to define the object boundary. |
| **Sub-segmentation** | Optional recursive stage: individual identified cells are cropped and re-segmented at higher detail. |
| **Internal Segmentation** | Identifies sub-structures (e.g. nuclei) inside each externally segmented object. |
| **Linkage** | Connects objects across time frames to form tracks using centroid-proximity and overlap methods. |
| **Analysis** | Measures spatial, morphological, and intensity properties and exports overlays and spreadsheets. |

---

## Usage

### Main window — Segmentation view

Each **segmentation panel** corresponds to one image stack. Panels are added and removed with the **Add Panel** / **Remove Panel** buttons in the bottom toolbar.

Every panel presents its stages in order. Completing a stage unlocks the next. The **DATA ANALYSIS >>** button becomes active once at least one panel has completed linkage.

#### Object Identification

1. Set the image stack path using the **Input** field.
2. Open **ID Settings** to configure detection parameters (sigma, threshold, frame range). Use **Guided Calibration** for a live-preview slider interface; auto-calibration buttons are available to estimate sigma from the image data directly or from an existing DataSet.
3. Click **Run Identification**.

#### External Segmentation (SARN)

Select a SARN method from the drop-down and click **Run External Seg**. Available methods:

| Method | Description |
|---|---|
| **CircleSARN** | Circular envelope, fastest; appropriate for roughly spherical objects. |
| **StarSARN** | Radial rays at fixed angular steps; better for irregular shapes. |
| **RecursiveSARN** | Propagates the prior-frame envelope as a starting estimate for the next frame. |
| **AdaptiveRecursiveSARN** | Recursive SARN with adaptive ray density based on local curvature. |

#### Internal Segmentation

Select a method and click **Run Internal Seg**. Available methods:

| Method | Description |
|---|---|
| **CircleInternal** | Fits a circle to detected internal maxima. |
| **GaussianInternal** | Fits a Gaussian intensity profile to each internal maximum. |

#### Linkage

Select a linkage method and click **Run Linkage**. Available methods:

| Method | Description |
|---|---|
| **CentroidLinkage** | Links nearest centroids across consecutive frames within a configurable search radius. |
| **OverlapLinkage** | Links objects whose perimeters overlap between frames. |

### Main window — Analysis view

Switch to analysis mode with **DATA ANALYSIS >>**. Each **analysis panel** selects an input target directory and an analysis method, then maps image channels to the required inputs.

Available analysis methods:

| Method | Description |
|---|---|
| **ExternalAnalysis** | Measures properties of the externally segmented object perimeter. |
| **InternalAnalysis** | Measures properties of internally segmented structures within each external object. |
| **RecursiveAnalysis** | Measures properties of recursively segmented sub-objects within parent cells. |

Click **Run** to execute. Click **GENERATE RESULTS** to export overlays and measurement spreadsheets to the configured output directory.

### Manual segmentation editing

After external segmentation and linkage, click **Manual Seg** to open the manual correction window. Controls:

| Button | Function |
|---|---|
| **End Object** | Finalise a drawn polygon and add it as a new segment. |
| **Delete Object** | Remove the selected segment. |
| **Merge Objects** | Combine two selected segments into one. |
| **Split / Link / Unlink** | Manual track correction tools *(implementation pending)*. |

### Recursive manual segmentation

Available when a DataSet with recursive child data is loaded. Each identified parent cell is presented in a cropped canvas for independent SARN correction. Use the **Next** button to advance through cells and **Finish** to return results to the parent DataSet.

---

## Parameters

### Object Identification

| Parameter | Default | Description |
|---|---|---|
| Sigma | 3 | Gaussian blur radius (pixels). Larger values suit larger or dimmer objects. |
| Threshold | 50 | Minimum intensity for a local maximum to be accepted as a candidate object. |
| Frame start / end | Full stack | Restricts detection to a sub-range of frames. |
| Invert intensity | false | For objects darker than background. |

### SARN (External Segmentation)

| Parameter | Description |
|---|---|
| Ray count | Number of radial sampling rays used to trace the object boundary. |
| Expansion factor | Multiplier applied to the initial radius estimate before ray tracing. |
| Intensity floor | Minimum normalised intensity below which a ray terminates. |

### Linkage

| Parameter | Description |
|---|---|
| Search radius | Maximum centroid-to-centroid distance (pixels) to consider two objects as the same track (CentroidLinkage). |
| Overlap threshold | Minimum fractional overlap required to link two objects (OverlapLinkage). |

---

## Output

Results are written to the directory specified in the **Output** field in the Analysis view.

| File | Contents |
|---|---|
| `overlay_<stack>.tif` | Original image stack with segmentation overlays drawn in. |
| `measurements_<stack>.xlsx` | Per-object, per-frame measurements (area, perimeter, centroid, intensity statistics). |

---

## Citation

If you use Seg2Tracks in your research, please cite:

> *Publication pending.*

---

## See also

- [Fiji](/software/fiji)
- [TrackMate](/plugins/trackmate) — alternative tracking plugin for simpler workflows
- [MorphoLibJ](/plugins/morpholibj) — complementary morphological analysis tools

---

## Source code

Source code and issue tracker: [https://github.com/fiji/seg2tracks](https://github.com/fiji/seg2tracks)

Developer: Joshua Zent
