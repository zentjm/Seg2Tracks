# Batch processing — design

*Status: draft, 2026-10-08. Phase 1 in progress. Expect this to change once there is a prototype
to react to.*

## Goals

One GUI-independent pipeline core, with several clients:

1. **Users' batch runs** — replay the whole panel setup on many samples, unattended.
2. **Testing and method comparison** — phantoms with ground truth, scored automatically
   (`mvn test`), plus comparison reports.
3. **AI / scripting integration** — a stable, documented way to run the pipeline and read results
   (SciJava command → macros, scripts, headless Fiji, PyImageJ; machine-readable outputs).
4. **Batch-only analyses** — methods that only make sense across many samples (summaries,
   statistics, derived parameters, representative overlays).

## Concepts

- **Settings** (`PipelineSettings`): everything one panel's run depends on, as plain data — blur
  sigma, threshold, recursive tolerance, invert, edge exclusion, the three cleanup parameters, and
  the SARN / internal segmentation / linkage methods **by class name** (the GUI stores them as list
  positions, which silently change meaning if `seg2tracks.config` is reordered).
- **Panel setup template**: all operation panels (their settings, which steps they run, which panel
  is a subsegmentation of which) and analysis panels (method + which panel feeds each input).
  Exported from the live GUI to a file ("Export setup for batch"), because the GUI's saved
  preferences do not record analysis inputs or subsegmentation links. The same format is reused
  for **run metadata** in outputs (Pre-v1.0 item), plus the Seg2Tracks version.
- **Sample**: one set of matched input files, one per panel (e.g. `…_GFP.tif` + `…_TX Red.tif`).
  Matching, both supported:
  - *folder per sample* — each subfolder is a sample; each panel picks its file by a pattern;
  - *pattern in one folder* — each panel has a pattern with a `{sample}` key.
  The batch dialog previews the matches before running (samples found, unmatched files, samples
  skipped because their files differ in width / height / frames).

## Architecture

```
GUI panels ─┐        Batch command (macro/headless) ─┐        Tests / comparison ─┐
            ▼                                        ▼                            ▼
                         PipelineSettings  /  panel setup template
                                    │
                                    ▼
             Seg2TracksPipeline   (identify → SARN → internal seg → link → filter)
                                    │
             ┌──────────────────────┼─────────────────────────┐
             ▼                      ▼                         ▼
   Tier 1: per-sample        Tier 2: batch analyses     Output writers
   analyses (existing)       (new, across samples)      (spreadsheet, images,
                                                         datasets, metadata, log)
```

### Pipeline core (`pipeline` package)
- `Seg2TracksPipeline` — one method per step, taking the stack, dataset, settings and a progress
  bar; plus `runAll()` for batch. The GUI's `OperationModel` calls these, passing the panel's own
  method instances, so GUI behaviour is unchanged. Batch and tests create fresh instances from the
  class names. `RecursionOperationModel` overrides all five steps (masked cropped stack,
  `childDataSet`, recursion perimeter map, `skipZeroBin`) and is routed through the core in
  phase 3, which will need those as optional step inputs.
- Progress: the algorithms take a Swing `JProgressBar` directly; headless runs pass a private one.
  (A listener interface can replace this later without touching the algorithms first.)

### Batch runner and command
- `BatchRunner`: finds and matches samples, runs panels in dependency order per sample (a
  subsegmentation panel gets its parent's result for the same sample), records per-sample failures
  in the log and carries on ("Sample07 failed: …"), never aborting the whole run.
- `Seg2TracksBatch`: a SciJava command (*Plugins › Segmentation › Seg2Tracks Batch*) with
  `@Parameter`s (template file, input folder, matching mode/patterns, output folder). SciJava
  provides the dialog, macro recording and headless use.

### Analyses
- **Tier 1 (per sample)**: the analysis methods configured in the analysis panels run on each
  sample; their rows go into the combined spreadsheet with a *Sample* column.
- **Tier 2 (batch analyses)**: new interface, registered in `seg2tracks.config`:
  ```java
  interface BatchAnalysisMethod {
      String getName();
      void analyze(BatchResults results, BatchOutput out);  // tables, images, metadata
  }
  ```
  `BatchResults` loads each sample's settings, datasets and tier-1 tables from the batch output
  folder one at a time (100 samples need not fit in memory). Methods that combine geometry across
  samples (e.g. an averaged representative overlay) need the samples to share dimensions, or an
  alignment rule of their own.

### Outputs (per batch run)
```
<output>/
  results.xlsx          tier-1 tables, all samples, Sample column (streaming writer for size)
  batch/                tier-2 outputs (tables, images)
  images/               one overlay per sample (which images exactly: to be decided)
  samples/<name>/       each sample's saved datasets (.s2t) → re-run tier 2 without re-segmenting
  run.properties        template + resolved settings + Seg2Tracks version + date (metadata)
  run.log               per-sample status and errors
```
Limits to respect: `.xlsx` caps sheets at ~1M rows (split or add CSV); POI's in-memory workbook
should be replaced by `SXSSFWorkbook` (streaming) for large batches.

### Shared input checks
One dimension check (width, height, frames) used by the GUI analyses and by batch sample matching.
Today `ChannelMerger` / `Interactions` take dimensions from the first dataset without checking.

## Phases
1. **Core extraction** — `PipelineSettings`, `Seg2TracksPipeline`, GUI routed through them; shared
   dimension check; golden test proving identical results on Phantom28.
2. **Batch, single-level** — template export, sample matching + preview, tier-1 analyses, combined
   spreadsheet, per-sample overlays, saved datasets, run metadata and log.
3. **Subsegmentation in batch** — same pattern for `RecursionOperationModel`; skip children whose
   parent failed.
4. **Tier-2 batch analyses** — framework + a first method; phantom comparison suite as a client.

## Open questions
- Batch dialog layout (panel rows, patterns, preview).
- Which images go in `images/`.
- Exact template / metadata file format (Java properties vs JSON).
