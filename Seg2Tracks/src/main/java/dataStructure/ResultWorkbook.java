package dataStructure;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;


/**
 * ResultWorkbook manages Excel (.xlsx) workbook creation for exporting segmentation and tracking results.
 * Uses Apache POI to create styled sheets and populate them with calculation results.
 */
public class ResultWorkbook {

	Workbook workbook; // Apache POI XSSFWorkbook
	String [] calculationNames; // Names of calculations (if used)
	ArrayList<Sheet> sheetList; // All sheets in the workbook
	ArrayList<Integer> sheetLines; // Current row number for each sheet
	CellStyle headerStyle; // Reusable style for header rows

	/**
	 * Constructs a new Excel workbook with formatted header style.
	 */
	public ResultWorkbook() {

		workbook = new XSSFWorkbook();
		sheetList = new ArrayList<Sheet>();
		sheetLines = new ArrayList<Integer>();

		// Define and create header row style
		headerStyle = workbook.createCellStyle();
		headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex()); // Light blue background
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		// Define and apply font style for headers
		XSSFFont font = ((XSSFWorkbook) workbook).createFont();
		font.setFontName("Arial");
		font.setFontHeightInPoints((short) 12);
		font.setBold(true);
		headerStyle.setFont(font);
	}

	/*
	public boolean sheetExists(String name)	 {
		if (workbook.getSheet(name) != null) return true;
		return false;
	}
	*/

	/**
	 * Adds a new sheet to the workbook with formatted headers.
	 * @param name sheet name
	 * @param headings column header labels
	 * @return sheet index in sheetList, or index of existing sheet if name already exists
	 */
	public int addSheet(String name, String[] headings) {

		// Check if sheet with this name already exists
		Sheet temp = workbook.getSheet(name);
		if (workbook.getSheet(name) != null) {
			//System.out.println("SHEET EXISTS");
			return sheetList.indexOf(temp);
		}

		Sheet sheet = workbook.createSheet(name);

		/*
		if (workbook.getSheet(name) == null) {
			sheet = workbook.createSheet(name);
		}
		else sheet = workbook.getSheet(name);
		*/

		// Set column widths for readability
		sheet.setColumnWidth(0, 6000);
		sheet.setColumnWidth(1, 4000);

		// Create and format header row
		Row header = sheet.createRow(0);
		Cell headerCell;
		for (int i = 0; i < headings.length; i++) {
			headerCell = header.createCell(i);
			headerCell.setCellValue(headings[i]);
			headerCell.setCellStyle(headerStyle);
		}

		sheetList.add(sheet);
		sheetLines.add(0);

		return sheetList.size() - 1; // Returns index of added sheet
	}

	/**
	 * Adds a row of data to a specified sheet.
	 * TODO: Allow multiple values per cell.
	 * @param sheetIndex which sheet to add to
	 * @param data array of values (String, Double, or Integer)
	 */
	public void addLine(int sheetIndex , Object[] data) {

		sheetLines.set(sheetIndex, sheetLines.get(sheetIndex) +1); // Increment sheet line counter
		Row row = sheetList.get(sheetIndex).createRow(sheetLines.get(sheetIndex)); // Get row in sheet

		Cell cell;
		for (int i=0; i < data.length; i++) {
			cell = row.createCell(i);
			// Write value based on type
			if (data[i] instanceof String) {
				cell.setCellValue((String) data[i]);
				continue;
			}
			if (data[i] instanceof Double) {
				cell.setCellValue((double) data[i]);
				continue;
			}
			if (data[i] instanceof Integer) {
				cell.setCellValue((int) data[i]);
				continue;
			}
		}
	}

	/*
	public void setOverride(boolean override) {
		this.override = override;
	}
	*/

	/**
	 * Gets the underlying Apache POI Workbook object.
	 * @return XSSFWorkbook for file writing or further modification
	 */
	public Workbook getWorkbook() {
		return workbook;
	}


}
