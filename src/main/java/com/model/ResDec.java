package com.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResDec {

	@JsonProperty("resDecObject")
	private ResDecObject resDecObject;

	@JsonProperty("resDecJson")
	private ResDecJson resDecJson;

	public ResDecObject getResDecObject() {
		return resDecObject;
	}

	public void setResDecObject(ResDecObject resDecObject) {
		this.resDecObject = resDecObject;
	}

	public ResDecJson getResDecJson() {
		return resDecJson;
	}

	public void setResDecJson(ResDecJson resDecJson) {
		this.resDecJson = resDecJson;
	}

}
