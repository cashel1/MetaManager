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

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.newcashel.meta.util.POIUtil;
import org.newcashel.meta.util.UTIL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LaunchParms {

	private static final Logger logger = LoggerFactory.getLogger(LaunchParms.class);
	
	// single launch instance, set in load
	//private static LaunchParms currentLaunch = null;
		
	private String deployMode;
	private String version;
	
	private String rebuildIndex;
	private String schemaFolder;
	
	private ArrayList<String> integrationTests;
	private ArrayList<String> rivers;
	
	public LaunchParms() {}
	
	// the first and only LaunchConfig marked with Current = true will be loaded and assigned to a static instance
	public LaunchParms(String deployMode, String version, String currentLaunchStr,  String rebuildIndex, String schemaFolder,
			String riverList, String integTestList) {

		// only loading the one and only currentLaunch, if multiple launches with true, the first will be current
		if (!(isCurrentLaunch(currentLaunchStr))) return;
		
		this.deployMode = deployMode;
		this.version = version;

		setRebuildIndex(rebuildIndex);
		setSchemaFolder(schemaFolder);
		
		addRiverList(riverList);
		addIntegrationTests(integTestList);
	}

	public String getDeployMode() {return deployMode;}
	public void setDeployMode(String deployMode) {this.deployMode = deployMode;}
	public String getVersion() {return version;}
	public void setVersion(String version) {this.version = version;}
	public ArrayList<String> getIntegrationTests() {return integrationTests;}
	public void setIntegrationTests(ArrayList<String> integrationTests) {this.integrationTests = integrationTests;}
	public ArrayList<String> getRivers() {return rivers;}
	public void setRivers(ArrayList<String> rivers) {this.rivers = rivers;}
	public String getRebuildIndex() {return rebuildIndex;}
	public void setRebuildIndex(String rebuildIndex) {this.rebuildIndex = rebuildIndex.toLowerCase();}
	public String getSchemaFolder() {return schemaFolder;}
	public void setSchemaFolder(String schemaFolder) {this.schemaFolder = schemaFolder;}
	
	public boolean isCurrentLaunch(String currentLaunchStr) {
		if (UTIL.convertBoolean(currentLaunchStr)) {
			return true;
		}
		return false;
	}
	
	public void addIntegrationTests(String integTestStr) {
		String[] tests = UTIL.convertCSVStringToList(integTestStr, true);
		if (tests != null) {	// error logged by util function
			setIntegrationTests(new ArrayList<String>(Arrays.asList(tests)));
		}
	}

	public void addRiverList(String riverStr) {
		String[] riverList = UTIL.convertCSVStringToList(riverStr, true);
		if (riverList != null) {	// error logged by util function
			setRivers(new ArrayList<String>(Arrays.asList(riverList)));
		}
	}
	
	public static LaunchParms load(HSSFWorkbook wb) throws Exception {
		// load the sheet
		HSSFSheet sheet = wb.getSheet("LaunchParms");
		if (sheet == null) {
			throw new Exception("The LaunchParms sheet was not found in BootBook, terminate load process");
		}
				
		Row row = null;
		
		// the first and only LaunchConfig marked with Current = true will be loaded and assigned to a static instance
		try {
			for (int i = 1; i <= 1; i++) {
				if ((row = sheet.getRow(i)) != null && POIUtil.getCellValue(row,0) != null && POIUtil.getCellValue(row,0).length() > 0) {
			
					// skip all but the first row that is current
					// NOTE, if the Sheet columns are changed, change the column offset
					if (!(UTIL.convertBoolean(POIUtil.getCellValue(row, 2)))) continue; 
					
					return new LaunchParms(POIUtil.getCellValue(row,0),POIUtil.getCellValue(row,1),POIUtil.getCellValue(row,2),
						POIUtil.getCellValue(row,3),POIUtil.getCellValue(row,4),POIUtil.getCellValue(row,5),POIUtil.getCellValue(row,6));
				}
			}
			
			String msg = "No launch configuration marked as current, server cannot start";
			logger.error(msg);
			throw new Exception(msg);
			
		} catch (Exception e) {
			String msg = "exception in LaunchParms load " + e.toString();
			logger.error(msg);
			throw new Exception(msg); 
		}
	}

}
