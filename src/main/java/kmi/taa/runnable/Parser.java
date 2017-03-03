package kmi.taa.runnable;

import java.util.SortedMap;

import kmi.taa.core.Cleaner;

public class Parser implements Runnable {	
    private String url;
	private SortedMap<String, String> results;
	private String proxy;

    public Parser(String url, SortedMap<String, String> results, String proxy) {
    	this.url = url;     
        this.results = results;
        this.proxy = proxy;
    }

	@Override
	public void run() {
		results.put(url, Cleaner.parseResLabel(url));	
	}

}
