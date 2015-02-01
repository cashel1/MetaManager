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

import java.util.HashMap;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.newcashel.meta.util.POIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class River {

	private static final Logger logger = LoggerFactory.getLogger(River.class);

	private static HashMap<String, River> rivers = new HashMap<String,River>();
	
	private String name;
	private String configFile;
	private long runMinuteMax;		// input as seconds, convert to mills
	
	public River() {}
	
	public River(String name, String configFile, String runMinuteMax) {
		this.name = name;
		this.configFile = configFile;
		addRunMinuteMax(runMinuteMax);
	}
	
	public static River getRiver(String name) {return rivers.get(name);}
	
	public static void load(HSSFWorkbook wb) throws Exception {

		// load the sheet
		HSSFSheet sheet = wb.getSheet("Rivers");
		if (sheet == null) {
			throw new Exception("The River sheet was not found in MetaBook, terminate load process");
		}
				
		NCClass ncCls = NCClass.getNCClass("River");
		Row row = null;
		
		try {
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				if ((row = sheet.getRow(i)) != null && POIUtil.getCellValue(row,0) != null && POIUtil.getCellValue(row,0).length() > 0) {

					String mode = POIUtil.getCellValue(row,0);
					if (mode == null || mode.length() < 1) {
						String msg = "deploy mode must be specified for River entries ";
						logger.error(msg);
						throw new Exception(msg);
					}

					River river = new River(POIUtil.getCellValue(row,0), POIUtil.getCellValue(row,1),
							POIUtil.getCellValue(row,2));
					
					rivers.put(river.getName(), river);
				}
			}	

		} catch (Exception e) {
			logger.error("exception in GroupAssign load " + e.toString());
		}
	}


	public String getName() {return name;}
	public void setName(String name) {this.name = name;}
	public String getConfigFile() {return configFile;}
	public void setConfigFile(String configFile) {this.configFile = configFile;}
	public long getRunMinuteMax() {return runMinuteMax;}
	public void setRunMinuteMax(long runMinuteMax) {this.runMinuteMax = runMinuteMax;}

	private void addRunMinuteMax(String val) {
		runMinuteMax = new Long(runMinuteMax) * 1000;
	}
	
	public static HashMap<String, River> getRivers() {return rivers;}
	public static void setRivers(HashMap<String, River> rivers) {River.rivers = rivers;}

	
	}
