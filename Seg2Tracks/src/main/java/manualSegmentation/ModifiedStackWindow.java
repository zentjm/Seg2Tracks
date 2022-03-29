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
	
	
	
