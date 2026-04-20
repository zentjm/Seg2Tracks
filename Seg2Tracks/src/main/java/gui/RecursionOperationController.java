package gui;

/**
 * Controller for the recursive/subsegmentation operation panel.
 * Extends OperationController to support multi-level segmentation workflows
 * where segmented regions from one panel are further segmented in this panel.
 *
 * All behaviour is inherited from OperationController; the recursive logic
 * lives in RecursionOperationModel, which overrides runIt().
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
