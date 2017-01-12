package com.vaibhav.solr.plugin.component;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.rest.ManagedResource;
import org.apache.solr.rest.ManagedResourceObserver;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaibhav.solr.plugin.rest.RedirectManager;
import com.vaibhav.solr.plugin.vo.RedirectRule;

/**
 * SearchComponent to add redirectUrl to response based on query term.
 * Add as the first component in the pipeline to skip query processing when redirect is applicable.
 *  
 * @author Vaibhav Devekar
 *
 */
public class ManagedRedirectComponent extends SearchComponent implements SolrCoreAware, ManagedResourceObserver {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String DESCRIPTION = "An API managed component to handle redirect rules";
	private static final String SOURCE = "";
	private static final String VERSION = "1.0.0";
	private static final String RESOURCE_ID = "/schema/analysis/redirect";
	private static final String REDIRECT_FIELD = "redirectUrl";

	private RedirectManager redirectManager;

	public void inform(SolrCore core) {
		log.info("Initializing ManagedRedirectComponent");
		try {
			initManagedResource(core.getResourceLoader());
		} catch (IOException e) {
			throw new SolrException(ErrorCode.SERVER_ERROR, e);
		}
	}

	public void initManagedResource(ResourceLoader loader) throws IOException {
		SolrResourceLoader solrResourceLoader = (SolrResourceLoader)loader;

		// here we want to register that we need to be managed
		// at a specified path and the ManagedResource impl class
		// that should be used to manage this component
		solrResourceLoader.getManagedResourceRegistry().
		registerManagedResource(RESOURCE_ID, getManagedResourceImplClass(), this);
	}

	@Override
	public void prepare(ResponseBuilder rb) throws IOException {		
		String query = rb.req.getOriginalParams().get("q");
		if(query == null) {
			return;
		}
		
		// TODO: Delegate query sanitization to a util
		query = query.trim().toLowerCase();
		query = query.replaceAll("\\s+", " ");
		
		Map<String, RedirectRule> rules = redirectManager.getRules();

		for(RedirectRule rule: rules.values()) {
			if(rule.applicableForQuery(query)) {
				rb.rsp.add(REDIRECT_FIELD, rule.getUrl());
				break;
			}
		}
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {

	}

	/**
	 * As long as this component is called before QueryComponent, 
	 * this will skip query execution in distributed mode when rule has been applied 
	 */
	@Override
	public int distributedProcess(ResponseBuilder rb) throws IOException {		
		if(rb.rsp.getValues().get(REDIRECT_FIELD) != null) {			
			rb.stage = ResponseBuilder.STAGE_DONE;
		}
		
		return super.distributedProcess(rb);
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public String getSource() {
		return SOURCE;
	}

	@Override
	public String getVersion() {
		return VERSION;	
	}

	public void onManagedResourceInitialized(NamedList<?> args, ManagedResource res) throws SolrException {		
		redirectManager = (RedirectManager) res;
	}

	private Class<? extends ManagedResource> getManagedResourceImplClass() {
		return RedirectManager.class;
	}

}
