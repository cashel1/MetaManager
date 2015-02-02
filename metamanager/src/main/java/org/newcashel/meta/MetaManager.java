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

package org.newcashel.meta;

import java.io.File;
import java.io.FileInputStream;

import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.newcashel.meta.model.ElasticAlias;
import org.newcashel.meta.model.ElasticIndex;
import org.newcashel.meta.model.LaunchParms;
import org.newcashel.meta.model.NCClass;
import org.newcashel.meta.model.Parameter;
import org.newcashel.meta.model.River;
import org.newcashel.meta.service.ElasticMetaManager;
import org.newcashel.meta.util.UTIL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaManager {

	private static final Logger logger = LoggerFactory.getLogger(MetaManager.class);
	
	private static Node node = null;
	
	// thread safe, recommended use for application lifecycle
	private  static Client client = null;	
	
	// keep reference to allow load processors to save LaunchParms with ES records
	private static LaunchParms currentlaunchParm = null;
	
	public static void main(String[] args) {

		try {
			UTIL.setRuntimeDir(args[0].toString());
			
			// load logging property file
			String metaManagerLogFile = UTIL.getRuntimeDir() + File.separator + "MetaManagerLog4j.properties";
			try {
				PropertyConfigurator.configure(metaManagerLogFile);
				logger.info("metaManager log locaton established");
			} catch (Exception e) {
				logger.error("could not logging properties file " + metaManagerLogFile);
				throw e;
			}

			// load the properties file
			String propFileName = UTIL.getRuntimeDir() + File.separator	+ "MetaManager.properties";
			try {
				UTIL.loadProperties(propFileName);
				logger.info("loaded properties " + propFileName);
			} catch (Exception e) {
				logger.error("could not load properties file " + propFileName);
				throw e;
			}

			// converts specific properties to global variables that are used frequently
			//setSystemProperties();
			
			// instantiate the Elastic node and client
			try {
				
				String clusterName = UTIL.getPropertyVal("CLUSTER_NAME");
				boolean clientOnly = UTIL.convertBoolean(UTIL.getPropertyVal("CLIENT_ONLY"));
				
				node = NodeBuilder.nodeBuilder().clusterName(clusterName).client(clientOnly).node();
				client = node.client();
				
				if (!(loadMeta())) {
					client.close();
					//node.close();
				} else {
					logger.info("all meta data loaded ");		
				}
			} catch (Exception e) {
				logger.error("could not establish Elastic client node " + propFileName);
				throw e;
			}
		} catch (Exception e) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					if (client != null) client.close();
					if (node != null) node.close();
				}
			});
		}
	}

	public static Client getClient() {return client;}
	public static LaunchParms getLaunchParm() {return currentlaunchParm;}
	
	public static boolean loadMeta() {

		try {
			
			LaunchParms currentlaunchParm = loadMetaBook(UTIL.getRuntimeDir() + File.separator + "MetaBook.xls");
			
			// populates validation maps with allowable values for field types
			ElasticMetaManager.initFieldValidation();
			
			// build indexes
			//ElasticMetaManager.buildAllIndexes(currentlaunchParm);
			
			// start the rivers
			//ElasticMetaManager.startRivers(currentlaunchParm);
			
			// fire the integration tests
			ElasticMetaManager.runIntegrationTests(currentlaunchParm);
			
			//loadQueryBook(UTIL.getRuntimeDir() + File.separator + "QueryBook.xls",	currentLaunch);

			//loadBootBook(UTIL.getRuntimeDir() + File.separator + "BootBook.xls", currentLaunch);

			logger.info("MetaManager system startup complete");
			
			return true;

		} catch (Exception e) {
			logger.error("Startup exception in MetaManager.loadMeta(), terminating program " + e.toString());
			return false;
		}
	}
		
	private static LaunchParms loadMetaBook(String metaWorkbook) throws Exception {
		HSSFWorkbook wb = null; 
		FileInputStream fis = null;
		try {
		
			fis = new FileInputStream(metaWorkbook);
			wb = new HSSFWorkbook(fis); 

			// load River defs first to allow LaunchParms to validate user entries
			River.load(wb);
			
			// loads first launch entry marked 'is current' true
			LaunchParms currentLaunch = LaunchParms.load(wb);

			// load the class definitions (NCClass plus Super if any) is one to one with ES Type 
			NCClass.load(wb, currentLaunch);
			
			// load Aliases first, referenced by Indexes
			ElasticAlias.load(wb, currentLaunch);
			
			ElasticIndex.load(wb, currentLaunch);
			
			return currentLaunch;
			
		} catch (Exception e) {
			logger.error("Query file cannnot be loaded: " + metaWorkbook + ", " + e.toString());
			return null;

		} finally {
			if (fis != null) fis.close();
		}
	}
	
	
	/*
	private static void loadBootBook(String bootBook, LaunchParms launchParm) throws Exception {
		HSSFWorkbook wb = null; 
		FileInputStream fis = null;
		try {

			fis = new FileInputStream(bootBook);
			wb = new HSSFWorkbook(fis); 

			// //
			Network.load(wb);
			Org.load(wb);
			Group.load(wb);
			User.load(wb);
			GroupMember.load(wb);
			//ResourceConstraint.load(wb);
			
			Notification.load(wb);
			// //
			
		} catch (Exception e) {
			logger.error("BookBook file cannnot be loaded: " + bootBook + ", " + e.toString());
			return;
		}

	}
	*/

	
	/*
	private static void loadQueryBook(String queryWorkbook, LaunchParms launchParm) throws Exception {
		HSSFWorkbook wb = null; 
		FileInputStream fis = null;
		try {
			
			ElasticQuery eq = ElasticQuery.getElasticQuery();
			
			fis = new FileInputStream(queryWorkbook);
			wb = new HSSFWorkbook(fis); 

			ElasticAlias.load(wb, launchParm);
			ElasticIndex.load(wb, launchParm);
			ElasticMetaManager.buildAllIndexes(LaunchParms.getCurrentLaunch().isRebuildIndexes());
					
			// load up the QueryParm's "aggs"
			Parameter.load(wb);
			
			//ElasticAlias.load(wb, launchParm);
			//ElasticIndex.load(wb, launchParm);
			//ElasticMetaManager.buildAllMissingIndexes();
			
			// Load Queries
			eq.loadQueries(wb,"Queries");
			
			ElasticMetaManager.assignAliasFilters();
			
			// compile and call per queries, add percolator on subType = perq
			eq.loadPerqQueries();
			
			// load ActionLists,  TODO, review name validation issue
			//ActionList.load(wb);
			
			//Action.load(wb);
			
		} catch (Exception e) {
			logger.error("Query file cannnot be loaded: " + queryWorkbook + ", " + e.toString());
			return;
		}
	}
	*/
	
}
