package util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import gui.AnalysisPanel;

public class FileSelectionPanel extends Observable implements ActionListener, DocumentListener {
	JButton buttonFile;
	JTextField textFieldFile;
	Timer timer;
	JPanel panel;
	boolean typing;
	FileType fileType = FileType.DEFAULT;
	File directory = null;
	File file = null;
	
	public FileSelectionPanel(String buttonName, String textFieldInput, JPanel panel) {
		buttonFile = new JButton(buttonName);
		buttonFile.setName(buttonName);
		textFieldFile = new JTextField(textFieldInput, 25);
		this.panel = panel;
		timer = new Timer (1000, this);
		timer.setRepeats(false);
		typing = false;
		initialize();
	}
	
	public void initialize() {
		buttonFile.addActionListener(this);
		textFieldFile.getDocument().addDocumentListener(this);
	}
	
	//Sets the textFieldInput through using the File Chooser
	public void getButtonPath() {
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setDialogTitle("Please select the " + buttonFile.getName() + " directory");
		int result = jfc.showDialog(panel, "Choose");
		jfc.setVisible(true);
		if (result == JFileChooser.APPROVE_OPTION) {
			textFieldFile.setText(jfc.getSelectedFile().getAbsolutePath());
			forceTimerUpdate();
		}
		jfc.setVisible(false);	
	}
	
	//Force timer to update
	public void forceTimerUpdate() {
		timer.getActionListeners()[0].actionPerformed(new ActionEvent(timer, ActionEvent.ACTION_PERFORMED,null));
	}

	//Resets or starts timer
	@Override
	public void insertUpdate(DocumentEvent e) {
		startTimer();	
	}

	//Resets or starts timer
	@Override //Resets or starts timer
	public void removeUpdate(DocumentEvent e) {
		startTimer();	
	}
	
	//Required override
	@Override
	public void changedUpdate(DocumentEvent e) {
		//Do nothing
	}
	
	//Resets the timer if the user is still typing
	public void startTimer() {
		if (!typing) {
			timer.start();
			typing = true;
			return;
		}
		timer.restart();
	}

	//Reacts to timer alert
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == timer) {
			System.out.println("Stopped typing");
			typing = false;
			timer.stop(); 
			checkFileValidity();
			updateModel();
		}
		if (e.getSource() == buttonFile) {
			getButtonPath();
		}
	}
	
	//Checks the type of file selected to determine if it is acceptable
	public void checkFileValidity() {
		
		// creates string arrays for input files, filtering out .DS_Store (mac requirement)
		File directory = new File(textFieldFile.getText());
		File [] files = directory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.equals(".DS_Store");
			}
		});
		
		if (files == null) {
			fileType = FileType.NOT_DIRECTORY;
			return;
		}
		
		if (files.length < 1) {
			fileType = FileType.NO_FILES;
			this.directory = directory;
			return;
		}
		
		if (files.length == 1) {
			fileType = fileType.SINGLE_FILE;
			this.directory = directory;
			this.file = files[0]; //TODO: Batch analysis
			return;
		}
		
		if (files.length > 1) {
			fileType = fileType.MULTIPLE_FILES;
			this.directory = directory;
			return;
		}
	}
	
	//Returns the button
	public JButton getButton() {
		return buttonFile;
	}
	
	//Returns the textField;
	public JTextField getField() {
		return textFieldFile;
	}
	
	//Returns the directory of the selected file
	public File getDirectory() {
		return directory;
	}
	
	//Returns the selected file
	public File getFile() {
		return file;
	}
	
	//Returns the file type
	public FileType getFileType() {
		return fileType;
	}

	//Updates the observers
	public void updateModel() {
		setChanged();
		notifyObservers();
		System.out.println("File Selection Updated");
	}
	
	

	


	
	


	
}


