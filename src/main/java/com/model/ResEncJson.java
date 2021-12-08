package com.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResEncJson {
	
	@JsonProperty("input")
	private String input;
	
	@JsonProperty("encrypted")
	private String encrypted;

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getEncrypted() {
		return encrypted;
	}

	public void setEncrypted(String encrypted) {
		this.encrypted = encrypted;
	}
	
	
}
