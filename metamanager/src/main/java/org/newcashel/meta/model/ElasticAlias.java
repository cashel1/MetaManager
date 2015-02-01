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

public class ElasticAlias {

	private static final Logger logger = LoggerFactory.getLogger(ElasticAlias.class);

	private String name;
	private String descp;
	private String searchRoute;		// for aliases only
	private String indexRoute;		// for aliases only
	private String aliasFilter;		// json value returned from Filter Query

	private static HashMap<String, ElasticAlias> elasticAliases = new HashMap<String,ElasticAlias>();

	public ElasticAlias() {}
	
	public ElasticAlias(String name, String descp, String searchRoute, String indexRoute, String aliasFilter) {
		this.name = name.toLowerCase();
		this.descp = descp;
		this.searchRoute = searchRoute;
		this.indexRoute = indexRoute;
		this.aliasFilter = aliasFilter;
	}
	
	public static ElasticAlias getElasticAlias(String name) {return elasticAliases.get(name);}
	
	public static HashMap<String, ElasticAlias> getElasticAliases() {return elasticAliases;}
	
	public static void load(HSSFWorkbook wb, LaunchParms launchParm) throws Exception {

		// load the sheet
		HSSFSheet sheet = wb.getSheet("Aliases");
		if (sheet == null) {
			throw new Exception("The Aliases sheet was not found in BootBook, terminate load process");
		}
				
		NCClass ncCls = NCClass.getNCClass("ElasticAlias");
		Row row = null;
		
		try {
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				if ((row = sheet.getRow(i)) != null && POIUtil.getCellValue(row,0) != null && POIUtil.getCellValue(row,0).length() > 0) {

					String mode = POIUtil.getCellValue(row,0);
					if (mode == null || mode.length() < 1) {
						String msg = "deploy mode must be specified for Alias entries ";
						logger.error(msg);
						throw new Exception(msg);
					}

					String version = POIUtil.getCellValue(row,1);

					// if version is set but not the specified launch version, skip it
					if (version != null && version.length() > 0) {
						if (!(launchParm.getVersion().equals(version))) {
							continue;
						}
					}

					ElasticAlias alias = new ElasticAlias(POIUtil.getCellValue(row,2), POIUtil.getCellValue(row,3), 
							POIUtil.getCellValue(row,4), POIUtil.getCellValue(row,5), POIUtil.getCellValue(row,6));
					elasticAliases.put(alias.getName(), alias);
				}
			}	

		} catch (Exception e) {
			logger.error("exception in GroupAssign load " + e.toString());
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescp() {
		return descp;
	}

	public void setDescp(String descp) {
		this.descp = descp;
	}

	public String getSearchRoute() {
		return searchRoute;
	}

	public void setSearchRoute(String searchRoute) {
		this.searchRoute = searchRoute;
	}

	public String getIndexRoute() {
		return indexRoute;
	}

	public void setIndexRoute(String indexRoute) {
		this.indexRoute = indexRoute;
	}

	public String getAliasFilter() {
		return aliasFilter;
	}

	public void setAliasFilter(String aliasFilter) {
		this.aliasFilter = aliasFilter;
	}

	
}
