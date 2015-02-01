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

package org.newcashel.meta.service;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.newcashel.meta.MetaManager;
import org.newcashel.meta.model.ElasticAlias;
import org.newcashel.meta.model.ElasticIndex;
import org.newcashel.meta.model.LaunchParms;
import org.newcashel.meta.model.NCClass;
import org.newcashel.meta.model.River;
import org.newcashel.meta.service.MetaSerializer;
import org.newcashel.meta.util.UTIL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticMetaManager {

	private static final Logger logger = LoggerFactory.getLogger(ElasticMetaManager.class);

	// populated in ElasticManager.initFieldValidation()
	private static HashMap<String, String[]> validStore = new HashMap<String, String[]>(); 
	
	//public static void buildAllIndexes(boolean rebuildAll) throws Exception {
	public static void buildAllIndexes(LaunchParms launchParm) throws Exception {
	
		final Client client = MetaManager.getClient();

		// loop thru all the indexes
		HashMap<String, ElasticIndex> indexes = ElasticIndex.getElasticIndexes();
		for (Map.Entry<String, ElasticIndex> entry : indexes.entrySet()) {
			
			ElasticIndex index = entry.getValue();

			// verify the index does not already exist (todo, pass as parm)
			final IndicesExistsResponse res = client.admin().indices().prepareExists(index.getName()).execute().actionGet();

			// if rebuild all specified, delete any index that exists
			if (launchParm.getRebuildIndex().equals("all")) {
				if (res.isExists()) {
					final DeleteIndexRequestBuilder delIdx = client.admin().indices().prepareDelete(index.getName());
					delIdx.execute().actionGet();
				}
			} 

			//client.admin().indices().
			
			MetaSerializer jsonSerializer = new MetaSerializer();

			if (!(res.isExists())) {

				if (index.getCommonAlias() == null) {
					logger.error("every index must specify a common alias " + index.getName());
					logger.info("index not built " + index.getName());
					continue;
				}

				// create new index
				int shards = index.getShardCount();
				int replicas = index.getReplicaCount();

				Settings indexSettings = null;
				if (index.getStoreType() != null && index.getStoreType().length() > 0) {
					indexSettings = ImmutableSettings.settingsBuilder()
							.put("number_of_shard", shards)
							.put("number_of_replicas", replicas)
							.put("index.store.type", index.getStoreType())
							.build();
				} else {
					indexSettings = ImmutableSettings.settingsBuilder()
							.put("number_of_shard", shards)
							.put("number_of_replicas", replicas)
							.build();
				}

				// create the index
				CreateIndexRequest indexRequest = new CreateIndexRequest(index.getName(), indexSettings);
				client.admin().indices().create(indexRequest).actionGet();

				// add the common alias to the index
				client.admin().indices().prepareAliases().addAlias(index.getName(),index.getCommonAlias().getName());

				// add the mapping for each class in the index
				for (NCClass ncCls : index.getNCClasses()) {

					String jsonStr = jsonSerializer.serializeNCClassMetaData(ncCls);
					
					// if schemaFolder set, output mapping for each type; file name is the type name
					if (launchParm.getSchemaFolder() != null) {
						String fileName = launchParm.getSchemaFolder() + File.separator + ncCls.getClassName() + ".json";
						UTIL.saveTextToFile(fileName, jsonStr);
					}
					
					PutMappingResponse response = client.admin().indices()
							.preparePutMapping(index.getName())
							.setType(ncCls.getClassName())
							.setSource(jsonStr)
							.execute()
							.actionGet();

					// set the index for this type (runtime specific, based on MetaBook mode and version
					ncCls.setAssignedIndexAlias(index.getCommonAlias());
				
				}

				// add other aliases for the index
				for (ElasticAlias alias : index.getRelatedAliases()) {
					client.admin().indices().prepareAliases().addAlias(index.getName(),alias.getName());
				}
				
			} else {
				// set the common alias for each class in the index
				// assume meta class info is freshly loaded, and the index alias needs to be set
				for (NCClass ncCls : index.getNCClasses()) {
					// set the index for this type (runtime specific, based on MetaBook mode and version
					ncCls.setAssignedIndexAlias(index.getCommonAlias());
				}
			}
		}
	} 

	public static void startRivers(LaunchParms launchParm) throws Exception {
		
		ArrayList<String> rivers = launchParm.getRivers();
		if (rivers == null) return;
		
		Client client = MetaManager.getClient();
		
		for (String riverName : rivers) {
			River river = River.getRiver(riverName);
			if (river == null) {
				String msg = "River name specified in LaunchParms is not defined on the River sheet " + riverName;
				logger.error(msg);
				throw new Exception(msg);
			}
			
			// open the river config file and extract the json source
			String jsonSource = UTIL.readAllTextFromFile(river.getConfigFile());
			
			// start the river 
			client.index(Requests.indexRequest("_river").type(river.getName()).id("_meta").source(jsonSource)).actionGet();
			
			String[] indexDeletions = {river.getName()};
			client.admin().indices().prepareDelete(indexDeletions).get();
			//client.admin().indices().prepareDelete(new String[] {}).get();
					
			//When river is done, you can just delete it from the _river index using this command:
			//curl -XDELETE localhost:9200/_river/you_river_name
			
		}
	}
	
	
	public static void runIntegrationTests(LaunchParms launchParm) throws Exception {
		ArrayList<String> integrationTests = launchParm.getIntegrationTests();
		if (integrationTests == null) return;
		
		Client client = MetaManager.getClient();
		
		for (String integrationTest : integrationTests) {

			Runner r = null;
			try {
				r = new BlockJUnit4ClassRunner(Class.forName(integrationTest));
			} catch (ClassNotFoundException | org.junit.runners.model.InitializationError e) {  
				logger.error("failure to initiate junit test " + e.toString());
				throw e;
			}
			JUnitCore c = new JUnitCore();
			c.run(Request.runner(r));
		}
	}
	
	
	// get the class assigned, common alias, then get the real index is points to
	public static String getCurrentIndex(NCClass ncCls) {
		String alias = ncCls.getAssignedIndexAlias().getName();
		return getCurrentIndex(alias);
	}

	// index's are versioned, but retain one and one only common alias
	public static String getCurrentIndex(String alias) {
		// iterate over the ElasticIndex entries for match on alias
		for (Map.Entry<String, ElasticIndex> entry : ElasticIndex.getElasticIndexes().entrySet()) {
			ElasticIndex index = entry.getValue();
			if (index.getCommonAlias() != null && index.getCommonAlias().getName().equals(alias)) return index.getName();
		}
		return null;
	}

	public static String getCurrentIndex(ElasticAlias alias) {
		return getCurrentIndex(alias.getName());
	}
	
	// call after indexes are built and queries are defined
	public static void assignAliasFilters() throws Exception {

		//ElasticQuery eq = new ElasticQuery();

		// loop thru the aliases and assign any filter for it
		// do this irregardless of whether the index(s) are rebuilt
		/*
		HashMap<String, ElasticAlias> aliases = ElasticAlias.getElasticAliases();
		for (Map.Entry<String, ElasticAlias> entry : aliases.entrySet()) {
			ElasticAlias alias = entry.getValue();
			String queryName = alias.getAliasFilter();
			if (queryName != null && queryName.length() > 0) {
				Query query = Query.getQuery(queryName);
				if (query == null) {
					logger.error("invalid query specified for alias filter " + queryName);
					return;
				}
				Query compiledQuery = eq.buildQuery(queryName,  null);
				alias.setAliasFilter(compiledQuery.getJson());
			}
		}
		*/
	}

	
	public static void buildElasticIndex(String indexName, String documentType, String mappingFile) throws IOException, InterruptedException {

		final Client client = MetaManager.getClient();

		NCClass ncCls = NCClass.getNCClass(documentType);
		if (ncCls == null) {
			logger.error("invalid NCClass name passed " + documentType);
		}

		try {
			byte[] encoded = Files.readAllBytes(Paths.get(mappingFile));
			String json = new String(encoded, StandardCharsets.UTF_8);

			final IndicesExistsResponse res = client.admin().indices().prepareExists(indexName).execute().actionGet();
			if (res.isExists()) {
				final DeleteIndexRequestBuilder delIdx = client.admin().indices().prepareDelete(indexName);
				delIdx.execute().actionGet();
			}

			Settings indexSettings = ImmutableSettings.settingsBuilder()
					.put("number_of_shard", 1)
					.put("number_of_replicas", 1)
					.build();

			CreateIndexRequest indexRequest = new CreateIndexRequest(indexName, indexSettings);
			client.admin().indices().create(indexRequest).actionGet();
			MetaSerializer jsonSerializer = new MetaSerializer();
			String jsonStr = jsonSerializer.serializeNCClassMetaData(ncCls);

			//final CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);
			//CreateIndexResponse createResponse = client.admin().indices().create(createIndexRequest(indexName)).actionGet();

			PutMappingResponse response = client.admin()
					.indices()
					.preparePutMapping(indexName)
					.setType(documentType)
					.setSource(jsonStr)
					.execute()
					.actionGet();
		} catch (Exception e) {
			logger.info("error defining mapping for index, type: " + indexName + ", " + documentType);
		}

	}

	// startRiver
	// create the river after all the indexing is built
	
	// stopRiver
	// client.admin().indices().prepareDeleteMapping("_river").setType("my_river").execute.actionGet();
	
	//public static void reIndex(String fromIndex, String toIndex, Query query, int SCROLL_SIZE, int TIMEOUT) {
	public static void reIndex(String fromIndex, String toIndex, String json, int SCROLL_SIZE, int TIMEOUT) {
	
		logger.info("Start creating a new index based on the old index.");

		final Client client = MetaManager.getClient();

		SearchResponse searchResponse = client.prepareSearch(fromIndex)
				//.setQuery(query.getJson())
				.setQuery(json)
				.setSearchType(SearchType.SCAN)
				.setScroll(new TimeValue(TIMEOUT))
				.setSize(SCROLL_SIZE).execute().actionGet();

		BulkProcessor bulkProcessor = BulkProcessor.builder(client, new BulkProcessor.Listener() {

			@Override
			public void beforeBulk(long executionId, BulkRequest request) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterBulk(long executionId, BulkRequest request,
					BulkResponse response) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterBulk(long executionId, BulkRequest request,
					Throwable failure) {
				// TODO Auto-generated method stub

			}})
			.setBulkActions(100)
			.setConcurrentRequests(1)
			.setFlushInterval(TimeValue.timeValueMinutes(10))
			.build();

		while (true) {
			searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())
					.setScroll(new TimeValue(TIMEOUT)).execute().actionGet();

			if (searchResponse.getHits().getHits().length == 0) {
				logger.info("Closing the bulk processor");
				bulkProcessor.close();
				break; //Break condition: No hits are returned
			}

	        for (SearchHit hit : searchResponse.getHits()) {
	            IndexRequest request = new IndexRequest(toIndex, hit.type(), hit.id());
	            request.source(hit.sourceRef());
	            bulkProcessor.add(request);
	        }
		}
	}


	public static HashMap<String, String[]> getValidStore() {return validStore;}
	public static void setValidStore(HashMap<String, String[]> validStore) {ElasticMetaManager.validStore = validStore;}
	
	
	public static void initFieldValidation() {
		String[] stringValid = {"store","index","doc_values","term_vector","boost","null_value","index_options","analyzer","index_analyzer","search_analyzer",
				"include_in_all","ignore_above","position_offset_gap"};
		validStore.put("string", stringValid);

		String[] numberValid = {"type","store","index","doc_values","precision_step","boost","null_value","include_in_all","ignore_malformed","coerce"};
		validStore.put("number", numberValid);
		
		String[] numberTypeValid = {"float","double","integer","long","short","byte"};
		validStore.put("number_type", numberTypeValid);
		
		String[] dateValid = {"format","store","index","doc_values","precision_step","boost","null_value","include_in_all","ignore_malformed"};
		validStore.put("date", dateValid);
		
		String[] booleanValid = {"store","index","boost","null_value"};
		validStore.put("boolean", booleanValid);
		
		//private static HashMap<String, Boolean> validStore = new HashMap<String, Boolean>(); 
		
	}

	public boolean isFieldQualifierValid(String fieldType, String fieldQualifier) throws Exception {
		
		String[] quals = validStore.get(fieldType);
		if (quals == null) {
			String msg = "invalid field type specified " + fieldType;
			logger.error(msg);
			throw new Exception(msg);
		}
		
		if (Arrays.asList(quals).contains(fieldQualifier)) return true;
		return false;
	}
}

