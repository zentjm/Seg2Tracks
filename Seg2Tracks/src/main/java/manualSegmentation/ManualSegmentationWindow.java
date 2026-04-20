package manualSegmentation;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;



/**
 * Custom ImageJ StackWindow for manual segmentation. Extends ImageJ's StackWindow to allow
 * dynamic control over user input (mouse wheel, slice selector) during interactive segmentation workflows.
 * Can disable these controls during drawing phases and re-enable them during navigation/menu phases.
 */
public class ManualSegmentationWindow extends StackWindow {
	
	ManualSegmentationController controller;
	
	
	
	public ManualSegmentationWindow(ImagePlus imagePlus) {
		super (imagePlus);
	}

	public ManualSegmentationWindow(ImagePlus imagePlus, ImageCanvas imageCanvas) {
		super (imagePlus, imageCanvas);
	}
	
	public ManualSegmentationWindow(ImagePlus imagePlus, ManualSegmentationController controller) {
		super (imagePlus);
		this.controller = controller; //TODO: do we really need a controller reference?
	}

	
	//Disables window control when using frame 
	public void setUserInput (boolean activateWindow) {
		
		if (!activateWindow) {
			removeMouseWheelListener(this);
			
			if (cSelector!=null) {
				remove(cSelector);
				cSelector.removeAdjustmentListener(this);
			}
			if (zSelector!=null) {
				remove(zSelector);
				zSelector.removeAdjustmentListener(this);
			}
			if (tSelector!=null) {
				remove(tSelector);
				tSelector.removeAdjustmentListener(this);
			}
		}
		
		if (activateWindow) {	
			addMouseWheelListener(this);
			if (cSelector!=null) {
				add(cSelector);
				//cSelector.removeAdjustmentListener(this);
			}
			if (zSelector!=null) {
				add(zSelector);
				//zSelector.removeAdjustmentListener(this);
			}
			if (tSelector!=null) {
				add(tSelector);
				//tSelector.removeAdjustmentListener(this);
			}
		}
	}

}
