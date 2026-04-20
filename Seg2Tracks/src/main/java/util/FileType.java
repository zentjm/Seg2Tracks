package util;


/**
 * Enumeration of file/directory validation states returned by FileSelectionPanel.
 * Used to classify the selected path (valid single file, multiple files, empty directory, invalid, etc.)
 * for appropriate UI feedback and validation logic.
 */
public enum FileType {
	DEFAULT(),
	NOT_DIRECTORY(),
	NO_FILES(),
	SINGLE_FILE(),
	MULTIPLE_FILES();
	
	FileType () {	
	}
}
