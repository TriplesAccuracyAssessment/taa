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

public class Comparer implements Runnable {

	private Integer lineId;
	private String sname;
	private String tname;
    private String line;
	private SortedMap<Integer, Boolean> results;
	private String proxy;

    public Comparer(Integer lineId, String sname, String tname, String line, SortedMap<Integer, Boolean> results, String proxy) {
    	this.lineId = lineId;
    	this.sname = sname;
    	this.tname =tname;
        this.line = line;
        this.results = results;
        this.proxy = proxy;
    }

    public void run() {
    	String[] split = line.split("\t");
    	results.put(lineId, Cleaner.equalSubjectNames(sname, tname, split[2], proxy));

    }

}
