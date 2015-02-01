package org.newcashel.meta.model;

import java.io.Serializable;

// maintained as a HM within specific Query instances 
// used to pass parms between server and client (nested within the passed ActionParm) 
// one to one with Parameter class (atb), which provides overall meta processing info on the Parm 
public class Parm implements Serializable {
		
	private static final long serialVersionUID = 8741609964494931140L;
	private String atb;
	private String val;
	private String source;			// the Query SQO source
	private String parmType;		// values defined in Parameter class
	private boolean userSupplied;
	
	public Parm() {}
	/*
	public Parm(String atb, String val) {
		this.atb = atb;
		this.val = val;
		this.parmType = Parameter.VALUE_ASSIGNMENT;
	}
	*/
	
	public Parm(String atb, String val, String parmType, String source, boolean userSupplied) {
		this.atb = atb;
		this.val = val;
		this.parmType = parmType;
		this.source = source;
		this.userSupplied = userSupplied;
	}
	
	public String getAtb() {return atb;}
	public void setAtb(String atb) {this.atb = atb;}
	public String getVal() {return val;}
	public void setVal(String val) {this.val = val;}
	public boolean isUserSupplied() {return userSupplied;}
	public void setUserSupplied(boolean userSupplied) {this.userSupplied = userSupplied;}
	public String getParmType() {return parmType;}
	public void setParmType(String parmType) {this.parmType = parmType;}
	public String getSource() {return source;}
	public void setSource(String source) {this.source = source;}
	
}
