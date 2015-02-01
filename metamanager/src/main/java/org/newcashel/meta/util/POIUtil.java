package org.newcashel.meta.util;

import java.text.SimpleDateFormat;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.newcashel.meta.model.NCClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class POIUtil {

	private static final Logger logger = LoggerFactory.getLogger(POIUtil.class);
	
	/*
	static public String[] getFirstRowVals(Sheet sheet) throws Exception {

		// load the sheet
		if (sheet == null) {
			throw new Exception("The Employee sheet was not found, terminate load process");
		}

		Row row = sheet.getRow(0);

		// array, populated offset positions will have resource field names
		// ASSuming no one will have more than 100 blank rows preceeding the header row
		String[] fieldNames = new String[100];

		// loop thru the header row, each valued cell will be the name of the field
		for (int i = 0; i < 100; i++)	{
			Cell cell = row.getCell(i);
			if (cell == null) continue;
			String val = cell.getStringCellValue();
			if (val == null) continue;
			fieldNames[i] = val;
		}
		return fieldNames;
	}
	*/
	
	public static String getCellValue(Row row, int i) throws Exception {
		Cell cell = row.getCell(i);
		if (cell == null) return null;
		return getCellValue(row, cell);
	}	

	// returns null if empty string
	public static String getPopulatedCellValue(Row row, int i) throws Exception {
		Cell cell = row.getCell(i);
		if (cell == null) return null;
		String str = getCellValue(row, cell);
		if (str.length() == 0) return null;
		return str;
	}	
	
	private static String getCellValue(Row row, Cell cell) throws Exception {

		switch(cell.getCellType()) {

		case HSSFCell.CELL_TYPE_STRING:
			return cell.getStringCellValue();

		case HSSFCell.CELL_TYPE_NUMERIC:

			if (HSSFDateUtil.isCellDateFormatted(cell)) {
				Double date = DateUtil.getExcelDate(cell.getDateCellValue());
				return cell.getDateCellValue().toString();
			} else {
				return new Double(cell.getNumericCellValue()).toString();
			}

		case HSSFCell.CELL_TYPE_BOOLEAN:
			return new Boolean(cell.getBooleanCellValue()).toString();

		case HSSFCell.CELL_TYPE_BLANK:
			return "";
		
		case HSSFCell.CELL_TYPE_FORMULA:
			return new Boolean(cell.getBooleanCellValue()).toString();
	
		// throw error on this one	
		case HSSFCell.CELL_TYPE_ERROR:
		
		}	
		throw new Exception ("Error, getCellValue(), no string conversion available for CELL TYPE " + cell.getCellType());
	}

	static public int convertAnyNumberToInt(Object o) {
		if (o == null || o.toString().length() < 1) return 0;
		return new Double(o.toString()).intValue();
	}

	static public Integer convertAnyNumberToInteger(Object o) {
		if (o == null || o.toString().length() < 1) return 0;
		return new Integer(new Double(o.toString()).intValue());
	}

	static public double convertAnyNumberToDouble(Object o) {
		if (o == null || o.toString().length() < 1) return 0;
		return new Double(o.toString()).doubleValue();
	}
	
}
