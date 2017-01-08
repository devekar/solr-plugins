package com.staples.solr.plugin.vo;

public class RedirectRule extends Rule{
	private String url;

	public RedirectRule(String phrase, MatchMode matchMode, String url) {
		super(phrase, matchMode);
		this.url = url;
	}
	
	public RedirectRule(String phrase, String matchMode, String url) {
		super(phrase, matchMode);
		this.url = url;
	}
	
	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RedirectRule [url=").append(url).append(", toString()=").append(super.toString()).append("]");
		return builder.toString();
	}
}
