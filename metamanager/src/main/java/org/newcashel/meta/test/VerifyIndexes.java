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

package org.newcashel.meta.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.newcashel.meta.MetaManager;
import org.newcashel.meta.model.ElasticIndex;
import org.newcashel.meta.model.LaunchParms;

//import org.elasticsearch.test.ElasticsearchIntegrationTest;

//@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.SUITE)
//public class VerifyIndexes extends ElasticsearchIntegrationTest {
public class VerifyIndexes {
	
	private static LaunchParms launchParms;
	private static Client client;
	
	
	@Before
	public void setUp() throws Exception {
		launchParms = MetaManager.getLaunchParm();
		client = MetaManager.getClient();
	}
	
	//ref http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/integration-tests.html
	@Test
	public void verifyIndexCreation() throws Exception {
		
		assertNotNull(launchParms);
		assertNotNull(client);
		
		// verify all of the indexes exist
		HashMap<String, ElasticIndex> indexes = ElasticIndex.getElasticIndexes();
		for (ElasticIndex index : indexes.values()) {
			
			final IndicesExistsResponse res = client.admin().indices().prepareExists(index.getName()).execute().actionGet();
			assert(res.isExists());
			
			//client.admin().indices().aliasesExist(GetAliasesRequest(), arg1);
			
			//Map indices = (Map) result.getJsonMap().get("indices");
	        //assertEquals(1, indices.size());
	        //assertNotNull(indices.get("twitter"));
			
			
			//indexExists(index.getName());
			//ensureGreen(index.getName());
		}
		
	}
}

