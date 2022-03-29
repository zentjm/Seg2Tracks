package dataStructure;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;


public class ResultWorkbook {

	Workbook workbook;
	String [] calculationNames;
	ArrayList<Sheet> sheetList;
	ArrayList<Integer> sheetLines;
	CellStyle headerStyle;
	
	boolean override;
	boolean overrideChecked;
	
	public ResultWorkbook() {
		
		override = false;
		overrideChecked = false;
		
		workbook = new XSSFWorkbook();
		sheetList = new ArrayList<Sheet>();
		sheetLines = new ArrayList<Integer>();
		
		//Header Row Style
		headerStyle = workbook.createCellStyle();
		headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		//Font Style
		XSSFFont font = ((XSSFWorkbook) workbook).createFont();
		font.setFontName("Arial");
		font.setFontHeightInPoints((short) 12);
		font.setBold(true);
		headerStyle.setFont(font);
	}
	
	public boolean sheetExists(String name)	 {
		if (workbook.getSheet(name) != null) return true;
		return false;
	}
	
	public int addSheet(String name, String[] headings) {
	
		Sheet sheet = null;
		if (workbook.getSheet(name) == null) {
			sheet = workbook.createSheet(name);
		}
		else {
			sheet = workbook.getSheet(name);
			if (override) {		
				for (int i = 0; i < sheet.getLastRowNum(); i++) {
					sheet.removeRow(sheet.getRow(i));
				}	
			}
		}
			
		
		sheet.setColumnWidth(0, 6000);
		sheet.setColumnWidth(1, 4000);
		
		Row header = sheet.createRow(0);
		Cell headerCell;
		for (int i = 0; i < headings.length; i++) {
			headerCell = header.createCell(i);
			headerCell.setCellValue(headings[i]);
			headerCell.setCellStyle(headerStyle);
		}
	
		sheetList.add(sheet);
		sheetLines.add(0);
		
		return sheetList.size() - 1; //returns index of added sheet. 
	}
	
	//TODO: Allow multiple values
	public void addLine(int sheetIndex , Object[] data) {
		
		sheetLines.set(sheetIndex, sheetLines.get(sheetIndex) +1); //iterate sheet line
		Row row = sheetList.get(sheetIndex).createRow(sheetLines.get(sheetIndex)); //get sheet row
		
		Cell cell;
		for (int i=0; i < data.length; i++) {
			cell = row.createCell(i);
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
	
	
	public void setOverride(boolean override) {
		this.override = override;
	}
	

	public Workbook getWorkbook() {
		return workbook;
	}
	

}
