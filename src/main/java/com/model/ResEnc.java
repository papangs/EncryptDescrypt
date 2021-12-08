package com.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResEnc {

	@JsonProperty("resEncObject")
	private ResEncObject resEncObject;

	@JsonProperty("resEncJson")
	private ResEncJson resEncJson;

	public ResEncObject getResEncObject() {
		return resEncObject;
	}

	public void setResEncObject(ResEncObject resEncObject) {
		this.resEncObject = resEncObject;
	}

	public ResEncJson getResEncJson() {
		return resEncJson;
	}

	public void setResEncJson(ResEncJson resEncJson) {
		this.resEncJson = resEncJson;
	}

	
}
