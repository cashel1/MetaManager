package org.newcashel.meta.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.newcashel.meta.model.Attribute;
import org.newcashel.meta.model.NCClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;

public class MetaSerializer {

	private static Logger logger = LoggerFactory.getLogger(MetaSerializer.class);
	static JsonFactory f = new JsonFactory();

	public MetaSerializer() {}

	public JsonGenerator getJsonGenerator(ByteArrayOutputStream stringStream) {
		try {
			JsonGenerator jgen = f.createGenerator(stringStream, JsonEncoding.UTF8);
			return jgen;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	// serializes a JSON return string consistent with ElasticSearch requirements
	public String serializeNCClassMetaData(NCClass ncCls) throws Exception {
		ByteArrayOutputStream stringStream = new ByteArrayOutputStream();
		JsonGenerator jgen = getJsonGenerator(stringStream);
		startObject(jgen);
		writeFieldName("properties", jgen);
		startObject(jgen);	

		// process the superClass, if any & process any nested types defined for this class
		NCClass superCls = ncCls.getSuperClass();
		if (superCls != null) {
			writeClassAtbs(superCls, jgen);
			writeSubTypeAtbs(superCls, jgen);
		}

		// process the class
		writeClassAtbs(ncCls, jgen);
		writeSubTypeAtbs(ncCls, jgen);

		endObject(jgen);
		endObject(jgen);	
		jgen.flush();
		return stringStream.toString();

	}

	private void writeClassAtbs(NCClass ncCls, JsonGenerator jgen) throws Exception {
		for (Map.Entry<String, Attribute> entry : ncCls.getAttributes().entrySet()) {
			Attribute atb = entry.getValue();
			if (atb.getType().equalsIgnoreCase("subType")) {
				continue;
			}
			writeFieldName(atb.getName(), jgen);
			startObject(jgen);
			writeFieldName("type", atb.getType().toLowerCase(), jgen);

			// default entry is default
			if (atb.getIndexName() != null) {
				writeFieldName("index_name", atb.getIndexName(), jgen);
			}

			// defaults to no
			if (atb.isStore()) {
				writeFieldName("store", "yes", jgen);
			} 

			// defaults to analyzed if not set
			if (atb.getIndex() != null) {
				writeFieldName("index", atb.getIndex(), jgen);
			}

			// defaults to true
			if (!(atb.isIncludeInAll())) {
				writeFieldName("include_in_all", "no", jgen);
			}

			// defaults to 4
			if (atb.getPrecision() > 0) {
				writeFieldName("precision_step", new Integer(atb.getPrecision()).toString(), jgen);
			} 

			if (atb.getDateFormat() != null) {
				writeFieldName("format", atb.getDateFormat(), jgen);
			} 

			if (atb.getFieldDataFormat() != null) {
				writeFieldName("fielddata", atb.getFieldDataFormat(), jgen);
			} 

			if (atb.isDocValues()) {
				writeFieldName("doc_values", "true", jgen);
			}

			// defaults to 1.0
			if (atb.getBoost() > 0) {
				writeFieldName("boost", new Double(atb.getBoost()).toString(), jgen);
			} 

			// defaults to not adding the field
			if (atb.getNullValue() != null) {
				writeFieldName("null_value", atb.getNullValue(), jgen);
			} 

			if (atb.getTermVector() != null) {
				writeFieldName("term_vector", atb.getTermVector(), jgen);
			}

			if (atb.getAnalyzer() != null) {
				writeFieldName("analyzer", atb.getAnalyzer(), jgen);
			}

			if (atb.getIndexAnalyzer() != null) {
				writeFieldName("index_analyzer", atb.getIndexAnalyzer(), jgen);
			}

			if (atb.getSearchAnalyzer() != null) {
				writeFieldName("search_analyzer", atb.getSearchAnalyzer(), jgen);
			}

			if (atb.getIgnoreAbove() > 0) {
				writeFieldName("ignore_above", new Integer(atb.getIgnoreAbove()).toString(), jgen);
			} 

			if (atb.getPositionOffset() > 0) {
				writeFieldName("position_offset_gap", new Integer(atb.getPositionOffset()).toString(), jgen);
			} 

			if (atb.isIgnoreMalformed()) {
				writeFieldName("ignore_malformed", "true", jgen);
			}

			if (atb.isCoerceNumber()) {
				writeFieldName("coerce", "true", jgen);
			}

			if (atb.isBinaryCompress()) {
				writeFieldName("compress", "true", jgen);
			}

			if (atb.getCompressThreshold() > 0) {
				writeFieldName("compress_threshold", new Integer(atb.getCompressThreshold()).toString(), jgen);
			} 

			// defaults to true
			if (!(atb.isIncludeInAll())) {
				writeFieldName("include_in_all", "no", jgen);
			} 

			endObject(jgen);
		}
	}

	private void writeSubTypeAtbs(NCClass ncCls, JsonGenerator jgen) throws Exception {
		HashMap<String, String> subTypes = ncCls.getSubTypes();
		if (subTypes == null) return;
		for (Map.Entry<String, String> entry : subTypes.entrySet()) {
			String collectionName = entry.getKey();
			String className = entry.getValue();
			writeFieldName(collectionName, jgen);
			startObject(jgen);
			writeFieldName("properties", jgen);
			startObject(jgen);	
			NCClass subCls = NCClass.getNCClass(className);
			if (subCls == null) {
				throw new Exception("nestedCls not valid: nestedClass, owningClass" + className + ", " + ncCls.getClassName());
			}
			// TODO, handle subs within subs
			writeClassAtbs(subCls, jgen);
			endObject(jgen);
			endObject(jgen);
		}		

	}

	public void startObject(JsonGenerator jgen) throws Exception {
		try {
			jgen.writeStartObject();
		} catch (Exception e) {
			jgen.flush();
			throw e;
		}
	}

	public void endObject(JsonGenerator jgen) throws Exception {
		try {
			jgen.writeEndObject();
		} catch (Exception e) {
			jgen.flush();
			throw e;
		}
	}

	public void writeFieldName(String term, String val, JsonGenerator jgen) throws Exception {
		try {
			jgen.writeStringField(term, val);
		} catch (Exception e) {
			jgen.flush();
			throw e;
		}
	}

	public void writeFieldName(String fieldName, JsonGenerator jgen) throws Exception {
		try {
			jgen.writeFieldName(fieldName);
		} catch (Exception e) {
			jgen.flush();
			throw e;
		}
	}
}
