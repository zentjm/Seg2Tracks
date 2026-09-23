package gui;

/**
 * Controller for the recursive/subsegmentation operation panel.
 * Extends OperationController to support multi-level segmentation workflows
 * where segmented regions from one panel are further segmented in this panel.
 *
 * All behaviour is inherited from OperationController; the recursive logic
 * lives in RecursionOperationModel, which overrides runIt().
 *
 * <h3>TODO: Override {@code runModifyInternal()} for recursive preview</h3>
 * The inherited {@code runModifyInternal()} launches a flat
 * {@link manualSegmentation.ManualSegmentationController} that is unaware of the
 * cell-by-cell recursive structure.  This should be overridden here to launch a
 * read-only variant of {@link manualSegmentation.RecursionManualController} that:
 * <ul>
 *   <li>Iterates cells in the same cropped-canvas layout as the editable controller.</li>
 *   <li>Displays {@code internalPerimeter} (final void boundary) where available,
 *       falling back to {@code externalPerimeter} (SARN envelope) — the inverse of
 *       the editable controller's display priority.</li>
 *   <li>Disables all drawing and modification actions (no Start Object, Delete, Merge).</li>
 * </ul>
 */
public class RecursionOperationController extends OperationController {

	/**
	 * Constructor for the recursive operation panel controller.
	 * @param controller  Reference to the parent Seg2TracksController
	 * @param model       The RecursionOperationModel for this panel
	 * @param panelNumber The panel index in the recursion hierarchy (always > 0)
	 * @param initialLoad true if loading from saved preferences, false if newly added
	 */
	public RecursionOperationController(Seg2TracksController controller, OperationModel model,
			int panelNumber, boolean initialLoad) {
		super(controller, model, panelNumber, initialLoad);
	}
}
