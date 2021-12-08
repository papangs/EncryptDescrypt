package com.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReqEncDec {
	
	@JsonProperty("input")
	private String input;

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}
	
	
	
}
