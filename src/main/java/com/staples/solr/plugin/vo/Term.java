package com.staples.solr.plugin.vo;

public class Term {
	private String phrase;
	private String[] words;
	
	public Term(String phrase) {
		this.phrase = phrase;
		this.words = phrase.split(" ");
	}
	
	public String getPhrase() {
		return phrase;
	}
	
	public String[] getWords() {
		return words;
	}
}
