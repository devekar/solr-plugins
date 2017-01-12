package com.vaibhav.solr.plugin.vo;

public abstract class Rule {
	private Term term;
	private MatchMode matchMode;

	public Rule(String phrase, MatchMode matchMode) {
		this.term = new Term(phrase);
		this.matchMode = matchMode;
	}
	
	public Rule(String phrase, String matchMode) {
		this.term = new Term(phrase);
		this.matchMode = MatchMode.valueOf(matchMode);
	}
	
	public Term getTerm() {
		return term;
	}
	
	public MatchMode getMatchMode() {
		return matchMode;
	}

	/**
	 * Check if rule is applicable for given query
	 */
	public boolean applicableForQuery(String query) {
		if(query == null) {
			return false;
		}

		switch(matchMode) {
		case exact:
			return query.equals(term.getPhrase());
		case phrase:
			// TODO: Use StringSearch library for performance
			return query.matches(".*\\b" + term.getPhrase() + "\\b.*");
		case all:
			for(String word: term.getWords()) {
				if(!query.contains(word)) {
					return false;
				}
			}
			return true;
		}

		return false;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Rule [term=").append(term).append(", matchMode=").append(matchMode).append("]");
		return builder.toString();
	}
}
