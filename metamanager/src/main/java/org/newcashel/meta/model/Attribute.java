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
import java.lang.reflect.Field;

public class Attribute implements Serializable {

	private static final long serialVersionUID = -3239391869607099478L;

	private String name = null;
	private String type = null;
	private String label = null;
	private String indexName = null;
	private boolean store = false;
	private String index = null;
	private boolean includeInAll = true;
	private int precision = 0;
	private String dateFormat = null;
	private String fieldDataFormat = null;
	private double boost = 0d;
	private String nullValue = null;
	private boolean docValues = false;
	private String termVector = null;
	private String analyzer = null;
	private String indexAnalyzer = null;
	private String searchAnalyzer = null;
	private int ignoreAbove = 0;
	private int positionOffset = 0;
	private boolean ignoreMalformed;
	private boolean coerceNumber;
	private boolean binaryCompress;
	private int compressThreshold;
	
	private Field field;
	
	public Attribute() {}

	public String getName() {return name;}
	public void setName(String name) {this.name = name;}
	public String getType() {return type;}
	public void setType(String type) {this.type = type;}
	public String getLabel() {return label;}
	public void setLabel(String label) {this.label = label;}
	public String getIndexName() {return indexName;}
	public void setIndexName(String indexName) {this.indexName = indexName;}
	public boolean isStore() {return store;}
	public void setStore(boolean store) {this.store = store;}
	public int getPrecision() {return precision;}
	public void setPrecision(int precision) {this.precision = precision;}
	public String getIndex() {return index;}
	public void setIndex(String index) {this.index = index;}
	public void setField(Field field) {this.field = field;}
	public String getDateFormat() {return dateFormat;}
	public void setDateFormat(String dateFormat) {this.dateFormat = dateFormat;}
	public double getBoost() {return boost;}
	public void setBoost(double boost) {this.boost = boost;}
	public String getNullValue() {return nullValue;}
	public void setNullValue(String nullValue) {this.nullValue = nullValue;}
	public boolean isIncludeInAll() {return includeInAll;}
	public void setIncludeInAll(boolean includeInAll) {this.includeInAll = includeInAll;}
	public boolean isDocValues() {return docValues;}
	public void setDocValues(boolean docValues) {this.docValues = docValues;}
	public String getTermVector() {return termVector;}
	public void setTermVector(String termVector) {this.termVector = termVector;}
	public String getAnalyzer() {return analyzer;}
	public void setAnalyzer(String analyzer) {this.analyzer = analyzer;}
	public String getIndexAnalyzer() {return indexAnalyzer;}
	public void setIndexAnalyzer(String indexAnalyzer) {this.indexAnalyzer = indexAnalyzer;}
	public String getSearchAnalyzer() {return searchAnalyzer;}
	public void setSearchAnalyzer(String searchAnalyzer) {this.searchAnalyzer = searchAnalyzer;}
	public int getPositionOffset() {return positionOffset;}
	public void setPositionOffset(int positionOffset) {this.positionOffset = positionOffset;}
	public boolean isIgnoreMalformed() {return ignoreMalformed;}
	public void setIgnoreMalformed(boolean ignoreMalformed) {this.ignoreMalformed = ignoreMalformed;}
	public boolean isCoerceNumber() {return coerceNumber;}
	public void setCoerceNumber(boolean coerceNumber) {this.coerceNumber = coerceNumber;}
	public boolean isBinaryCompress() {return binaryCompress;}
	public void setBinaryCompress(boolean binaryCompress) {this.binaryCompress = binaryCompress;}
	public int getCompressThreshold() {return compressThreshold;}
	public void setCompressThreshold(int compressThreshold) {this.compressThreshold = compressThreshold;}
	public String getFieldDataFormat() {return fieldDataFormat;}
	public void setFieldDataFormat(String fieldDataFormat) {this.fieldDataFormat = fieldDataFormat;}
	public int getIgnoreAbove() {return ignoreAbove;}
	public void setIgnoreAbove(int ignoreAbove) {this.ignoreAbove = ignoreAbove;}
	
}
