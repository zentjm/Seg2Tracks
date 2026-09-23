package util;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;

import javax.naming.directory.BasicAttributes;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.lang3.SystemUtils;

import dataStructure.DataSet;

/**
 * Utility for managing Seg2Tracks data directory and autosave functionality.
 * Creates and manages the platform-specific settings/data directory (Mac: ~/Library/Application Support/Seg2Tracks,
 * Windows: %APPDATA%\Seg2Tracks, Linux: ~/Seg2Tracks). Handles DataSet serialization, deserialization,
 * and rolling autosave with limited file retention (max 4 most recent autosaves).
 */
public class FileResourcesUtil implements ActionListener {

	final int autosaveNumber = 4;
	File directory = null;
	String[] autoSaves;
	
	JFileChooser jfc;
	JButton button;
	boolean loadFromOverlay;
	
	public FileResourcesUtil() {
		button = new JButton("Load from Overlay Image"); //TODO: put in own JFile
		getDataSetFileLocation();
		loadFromOverlay = false;
	}
	
	//TODO: Consider using AppDirs on github
	//Generates or returns dir:Seg2tracks
	private void getDataSetFileLocation() {
		File directory = new File(getSystemDirectory() + File.separator + "Seg2Tracks");
		System.out.println("directory: " + directory.toString());
		if (!directory.isDirectory() && !directory.mkdir())
			System.err.println("[Seg2Tracks] Could not create data directory: " + directory);
		this.directory = directory;
	}
	
	//Identifies application data parent directory for dir:Seg2Tracks 
	private static String getSystemDirectory() {
		String homeFile;
		String system = System.getProperty("os.name").toLowerCase();
	    if (system.contains("win")) homeFile = System.getenv("APPDATA"); //TODO: test windows
	    else if (system.contains("mac")) homeFile = 
	    		System.getProperty("user.home") + "/Library/Application Support"; //MAC 
	    else if (system.contains("nux")) homeFile = System.getProperty("user.home");
	    else  homeFile = System.getProperty("user.dir");
	    System.out.println("OS Home file is: " + homeFile);
	    return homeFile;  
	}

	/*
	//Returns load file from dir:Seg2Tracks
	public DataSet loadDataSet(Component panel) {
		File loadFile;
		DataSet dataSet;
		JFileChooser jfc = new JFileChooser(directory);
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jfc.setDialogTitle("Please select the load file");
		int result = jfc.showDialog(panel, "Choose");
		jfc.setVisible(true);
		if (result == JFileChooser.APPROVE_OPTION) {
			loadFile = jfc.getSelectedFile();
			try {
				ObjectInputStream objectIn = new ObjectInputStream(
						new BufferedInputStream(new FileInputStream(loadFile)));
				dataSet = (DataSet) objectIn.readObject();
				objectIn.close();
				return dataSet;
			}
			catch(Exception exc){
				exc.printStackTrace(); // If there was an error, print the info.
				//System.out.println("Did not load file");
			}
		}
		return null;
	}
	*/

	
	//Returns load file from DataSet or Overlay
	public DataSet loadData(Component panel) {
		File loadFile;
		DataSet dataSet;
		jfc = new JFileChooser(directory);
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		//button.addActionListener(this);
		//jfc.setAccessory(button);
		jfc.setDialogTitle("Please select the load file");
		int result = jfc.showDialog(panel, "Choose");
		jfc.setVisible(true);
		if (result == JFileChooser.APPROVE_OPTION) {
			loadFile = jfc.getSelectedFile();
			dataSet = loadFromOverlay ? loadDataSetFromOverlay(loadFile) :
				loadDataSet(loadFile);
			return dataSet;
		}
		return null;
	}
	
	//Loads from a DataFile
	public DataSet loadDataSet (File loadFile) {
		try (ObjectInputStream objectIn = new CompatObjectInputStream(
				new BufferedInputStream(new FileInputStream(loadFile)))) {
			return (DataSet) objectIn.readObject();
		}
		catch(Exception exc){
			exc.printStackTrace(); // If there was an error, print the info.
			System.out.println("Did not load file");
		}
		return null;
	}

	/**
	 * ObjectInputStream that tolerates serialVersionUID drift on our own data classes.
	 * <p>Several {@code dataStructure} classes had an explicit {@code serialVersionUID = 1L}
	 * added in v0.5.1 (notably {@link dataStructure.LinkSetModel}); before that the JVM used
	 * an auto-computed UID. That change silently broke loading of every dataset saved by an
	 * earlier build. Because those edits only added the UID constant — the serialized field
	 * layouts are unchanged — substituting the local class descriptor when the name resolves
	 * lets us load old <em>and</em> new files interchangeably without corrupting field data.
	 */
	private static class CompatObjectInputStream extends ObjectInputStream {
		CompatObjectInputStream(InputStream in) throws IOException { super(in); }

		@Override
		protected java.io.ObjectStreamClass readClassDescriptor()
				throws IOException, ClassNotFoundException {
			java.io.ObjectStreamClass streamDesc = super.readClassDescriptor();
			try {
				Class<?> local = Class.forName(streamDesc.getName(), false, getClass().getClassLoader());
				java.io.ObjectStreamClass localDesc = java.io.ObjectStreamClass.lookup(local);
				// Only override on UID mismatch, and only for our own serializable classes,
				// so JDK/library descriptors are always read exactly as written.
				if (localDesc != null
						&& streamDesc.getName().startsWith("dataStructure.")
						&& localDesc.getSerialVersionUID() != streamDesc.getSerialVersionUID()) {
					return localDesc;
				}
			} catch (ClassNotFoundException ignore) {
				// fall through — let the default resolution report the missing class
			}
			return streamDesc;
		}
	}
		
	//Loads from Overlay
	public DataSet loadDataSetFromOverlay(File loadFile) {
		DataSet dataSet;
		try {
			FileLoadFromOverlay loadOverlay = new FileLoadFromOverlay(loadFile);
			dataSet = loadOverlay.run();
			return dataSet;
		}
		catch(Exception exc){
			exc.printStackTrace(); // If there was an error, print the info.
			System.out.println("Did not load file");
		}
		return null;
	}

	
	
	
	
	
	//Saves file to dir:Seg2Tracks
	public boolean saveDataSet(Component panel, DataSet dataSet) {
		String name = JOptionPane.showInputDialog(panel, "Input save file name", "Save", JOptionPane.OK_CANCEL_OPTION);
		if (name == null || name.trim().isEmpty()) return false;
		try {
			File saveFile = new File(directory + File.separator + name);
			try (FileOutputStream fileStream = new FileOutputStream(saveFile);
			     ObjectOutputStream objectStream = new ObjectOutputStream(fileStream)) {
				objectStream.writeObject(dataSet);
			}
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	//Autosaves manual progress
	public boolean autosaveDataSet(DataSet dataSet) {
		//gets the current list of autosave files
		File[] list = directory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("Autosave_");
			}
		});
		if (list == null) list = new File[0]; // listFiles() returns null on I/O error
		//Deletes autosaves older than the first four
		if (list.length > autosaveNumber) {
			//sorts files by creation date
			Arrays.sort(list, new Comparator<File>() {
				public int compare(File a, File b) {
					FileTime timeA;
					FileTime timeB;
					try {
						timeA = Files.readAttributes(a.toPath(),BasicFileAttributes.class).creationTime();
						timeB = Files.readAttributes(b.toPath(),BasicFileAttributes.class).creationTime();
						return (timeB.compareTo(timeA));
					} catch (IOException e) {
						e.printStackTrace();
					}
					return 0;
				}
		
			});
			
			//deletes all files greater than the earliest four
			for (int i = autosaveNumber; i < list.length; i++) {
				list[i].delete();
			}
		}
		Calendar time = Calendar.getInstance();
		int day = time.get(Calendar.DAY_OF_MONTH);
		int hour = time.get(Calendar.HOUR_OF_DAY);
		int minute = time.get(Calendar.MINUTE);
		int second = time.get(Calendar.SECOND);
		
		//Adds new autosave file
		try {
			File saveFile = new File(directory + File.separator + "Autosave_" + day + "-" + hour + "-" + minute + "-" + second);
			try (FileOutputStream fileStream = new FileOutputStream(saveFile);
			     ObjectOutputStream objectStream = new ObjectOutputStream(fileStream)) {
				objectStream.writeObject(dataSet);
			}
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == button) {
			loadFromOverlay = !loadFromOverlay;
			if (loadFromOverlay) button.setText("Load from DataSet Save"); //TODO: mods for image file location.
			if (!loadFromOverlay) button.setText("Load from Overlay Image");
			
		}
	}
	
	
	
	
}
