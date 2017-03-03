/*
 * (C) Copyright 2017 Shuangyan Liu
 * Shuangyan.Liu@open.ac.uk 
 * Knowledge Media Institute
 * The Open University, United Kingdom
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
