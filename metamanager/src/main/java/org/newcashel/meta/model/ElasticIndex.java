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
import java.util.HashMap;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.newcashel.meta.util.POIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// instances are only available via the local HashMap; not stored in Elastic
public class ElasticIndex {
	private static final Logger logger = LoggerFactory.getLogger(ElasticIndex.class);

	private String name;
	private String descp;
	private int shardCount;				// for indexes only
	private int replicaCount;			// for indexes only
	private String storeType;
	private String postingFormat;
	private ElasticAlias commonAlias;	
	
	
	private ArrayList<ElasticAlias> relatedAliases = new ArrayList<ElasticAlias>();
	private ArrayList<NCClass> ncClasses = new ArrayList<NCClass>();
	
	
	private static HashMap<String, ElasticIndex> elasticIndexes = new HashMap<String,ElasticIndex>();

	public ElasticIndex() {}
	public ElasticIndex(String name, String descp, String shardCount, String replicaCount, String storeType, 
			String postingFormat, String commonAlias, String relatedAliasesStr, String ncClassesStr) {
		setName(name);
		this.descp = descp;
		setShardCount(shardCount);
		setReplicaCount(replicaCount);
		setStoreType(storeType);
		setCommonAlias(commonAlias);
		setAliases(relatedAliasesStr);
		setNCClasses(ncClassesStr);
		setPostingFormat(postingFormat);
	}
	
	public static ElasticIndex getElasticIndex(String name) {return elasticIndexes.get(name);}
	
	public static HashMap<String, ElasticIndex> getElasticIndexes() {return elasticIndexes;}
	
	
	public static void load(HSSFWorkbook wb, LaunchParms launchParm) throws Exception {

		// load the sheet
		HSSFSheet sheet = wb.getSheet("Indexes");
		if (sheet == null) {
			throw new Exception("The Indexes sheet was not found in BootBook, terminate load process");
		}
				
		NCClass ncCls = NCClass.getNCClass("ElasticIndex");
		Row row = null;
		
		
		try {
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				if ((row = sheet.getRow(i)) != null && POIUtil.getCellValue(row,0) != null && POIUtil.getCellValue(row,0).length() > 0) {

					String mode = POIUtil.getCellValue(row,0);
					if (mode == null || mode.length() < 1) {
						String msg = "deploy mode must be specified for Index entries ";
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
					
					ElasticIndex index = new ElasticIndex(POIUtil.getCellValue(row,2), POIUtil.getCellValue(row,3), 
							POIUtil.getCellValue(row,4), POIUtil.getCellValue(row,5), POIUtil.getCellValue(row,6), 
							POIUtil.getCellValue(row,7),POIUtil.getCellValue(row,8),POIUtil.getCellValue(row,9),
							POIUtil.getCellValue(row,10));
					elasticIndexes.put(index.getName(), index);
				}
			}	
			
		} catch (Exception e) {
			logger.error("exception in GroupAssign load " + e.toString());
		}
	}

	public void setShardCount(String shards) {
		try {
			Integer s = POIUtil.convertAnyNumberToInteger(shards);
			setShardCount(s);
		} catch (Exception e) {
			logger.error("illegal shard integer value passed to ElasticIndex: " + name + ", " + shards);
		}
	}
	
	public void setReplicaCount(String replicas) {
		try {
			Integer s = POIUtil.convertAnyNumberToInteger(replicas);
			setReplicaCount(s);
		} catch (Exception e) {
			logger.error("illegal replica integer value passed to ElasticIndex: " + name + ", " + replicas);
		}
	}

	public void setAliases(String relatedIndexStr) {
		if (relatedIndexStr == null || relatedIndexStr.length() < 1) return;
		String[] aliases = relatedIndexStr.split("\\s*,\\s*");
		for (String alias : aliases) {
			ElasticAlias esAlias = ElasticAlias.getElasticAlias(alias.toLowerCase());
			if (esAlias == null) {
				logger.error("invalid related alias " + alias);
			} else {
				relatedAliases.add(esAlias);
			}
		}
	}

	public void setCommonAlias(String commonAliasStr) {
		if (commonAliasStr == null || commonAliasStr.length() < 1) return;
		ElasticAlias esAlias = ElasticAlias.getElasticAlias(commonAliasStr.toLowerCase());
		if (esAlias == null) {
			logger.error("invalid related common alias " + commonAliasStr);
		} else {
			setCommonAlias(esAlias);
		}
	}

	public void setNCClasses(String ncClassStr) {
		if (ncClassStr == null || ncClassStr.length() < 1) return;
		String[] classes = ncClassStr.split("\\s*,\\s*");
		for (String cls : classes) {
			NCClass ncCls = NCClass.getNCClass(cls);
			if (ncCls == null) {
				logger.error("invalid NCClass " + cls);
			} else {
				ncClasses.add(ncCls);
			}
		}
	}

	public String getName() {return name;}
	public void setName(String name) {this.name = name.toLowerCase();}
	public String getDescp() {return descp;}
	public void setDescp(String descp) {this.descp = descp;}
	public int getShardCount() {return shardCount;}
	public void setShardCount(int shardCount) {this.shardCount = shardCount;}
	public int getReplicaCount() {return replicaCount;}
	public void setReplicaCount(int replicaCount) {this.replicaCount = replicaCount;}
	public ArrayList<NCClass> getNCClasses() { return ncClasses;}
	public ArrayList<ElasticAlias> getRelatedAliases() { return relatedAliases;}
	public ElasticAlias getCommonAlias() {return commonAlias;}
	public void setCommonAlias(ElasticAlias commonAlias) {this.commonAlias = commonAlias;}
	public String getStoreType() {return storeType;}
	public void setStoreType(String storeType) {this.storeType = storeType;}
	public String getPostingFormat() {
		return postingFormat;
	}
	public void setPostingFormat(String postingFormat) {
		this.postingFormat = postingFormat;
	}
	
}
