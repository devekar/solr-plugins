package com.vaibhav.solr.plugin.rest;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.rest.BaseSolrResource;
import org.apache.solr.rest.ManagedResource;
import org.apache.solr.rest.ManagedResourceStorage.StorageIO;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaibhav.solr.plugin.vo.RedirectRule;

/**
 * ManagedResource implementation for redirect rules. 
 * 
 * @author Vaibhav Devekar
 *
 */
public class RedirectManager extends ManagedResource implements ManagedResource.ChildResourceSupport {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private static final String URL = "url";
	private static final String MATCH_MODE = "matchMode";

	private Map<String, RedirectRule> rules = new HashMap<String, RedirectRule>();
	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	ScheduledFuture<?> scheduledFuture = null;

	public RedirectManager(String resourceId, SolrResourceLoader loader, StorageIO storageIO)
			throws SolrException {
		super(resourceId, loader, storageIO);
		
		//TODO: Use zookeeper watches to trigger reloads
		scheduleReload(5);		
	}

	private void scheduleReload(int delay) {
		scheduledFuture = scheduler.scheduleWithFixedDelay(new Runnable() {
			public void run() {
				try {
					reloadFromStorage();					
				} catch (SolrException e) {
					log.error("Scheduled task failed. ", e);
				}				
			}
		}, delay, delay, TimeUnit.MINUTES);
		
		log.info("Scheduled reload for redirect mappings with delay of {} minutes", delay);
	}

	@Override
	protected void onManagedDataLoadedFromStorage(NamedList<?> managedInitArgs, Object managedData)
			throws SolrException {		
		if(managedData != null) {
			applyUpdatesToManagedData(managedData);
			log.info("Loaded {} redirect mappings for {}", rules.size(), getResourceId());
		}
	}

	@Override
	protected Object applyUpdatesToManagedData(Object updates) {
		boolean madeChanges = false;

		if (updates instanceof Map) {
			Map<String, Object> jsonMap = (Map<String, Object>) updates;

			for (String term : jsonMap.keySet()) {
				Object valObj = jsonMap.get(term);
				if(valObj instanceof Map) {
					Map<String,Object> val = (Map<String, Object>) valObj;
					Object urlObj = val.get(URL);
					Object matchModeObj = val.get(MATCH_MODE);
					if(urlObj instanceof String && matchModeObj instanceof String) {
						RedirectRule redirectRule =  new RedirectRule(term, (String)matchModeObj,  (String)urlObj);
						rules.put(term, redirectRule);
						madeChanges = true;
					}					
				}
			}
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unsupported data format (" + updates.getClass().getName() + "); expected a JSON object (Map)!");
		}

		return madeChanges ? getStoredView() : null;
	}

	@Override
	public synchronized void doDeleteChild(BaseSolrResource endpoint, String childId) {
		if(!rules.containsKey(childId)) {
			throw new SolrException(ErrorCode.NOT_FOUND,
					String.format(Locale.ROOT, "%s not found in %s", childId, getResourceId()));       
		} 

		rules.remove(childId);
		storeManagedData(getStoredView());

		log.info("Removed redirect mapping for: {}", childId);  
	}

	@Override
	public void doGet(BaseSolrResource endpoint, String childId) {
		SolrQueryResponse response = endpoint.getSolrResponse();
		if(childId != null) {
			if(!rules.containsKey(childId)) {
				throw new SolrException(ErrorCode.NOT_FOUND,
						String.format(Locale.ROOT, "%s not found in %s", childId, getResourceId()));       
			} 

			response.add(childId, getStoredView().get(childId));
		} else {
			response.add("redirectMappings", buildMapToStore(getStoredView()));
		}			
	}

	public Map<String, RedirectRule> getRules() {
		return rules;
	}

	private Map<String, Map<String, String>> getStoredView() {
		Map<String, Map<String, String>> storedView = new HashMap<String, Map<String, String>>();

		for(Entry<String, RedirectRule> rule: rules.entrySet()) {
			Map<String, String> value = new HashMap<String, String>();
			value.put(URL, rule.getValue().getUrl());
			value.put(MATCH_MODE, rule.getValue().getMatchMode().toString());
			storedView.put(rule.getKey(), value);
		}

		return storedView;
	}
	
}
