package org.newcashel.meta.model;

import org.elasticsearch.common.geo.GeoPoint;

public class MetaProp {

	private String type;		// meta category, assigned from system list of valid types
	private String name;
	private String value;		
	private String location;	// a geoHash value	
	
	public MetaProp() {}

	// new MetaProp(
	public MetaProp(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public MetaProp(String type, String name, String value) {
		this.setType(type);
		this.name = name;
		this.value = value;
	}
	
	public MetaProp(String type, String name, String value, String location) {
		this.setType(type);
		this.name = name;
		this.value = value;
		this.location = location;
	}
	
	public String getType() {return type;}
	public void setType(String type) {this.type = type;}
	public String getName() {return name;}
	public void setName(String name) {this.name = name;}
	public String getValue() {return value;}
	public void setValue(String value) {this.value = value;}
	public String getLocation() {return location;}
	public void setLocation(String location) {this.location = location;}

}
