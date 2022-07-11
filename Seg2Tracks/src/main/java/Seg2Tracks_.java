

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
	


	//Constructor can be used for initialization
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
	 * Loads the Seg2Tracks Plugin in the GUI
	 */
	@Override
	public void run() {
		if (IJ.versionLessThan("1.27w")) return; // closes if imagej is out of date
		
		//Adapts menus to OS
		try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

		Seg2TracksModel model = new Seg2TracksModel();
		Seg2TracksController controller = new Seg2TracksController(model);

	}
}	
		



		
		
		
		
		
		
				
	
	













