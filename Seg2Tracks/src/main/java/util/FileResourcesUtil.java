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
		if (!directory.isDirectory()) directory.mkdir();
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
				System.out.println("Did not load file");
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
		DataSet dataSet;
		try {
			ObjectInputStream objectIn = new ObjectInputStream(
					new BufferedInputStream(new FileInputStream(loadFile)));
			dataSet = (DataSet) objectIn.readObject();
			objectIn.close();
			return dataSet;
		}
		catch(Exception exc){
			exc.printStackTrace(); // If there was an error, print the info.
			System.out.println("Did not load file");
		}
		return null;
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
		String name =  JOptionPane.showInputDialog(panel, "Input save file name", "Save", JOptionPane.OK_CANCEL_OPTION);
		try { 
			File saveFile = new File (directory + File.separator + name);
			FileOutputStream fileStream = new FileOutputStream(saveFile);
			ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);
			objectStream.writeObject(dataSet);
			objectStream.close();
			return true;
		}
		catch(Exception e) {
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
			File saveFile = new File (directory + File.separator + "Autosave_" + day + "-" + hour + "-" + minute + "-" + second);
			FileOutputStream fileStream = new FileOutputStream(saveFile);
			ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);
			objectStream.writeObject(dataSet);
			objectStream.close();
			return true;
		}
		catch(Exception e) {
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
