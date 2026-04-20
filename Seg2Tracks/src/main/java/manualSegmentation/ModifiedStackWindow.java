package manualSegmentation;

import java.awt.EventQueue;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.*;
import ij.measure.Calibration;
import ij.plugin.frame.SyncWindows;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

/**
 * Custom ImageJ StackWindow for manual segmentation. Extends ImageJ's StackWindow to allow
 * dynamic control over user input (mouse wheel, slice/channel/time selectors) during interactive segmentation.
 * Provides userInput() method to enable/disable UI controls depending on workflow phase.
 */
public class ModifiedStackWindow extends StackWindow {

	AdjustmentListener cAdjust;
	AdjustmentListener zAdjust;
	AdjustmentListener tAdjust;
	
	public ModifiedStackWindow(ImagePlus imp) {
		super (imp);
	}

	public ModifiedStackWindow(ImagePlus imp, ImageCanvas ic) {
		super (imp, ic);
	}
	
	
	
	/** Returns the z-stack scrollbar so it can be embedded in another container. */
	public ij.gui.ScrollbarWithLabel getZSelector() {
		return (ij.gui.ScrollbarWithLabel) zSelector;
	}

	public void userInput (boolean b) {
		
		if (!b) {
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
		
		if (b) {	
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
	
	
	
