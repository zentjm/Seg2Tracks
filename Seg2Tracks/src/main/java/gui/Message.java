package gui;

import java.awt.Color;

public enum Message {
	DEFAULT( " ", Color.BLACK),
	TYPING ("Typing...", Color.BLACK),
	NO_FILES( "This directory contains no files", Color.RED),
	SINGLE_INPUT( "Single input file selected", Color.BLACK),
	MULTIPLE_INPUT( "Multiple input files selected", Color.RED);

	public final String description;
	public final Color color;
	
	Message ( String description, Color color) {
		this.description = description;
		this.color = color;
	}
	
	public String getDescription() {
		return description;
	}
	
	public Color getColor() {
		return color;
	}
	
}
