package org.newcashel.meta.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;



import org.newcashel.meta.MetaManager;
//import org.newcashel.service.ESPersist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;


@JsonTypeInfo(use=Id.MINIMAL_CLASS, include=As.PROPERTY, property="org.newcashel.model.Resource")
@JsonSubTypes({
	@JsonSubTypes.Type(value=org.newcashel.meta.model.MetaProp.class, name="metaProps"),
	//@JsonSubTypes.Type(value=String.class, name="secGroups")
})

//@JsonTypeInfo(use=Id.MINIMAL_CLASS, include=As.PROPERTY, property="org.newcashel.model.Resource")

public class Resource implements Inst {

	private static final Logger logger = LoggerFactory.getLogger(Resource.class);
	
	// Inst props, elastic managed 
	private String _id;					
	private String _type;			 
	private long _version;			
	//private float score;			
	
	private String schemaVersion;		// launch config schema version used to create this resource
	private String externalId;			// user supplied, must be unique
	private String parentId;			// parent resource, hierarchical
	private String networkId;			// jury is out on this
	private String langCode;
	private String confidCode;
	private String encryptKey;
	private String name;
	
	private Date indexDate;
	private Date effectiveFrom;
	private Date effectiveThru;
	
	public ArrayList<MetaProp> metaProps = null;
	
	public Resource() {
		// default effectiveFrom to indexDate; setter will override this default if provided
		setIndexDate(new Date());
		setEffectiveFrom(getIndexDate());
		setSchemaVersion(MetaManager.getLaunchParm().getVersion());
	}
	
	@JsonIgnore
	public boolean addMetaProp(MetaProp metaProp) {
		if (metaProps == null) {
			metaProps = new ArrayList<MetaProp>();
		}
		metaProps.add(metaProp);
		return true;
		
		/*  this was taking meta and 'pushing' up to a flatten'ed inst
		// if the meta value is mapped to an Act prop, set the Act value
		//if (metaProp.get)
		MetaPropMap mpm = MetaPropMap.getMapVal(metaProp.getName());
		System.out.println("Checking on metaProp " + metaProp.getName());
		if (mpm.getActProp() != null) {
			String propName = mpm.getActProp();
			try {
				System.out.println("Checking on metaProp field " + mpm.getActProp()); field == null
				Field field = Act.class.getField(mpm.getActProp());
				field.set(this, metaProp.getValue().toString());
				
				
				
			} catch (Exception e) {
				System.out.println("NO SUCH FIELD " + mpm.getActProp());
			}
		}
		*/
	}
	public ArrayList<MetaProp> getMetaProps() { return metaProps;}
	
	@JsonSetter
	public void setMetaProps(ArrayList<MetaProp> metaProps) {
		this.metaProps = metaProps;
	}
	
	@JsonIgnore
	public String getFileId() {
		if (getType() == null || getId() == null) {
			logger.error("attempt to create a unique act id before type and id defined. returning a timestamp");
			return (new Long(new Date().getTime())).toString();
		}
		return getType() + "_" + getId();
	}
	
	@JsonIgnore
	public static String getIdFromFileId(String fileId) throws Exception {
		if (fileId == null) {
			throw new Exception("null fileId passed to getIdFromFileId()");
		}
		int offset = fileId.indexOf("_");
		if (offset < 1) {
			throw new Exception("invalid fileId passed to getIdFromFileId() " + fileId);
		}
		return fileId.substring(offset + 1, fileId.length());
	}
	
	@JsonIgnore
	public static String getTypeFromFileId(String fileId) throws Exception {
		if (fileId == null) {
			throw new Exception("null fileId passed to getTypeFromFileId()");
		}
		int offset = fileId.indexOf("_");
		if (offset < 1) {
			throw new Exception("invalid fileId passed to getTypeFromFileId() " + fileId);
		}
		return fileId.substring(0,offset);
		
	}
	
	
	public String getId() {return _id;}
	public void setId(String id) {this._id = id;}
	public String getType() {return _type;}
	public void setType(String type) {this._type = type;}
	public long getVersion() {return _version;}
	public void setVersion(long version) {this._version = version;}
	//public float getScore() { return score;}
	//public void setScore(float score) {this.score = score;}

	public String getSchemaVersion() {return schemaVersion;}
	public void setSchemaVersion(String schemaVersion) {this.schemaVersion = schemaVersion;}
	public Date getIndexDate() {return indexDate;}
	public void setIndexDate(Date indexDate) {this.indexDate = indexDate;}
	
	public String getParentId() {return parentId;}
	public void setParentId(String parentId) {this.parentId = parentId;}
	public String getExternalId() {return externalId;}
	public void setExternalId(String externalId) {this.externalId = externalId;}
	
	public String getName() {return name;}
	public void setName(String name) {this.name = name;}
	
	public Date getEffectiveFrom() {return effectiveFrom;}
	public void setEffectiveFrom(Date effectiveFrom) {this.effectiveFrom = effectiveFrom;}
	public Date getEffectiveThru() {return effectiveThru;}
	public void setEffectiveThru(Date effectiveThru) {this.effectiveThru = effectiveThru;}

	public String getNetworkId() {return networkId;}
	public void setNetworkId(String networkId) {this.networkId = networkId;}

	public String getLangCode() {return langCode;}
	public void setLangCode(String langCode) {this.langCode = langCode;}
	public String getConfidCode() {return confidCode;}
	public void setConfidCode(String configCode) {this.confidCode = confidCode;}
	public String getEncryptKey() {return encryptKey;}
	public void setEncryptKey(String encryptKey) {this.encryptKey = encryptKey;}

	
			
}
