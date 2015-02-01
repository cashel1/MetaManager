package org.newcashel.meta.model;

import java.util.HashMap;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.newcashel.meta.util.POIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// these are defined in the Parameter sheet
public class Parameter {

	private static final Logger logger = LoggerFactory.getLogger(Parameter.class);

	private static HashMap<String, Parameter> parameters = new HashMap<String, Parameter>();
	
	public static final String VALUE_ASSIGNMENT = "V_";
	public static final String PARAMETERVALUE_ASSIGNMENT = "C_";
	
	private String parmName;
	private String assignment;
	private Object defaultValue;
	private Object highDefaultValue;	// set for range default vals	
	private String type;
	private String format;
	
	public Parameter(String parmName, String assignment, String parmValue, String type, String format) {
		this.parmName = parmName;
		setAssignment(assignment);
		this.defaultValue = parmValue;
		this.type = type;
		this.format = format;
		
		// Parms can either provide a value or an additional constraint
		parameters.put(this.getAssignment()+parmName, this);
	}
	
	public static Parameter getValueParameter(String name) {
		return parameters.get(PARAMETERVALUE_ASSIGNMENT + name);
	}
	
	public static Parameter getConstraintParameter(String name) {
		return parameters.get(VALUE_ASSIGNMENT + name);
	}
	
	public String getParmName() {return parmName;}
	public void setParmName(String parmName) {this.parmName = parmName;}
	public String getAssignment() {return assignment;}
	public Object getDefaultValue() {return defaultValue;}
	public void setDefaultValue(Object defaultValue) {this.defaultValue = defaultValue;}
	public String getType() {return type;}
	public void setType(String type) {this.type = type;}
	public String getFormat() {return format;}
	public void setFormat(String format) {this.format = format;}
	public Object getHighDefaultValue() {return highDefaultValue;}
	public void setHighDefaultValue(Object highDefaultValue) {this.highDefaultValue = highDefaultValue;}

	public void setAssignment(String assignment) {
		if (assignment.equalsIgnoreCase("value")) {
			this.assignment = VALUE_ASSIGNMENT;
		} else if (assignment.equalsIgnoreCase("constraint")) {
			this.assignment = PARAMETERVALUE_ASSIGNMENT;
		} else {
			String msg = "invalid assignment value received in Parameter.setAssignemnt() " + assignment;
			logger.error(msg);
		}
	}
	
	public static void load(HSSFWorkbook wb) throws Exception {

		// load the sheet
		Sheet sheet = wb.getSheet("Parameters");
		if (sheet == null) {
			throw new Exception("The Parameter sheet was not found, terminate load process");
		}
		
		Row row = null;
		for (int i = 1; i <= sheet.getLastRowNum(); i++) {
			if ((row = sheet.getRow(i)) != null && POIUtil.getCellValue(row,0) != null && POIUtil.getCellValue(row,0).length() > 0) {
				new Parameter(POIUtil.getCellValue(row,0), POIUtil.getCellValue(row,1),POIUtil.getCellValue(row,2),
						POIUtil.getCellValue(row,3),POIUtil.getCellValue(row,4));
			}
		}
	}
}
