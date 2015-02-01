/*
 * Licensed to NewCashel under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. NewCashel licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.newcashel.meta.model;

import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
//import org.newcashel.service.ElasticQuery;
import org.newcashel.meta.util.POIUtil;
import org.newcashel.meta.util.UTIL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class NCClass implements Serializable {

	private static final int CLASSNAME_OFFSET = 0;
	private static final int SUPERCLASS_NAME_OFFSET = 1;
	private static final int PARENTCLASS_OFFSET = 2;
	private static final int PRIMARYKEY_OFFSET = 3;
	private static final int PERCOLATE_OFFSET = 4;
	private static final int GROUP_CONSTRAIN_OFFSET = 5;
	private static final int ATTRIBUTE_OFFSET = 6;
	private static final int TYPE_OFFSET = 7;
	private static final int VERSION_OFFSET = 8;
	private static final int LABEL_OFFSET = 9;
	private static final int INDEXNAME_OFFSET = 10;
	private static final int STORE_OFFSET = 11;
	private static final int INDEX_OFFSET = 12;
	private static final int INCLUDEINALL_OFFSET = 13;
	private static final int PRECISIONSTEP_OFFSET = 14;
	private static final int DATEFORMAT_OFFSET = 15;
	private static final int FIELDDATAFORMAT_OFFSET = 16;
	private static final int DOCVALUES_OFFSET = 17;
	private static final int BOOST_OFFSET = 18;
	private static final int NULLVALUE_OFFSET = 19;
	private static final int TERMVECTOR_OFFSET = 20;
	private static final int ANALYZER_OFFSET = 21;
	private static final int INDEX_ANALYZER_OFFSET = 22;
	private static final int SEARCH_ANALYZER_OFFSET = 23;
	private static final int IGNOREABOVE_OFFSET = 24;
	private static final int POSITIONGAP_OFFSET = 25;
	private static final int IGNOREMALFORMED_OFFSET = 26;
	private static final int COERCENUMBER_OFFSET = 27;
	private static final int BINARYCOMPRESS_OFFSET = 28;
	private static final int COMPRESSTHRESHOLD_OFFSET = 29;
	private static final int JAVA_CLASS_OFFSET = 30;
		
	//private static final long serialVersionUID = -6881574265686003184L;
	private static final Logger logger = LoggerFactory.getLogger(NCClass.class);
	
	private static HashMap<String, NCClass> classes = new HashMap<String, NCClass>();
	
	// used to access the sub class when serializing JSON (key is the field collection name)
	private static HashMap<String, String> subTypes = new HashMap<String, String>();
	
	private HashMap<String, Attribute> attributes = new HashMap<String, Attribute>();
	
	// key is the label (must be unique per class), return attribute
	private HashMap<String, Attribute> labels = new HashMap<String, Attribute>();
	
	//private ArrayList<String> indexes = new ArrayList<String>(2);
	private ElasticAlias assignedIndexAlias; 	// assigned at runtime based on MetaBook config (mode, version)
	
	private String className;
	private String classParent;
	
	@JsonIgnore()
	private NCClass superClass;
	
	private String superClassName;
	private String javaClassName;
	private String primaryKey;
	private boolean percolate;		// if true, ES add/update will follow up on perq triggers
	private boolean groupConstrain;	// if true, user's assigned groups will be used to constrain search results
	
	
	public NCClass() {}
	
	public static HashMap<String, NCClass> getNCClasses() {return classes;};
	
	public static NCClass getNCClass(String key) {return classes.get(key);}
	
	public static NCClass getSubClass(String key) {
		String className =  subTypes.get(key);
		return getNCClass(className);
	}
	
	public NCClass getSuperClass() {
		if (superClass == null) {
			if (superClassName == null) return null;	// no super class
			this.superClass = getNCClass(getSuperClassName());
			if (superClass == null) {
				logger.debug("no super class for " + getClassName());
			}
		}
		return superClass;
	}
		
	public Attribute getAttribute(String key) {
		Attribute atb = attributes.get(key);
		if (atb != null) return atb;
		NCClass superCls = getSuperClass(); 
		if (superCls != null) return superCls.attributes.get(key);
		return null;
	}

	public String getAggregationTag(String atbStr) throws Exception {
		Attribute atb = getAttribute(atbStr);
		if (atb == null) {
			throw new Exception("invalid atb, cannot create aggregation tag " + atbStr);
		}
		return getClassName() + "_" + atbStr; 
	}
	
	public HashMap<String, Attribute> getSuperAttributes() {
		NCClass superClass = getSuperClass();
		if (superClass != null) return superClass.getAttributes();
		return null; 
	}
	
	public HashMap<String, Attribute> getAttributes() {return attributes;}
	
	public Attribute getAttributeByLabel(String key) {return labels.get(key);}
 
	public static String getElasticMeta(String className) throws Exception {
		NCClass ncCls = getNCClass(className);
		if (ncCls == null) {
			throw new Exception("Attempt to build an Elastic Type without any definition for " + className);
		}
		return null;
	}
	
	public static void load(HSSFWorkbook wb, LaunchParms launchParm) throws Exception {

		// load the sheet
		Sheet sheet = wb.getSheet("ClassAttributes");
		if (sheet == null) {
			throw new Exception("The ClassAttributes sheet was not found in the MetaBook, terminate load process");
		}

		//String[] fieldNames = POIUtil.getFirstRowVals(sheet);

		Class cls = Class.forName("org.newcashel.meta.model.NCClass");
		Class[] parmString = new Class[1];	

		Row row = null;

		try {

			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				// skip blank rows between class attributes
				row = sheet.getRow(i);
				
				if (row != null && (POIUtil.getCellValue(row,ATTRIBUTE_OFFSET) == null || 
						POIUtil.getCellValue(row,TYPE_OFFSET).length() < 1)) continue;

				// get the size of the cell, the length will be the number of atbs in the class
				// determine if the next Cell to the left is a separate Cell or part of a CellRangeAddress
				Cell cell = row.getCell(0, Row.RETURN_BLANK_AS_NULL);
				if (cell == null) {
					continue;
				}

				CellRangeAddress cra = getCellRangeAddress(sheet, row.getRowNum(), 0);
				if (cra == null) {
					return;
				}

				// instantiate the NCClass instance
				NCClass ncClass = new NCClass();
				ncClass.setClassName(POIUtil.getCellValue(row, CLASSNAME_OFFSET));
				//logger.info("loading NCClass " + ncClass.getClassName());

				ncClass.setSuperClassName(POIUtil.getCellValue(row, SUPERCLASS_NAME_OFFSET));
				ncClass.setClassParent(POIUtil.getCellValue(row, PARENTCLASS_OFFSET));
				ncClass.setPrimaryKey(POIUtil.getCellValue(row, PRIMARYKEY_OFFSET));
				ncClass.setPercolate(new Boolean(POIUtil.getCellValue(row, PERCOLATE_OFFSET)));
				ncClass.setGroupConstrain(new Boolean(POIUtil.getCellValue(row, GROUP_CONSTRAIN_OFFSET)));

				// not throwing java class errors, may not be significant to load context
				
				// TODO, if NO_VERIFY_JAVA_CLASS is true, skip validation
				// TODO, if NO_VERF true and blank 
				
				String javaClassName =  POIUtil.getCellValue(row, JAVA_CLASS_OFFSET);
				if (javaClassName.endsWith("Person.class")) {
					System.out.println("OKK");
				}
				Class<?> javaCls = null; 
				if (javaClassName != null && javaClassName.length() > 0) {
					ncClass.setJavaClassName(javaClassName);
					try {
						javaCls = Class.forName(javaClassName);
					} catch (Exception e) {
						logger.error("Java class specified but cannot be loaded for " + ncClass.getClassName() + ", " + javaClassName);
					}
				} else {
					logger.info("no java class specified for class " + ncClass.getClassName());
				}

				classes.put(ncClass.getClassName(), ncClass);
				logger.info("Adding class " + ncClass.getClassName());
	
				// loop for all the rows in the cell range
				for (i = cra.getFirstRow(); i <= cra.getLastRow(); i++) {
					row = sheet.getRow(i);
					if (row == null) {
						return;	// range iteration complete
					}
					cell = row.getCell(ATTRIBUTE_OFFSET, Row.RETURN_BLANK_AS_NULL);
					if (cell == null) continue;

					String atbName = POIUtil.getCellValue(row, ATTRIBUTE_OFFSET);
					String version = POIUtil.getCellValue(row, VERSION_OFFSET);
				
					// if  no version id and the atb has not been set, then set it
					// if a version and it matches the build version, set/overwrite the value 
					Attribute atb = null;

					// if version id is set and matches the launchParm setting, use it else skip 
					// a non-versioned atb may be encountered first, reuse it if received a versioned one
					if (version != null && version.length() > 0) {
						if (!(launchParm.getVersion().equals(version))) {
							continue;
						}
						logger.debug("add version specific atb " + ncClass.getClassName() + ", " + atbName + ", " + version);
						// if a default version has already been established, use it else create one
						atb = ncClass.getAttribute(atbName);
						if (atb == null) {
							atb = new Attribute();
						}
					} else {	// no version, use existing if already set to the current version
						atb = ncClass.getAttribute(atbName);
						if (atb == null) {
							atb = new Attribute();
						} else continue;	// already established a version specific atb, ignore non-versioned entry
					}

					// create the Attributes and add to the class instance
					// TODO, verify not null on these required values, user may override Excel edits
					atb.setName(POIUtil.getCellValue(row, ATTRIBUTE_OFFSET));
					atb.setType(POIUtil.getCellValue(row, TYPE_OFFSET));
					atb.setLabel(POIUtil.getCellValue(row, LABEL_OFFSET));
					atb.setIndexName(POIUtil.getPopulatedCellValue(row, INDEXNAME_OFFSET));
					
					//logger.info("added NCClass atb " + ncClass.getClassName() + ", " + atb.getName());
					
					// defaults to false
					atb.setStore(UTIL.convertBoolean(POIUtil.getCellValue(row, STORE_OFFSET)));
					/*
					String storeVal = POIUtil.getPopulatedCellValue(row, STORE_OFFSET);
					if (storeVal != null) {
						atb.setStore(new Boolean(storeVal));
					}
					*/
					
					// analyzed is default value, will tokenize field
					String indexVal = POIUtil.getPopulatedCellValue(row, INDEX_OFFSET);
					if (indexVal != null) {
						atb.setIndex(indexVal);
					}
					
					// default is true, don't set unless value is not
					String includeInAll = POIUtil.getPopulatedCellValue(row, INCLUDEINALL_OFFSET);
					if (includeInAll != null && includeInAll.equalsIgnoreCase("no")) {
						atb.setIncludeInAll(false);
					} 
					
					// default varies, based on the numeric type
					// TODO, verify numeric field
					String precision = POIUtil.getPopulatedCellValue(row, PRECISIONSTEP_OFFSET);
					if (precision != null) {
						atb.setPrecision(new Integer(precision));
					} 

					String dateFormat = POIUtil.getPopulatedCellValue(row, DATEFORMAT_OFFSET);
					if (dateFormat != null) {
						atb.setDateFormat(dateFormat);
					}
					
					String fieldDataFormat = POIUtil.getPopulatedCellValue(row, FIELDDATAFORMAT_OFFSET);
					if (fieldDataFormat != null) {
						atb.setFieldDataFormat(fieldDataFormat);
					}
					
					atb.setDocValues(UTIL.convertBoolean(POIUtil.getCellValue(row, DOCVALUES_OFFSET)));
					
					String boost = POIUtil.getPopulatedCellValue(row, BOOST_OFFSET);
					if (boost != null) {
						atb.setBoost(new Double(boost));
					}
					
					// defaults to not adding the field to the JSON string
					String nullVal = POIUtil.getPopulatedCellValue(row, NULLVALUE_OFFSET);
					if (nullVal != null) {
						atb.setNullValue(nullVal);
					}
					
					String termVector = POIUtil.getPopulatedCellValue(row, TERMVECTOR_OFFSET);
					if (termVector != null) {
						atb.setTermVector(termVector);
					}
					
					String analyzer = POIUtil.getPopulatedCellValue(row, ANALYZER_OFFSET);
					if (analyzer != null) {
						atb.setAnalyzer(analyzer);
					}
					
					String indexAnalyzer = POIUtil.getPopulatedCellValue(row, INDEX_ANALYZER_OFFSET);
					if (indexAnalyzer != null) {
						atb.setIndexAnalyzer(indexAnalyzer);
					}
					
					String searchAnalyzer = POIUtil.getPopulatedCellValue(row, SEARCH_ANALYZER_OFFSET);
					if (searchAnalyzer != null) {
						atb.setSearchAnalyzer(searchAnalyzer);
					}
					
					atb.setIgnoreAbove(UTIL.convertAnyNumberToInt(POIUtil.getCellValue(row, IGNOREABOVE_OFFSET)));
					atb.setPositionOffset(UTIL.convertAnyNumberToInt(POIUtil.getCellValue(row, POSITIONGAP_OFFSET)));
					atb.setIgnoreMalformed(UTIL.convertBoolean(POIUtil.getCellValue(row, IGNOREMALFORMED_OFFSET)));
					atb.setCoerceNumber(UTIL.convertBoolean(POIUtil.getCellValue(row, COERCENUMBER_OFFSET)));
					atb.setBinaryCompress(UTIL.convertBoolean(POIUtil.getCellValue(row, BINARYCOMPRESS_OFFSET)));
					atb.setCompressThreshold(UTIL.convertAnyNumberToInt(POIUtil.getCellValue(row, COMPRESSTHRESHOLD_OFFSET)));
					
					
					// TODO, all all the others
					
					//atb.setStore(UTIL.convertBoolean(POIUtil.getCellValue(row, STORE_OFFSET)));
					
					
					if (atb.getType().equalsIgnoreCase("SubType")) {
						subTypes.put(atb.getName(), atb.getLabel());
					} else {
						// save the attribute
						ncClass.attributes.put(atb.getName(), atb);
						ncClass.labels.put(atb.getLabel(), atb);

						// if java class, verify the field accessibility
						if (javaCls != null) {
							Field field = null;
							Class<?> current = javaCls;
							while (!(current.getName().equals("java.lang.Object"))) {
								try {
									field = current.getDeclaredField(atb.getName());
									atb.setField(field);
									//atb.setField(current.getDeclaredField(atb.getName()));
									break;
								} catch (Exception e) {
									//System.out.println("java reflection warning, class/field not found, checking super class " + cls.getName() + ", " + atb.getName());
									current = current.getSuperclass();
									continue;
								}
							}

							if (field != null) {
								field.setAccessible(true);
							}
						}
					}
				}
				i--;	// continue the loop on the prior row
			}
		} catch (Exception e) {
			String msg = "exception in NCClass load " + e.toString();
			logger.error(msg);
			throw new Exception(msg);
		}
	}
	
	private static CellRangeAddress getCellRangeAddress(Sheet sheet, int row, int col) {
		for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
			CellRangeAddress cellRange = sheet.getMergedRegion(i);
			if (cellRange.isInRange(row, col)) return cellRange;   
		}
		return null;
	}


	public String getClassName() {return className;}
	public void setClassName(String className) {this.className = className;}
	public String getClassParent() {return classParent;}
	public void setClassParent(String classParent) {this.classParent = classParent;}
	public String getJavaClassName() {return javaClassName;}
	public void setJavaClassName(String javaClassName) {this.javaClassName = javaClassName;}
	
	// UTIL strips and trims the primary key string  
	// prop names that comprise the  primary key are returned 
	// TODO, strip and reassemble the key string in the set function
	public String[] getPrimaryKeys() throws Exception {
		if (primaryKey == null) return null;
		//String[] keys = UTIL.convertStringToList(primaryKey, ",");
		String[] keys = UTIL.convertCSVStringToList(primaryKey, false);
		return keys;
	}

	public static String getPrimaryKeyValue(String[] keys, Inst inst) throws Exception {
		try {
			StringWriter sw = new StringWriter();
			for (int i = 0; i < keys.length; i++) {
				Object value = PropertyUtils.getProperty(inst, keys[i]);
				if (value == null) {
					return null;	// primary key part may not be available, ES will assign id
				}
				sw.write(value.toString());
				if ((i + 1) < keys.length) {
					sw.write("_");
				}
			}
			sw.flush();
			return sw.toString();

		} catch (Exception e) {
			String msg = "primary key build failed on this class: " + inst.getType(); 
			logger.error(msg);
			throw new Exception(msg);
		}
	}
	// note, this returns a csv string
	public String getPrimaryKey() {return primaryKey;}
	public void setPrimaryKey(String primaryKey) {this.primaryKey = primaryKey;}
	
	public HashMap<String, String> getSubTypes() {return subTypes;}
	
	public void setAttributes(HashMap<String, Attribute> attributes) {this.attributes = attributes;}
	public String getSuperClassName() {return superClassName;}
	public void setSuperClassName(String superClassName) {this.superClassName = superClassName;}
	
	public boolean isPercolate() {return percolate;}
	public void setPercolate(boolean percolate) {this.percolate = percolate;}
	public boolean isGroupConstrain() {return groupConstrain;}
	public void setGroupConstrain(boolean groupConstrain) {this.groupConstrain = groupConstrain;}

	
	public ElasticAlias getAssignedIndexAlias() {return assignedIndexAlias;}

	// called by ElasticMetaManager at startup, a given Type is assigned to one and one only alias
	public void setAssignedIndexAlias(ElasticAlias assignedIndex) {
		this.assignedIndexAlias = assignedIndex;
	}
	
	/*
	public String getQualifiedAtbName(String atbStr) {
		StringWriter sw = new StringWriter();
		sw.write(getClassPrefix());
		sw.write(".");
		sw.write(atbStr);
		return sw.toString();
	}
	*/

	
	/*
	public void setIndexes(ArrayList<String> indexes) {this.indexes = indexes;}
	public void setIndexes(String indexList) {
		String[] s = indexList.split(",");
		for (int i = 0; i < s.length; i++) {
			indexes.add(s[i].toLowerCase());
		}
	}
	*/
}
