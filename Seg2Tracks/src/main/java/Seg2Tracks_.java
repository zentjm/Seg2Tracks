

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.UIManager;

import ij.IJ;
//import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.VirtualStack;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.FileSaver;
import ij.plugin.frame.RoiManager;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;
import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
//import org.zentjm.Linkage3.Link;
//import org.zentjm.Linkage3.Link2;
//import org.zentjm.Segmentation.Morphology;
//import org.zentjm.Segmentation.Polygonz;
//import org.zentjm.Segmentation.Segment;
//import org.zentjm.Segmentation.SortPolygonz;

import gui.Seg2TracksController;
import gui.Seg2TracksModel;

//import fiji.Debug;


/**
 * A program that allows cell analysis. 
 *
 * @author Joshua Zent
 */
//public class Primes_ extends ImagePlus implements PlugIn

@Plugin(type = Command.class, menuPath = "Plugins>Segmentation>Seg2Tracks")
public class Seg2Tracks_ implements Command {
	


	/**
	 * Constructor for the Seg2Tracks_ plugin entry point.
	 */
	public Seg2Tracks_() {

	}
	
	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args
	 *            unused
	 */
	public static void main(String[] args) {

		ImageJ ij = new ImageJ();
		ij.command().run(Seg2Tracks_.class, true);
		
	}
	
/**
	 * Initializes and loads the Seg2Tracks plugin user interface.
	 * Validates ImageJ version compatibility, applies OS-specific UI themes,
	 * and creates the model-controller architecture for the segmentation GUI.
	 */
	@Override
	public void run() {
		// Exit if ImageJ version is too old
		if (IJ.versionLessThan("1.27w")) return;

		// Adapt UI menus and dialogs to match the current operating system's look and feel
		try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

		// Initialize the model-view-controller architecture
		Seg2TracksModel model = new Seg2TracksModel();
		Seg2TracksController controller = new Seg2TracksController(model);
	}
}	
		



		
		
		
		
		
		
				
	
	













