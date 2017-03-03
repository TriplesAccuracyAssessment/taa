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
