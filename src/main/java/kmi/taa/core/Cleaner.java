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
package kmi.taa.core;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.log4j.Logger;

import kmi.taa.runnable.Comparer;
import kmi.taa.runnable.Parser;


public class Cleaner {
	final static Logger log = Logger.getLogger(Cleaner.class.getName());
	private String dirOutput;
		
	public Cleaner(String dir) {
		this.dirOutput = dir;
	}
	
	/*
	 * Remove equivalent links in multi-languges
	 */
	public void rmMultipleDB(String origin, String rmMultiDB) throws IOException {
		BufferedReader br = null;
		String line = "";
		String csvSplit = "\t";
		StringBuilder builder = new StringBuilder();
		StringBuilder rmbuilder = new StringBuilder();
		
		try {
			br = new BufferedReader(new FileReader(origin));
			System.out.println(System.currentTimeMillis()+": checking duplicated dbpedia subject links...");
			while((line = br.readLine()) != null) {
				String[] str = line.split(csvSplit);
				// need to generalize the domain name
				if(!str[2].contains("dbpedia.org")) { 
					builder.append(line);
					builder.append(System.lineSeparator());
				}
				else {
					rmbuilder.append(line);
					rmbuilder.append(System.lineSeparator());
				}
			}
		} finally {
			if(br != null) {
				br.close();
			}
		}
		
		FileHelper.writeFile(builder.toString(), rmMultiDB, false);
		FileHelper.writeFile(rmbuilder.toString(), dirOutput+"duplicatedDBpedia_slinks.txt", false);
		System.out.println(System.currentTimeMillis()+": completed");
	}
	
	/*
	 * Remove duplicate subject links
	 */
	public void rmDuplicate(String origin, String rmDup) throws IOException, URISyntaxException {
		BufferedReader br = null;
		String line = "";
		String csvSplit = "\t";
		StringBuilder builder = new StringBuilder();
		StringBuilder rmbuilder = new StringBuilder();
		
		try {
			br = new BufferedReader(new FileReader(origin));
			System.out.println(System.currentTimeMillis()+": checking duplicated subject links ...");
			Integer resid = new Integer("1");
			HashSet<URI> set = new HashSet<>();
			
			while((line = br.readLine()) != null) {
				String[] str = line.split(csvSplit);						
				Integer id = new Integer(str[0]);
				URI uri = new URI(str[2]);
				
				if(id.equals(resid)) { 
					if(!set.contains(uri) && !isFNequal(set, uri)) {
						builder.append(line);
						builder.append(System.lineSeparator());	
						set.add(uri);
					}
					else {
						rmbuilder.append(line);
						rmbuilder.append(System.lineSeparator());
					}
				} else {
					resid = id;
					set.clear();					
					builder.append(line);
					builder.append(System.lineSeparator());	
					set.add(uri);
				}				
			}
		} finally {
			if(br != null) {
				br.close();
			}
		}
		
		FileHelper.writeFile(builder.toString(), rmDup, false);
		FileHelper.writeFile(rmbuilder.toString(), dirOutput+"duplicate_slinks.txt", false);
		System.out.println(System.currentTimeMillis()+": completed");

	}
	
	/*
	 * Test if URL is accessible within given TimeOut time
	 */
	public void validate(String origin, String validated) {
		BufferedReader br = null;
		String line = "";
		String csvSplit = "\t";
		StringBuilder builder = new StringBuilder();
		StringBuilder rmBuilder = new StringBuilder();
		
		try {
			br = new BufferedReader(new FileReader(origin));
			System.out.println(System.currentTimeMillis()+": validating subject links ...");
			Map<Integer, String> allUrls = new LinkedHashMap<Integer, String>();
			Map<Integer, String> originalLines = new HashMap<Integer, String>();
			
			while((line = br.readLine()) != null) {
				String[] str = line.split(csvSplit);				
				String url = str[2]; 
				allUrls.put(Integer.valueOf(str[str.length-1]), url);
				originalLines.put(Integer.valueOf(str[str.length-1]), line);
			}
			System.out.println(System.currentTimeMillis()+": starting multithreaded ping on all urls ...");
			SortedMap<Integer, Boolean> results = pingAll(allUrls);

			for (Integer lineId:results.keySet()) {				
				boolean isLive = results.get(lineId);
				String l = originalLines.get(lineId);				
					if(isLive) {
						builder.append(l);
						builder.append(System.lineSeparator());
					}
					else {
						rmBuilder.append(l);
						rmBuilder.append(System.lineSeparator());
					}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} finally {
			
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			
		}
		
		FileHelper.writeFile(builder.toString(), validated, false);
		FileHelper.writeFile(rmBuilder.toString(), dirOutput+"removed_invalid_slinks.txt", false);
		System.out.println(System.currentTimeMillis()+": completed");

	}
		
	public static boolean ping(String url, int timeout) {
	    // Otherwise an exception may be thrown on invalid SSL certificates
	    url = url.replaceFirst("^https", "http");

	    try {
	        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();	        
	        connection.setConnectTimeout(timeout);
	        connection.setReadTimeout(timeout);
	        connection.setRequestMethod("HEAD");	        
	        int responseCode = connection.getResponseCode();
	        return (200 <= responseCode && responseCode <= 399);
	    } catch (IOException exception) {
	        return false;
	    }
	}
	
	public static SortedMap<Integer, Boolean> pingAll(Map<Integer, String> urls) {
		SortedMap<Integer, Boolean> results = Collections.synchronizedSortedMap(new TreeMap<Integer, Boolean>());
		ExecutorService pool = Executors.newFixedThreadPool(100);
    	
    	int howManyUrl = urls.size();
    	
        for (Integer lineId:urls.keySet()) {
        	String url = urls.get(lineId);
            pool.execute(new Pinger(lineId, url, results));
        }
        pool.shutdown();
        
        while(results.keySet().size() < howManyUrl)
        	try {
        		Thread.sleep(1000);
        		System.out.println("Already pinged " + results.keySet().size() + " urls...");
        	}
        	catch(Exception e) {
        	
        	}
        
        System.out.println("All urls are pinged");
        return results;
	}
	
	
	public boolean isFNequal(HashSet<URI> set, URI target) {
		for(URI uri:set) {
			if (!uri.equals(target)) {
				if (uri.toString().equals(target.toString() + "/")
						|| target.toString().equals(uri.toString() + "/"))
					return true;
			}
		}
		return false;
	}
	
	/*
	 * remove subject links that are wrongly linked to the source subject link,
	 * which do not represent the same resource
	 */
	public void rmWrongSlinks(String ipfile, String opfile, String proxy, String outputDir) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(ipfile));		
		System.out.println(System.currentTimeMillis()+": remove wrong subject links ...");		
		String line;
		StringBuilder builder = new StringBuilder();
		StringBuilder rmBuilder = new StringBuilder();
		
		Map<Integer, String> originalLines = new LinkedHashMap<Integer, String>();
		Set<String> sourceSubjects = new HashSet<String>();
		Set<String> targetSubjects = new HashSet<String>();
		
		while((line = br.readLine()) != null) {
			String[] split = line.split("\t");
			if(!sourceSubjects.contains(split[1])) {
				sourceSubjects.add(split[1]);
			}
			if(!targetSubjects.contains(split[2])) {
				targetSubjects.add(split[2]);
			}
			originalLines.put(Integer.valueOf(split[split.length-1]), line);
		}
		br.close();
		
		Map<String, String> subjectNames = parseAllUrlNames(sourceSubjects, proxy, outputDir);	
		Map<String, String> targetNames = parseAllUrlNames(targetSubjects, proxy, outputDir);
		
		System.out.println(System.currentTimeMillis()+": starting multithreaded comparing on all pairs of subject links ...");
		SortedMap<Integer, Boolean> results = compareAll(subjectNames, targetNames, originalLines, proxy, outputDir);
		
		for (Integer lineId:results.keySet()) {				
			boolean isEqual = results.get(lineId);
			String l = originalLines.get(lineId);				
				if(isEqual) {
					builder.append(l);
					builder.append(System.lineSeparator());
				}
				else {
					rmBuilder.append(l);
					rmBuilder.append(System.lineSeparator());
				}
		}		
		FileHelper.writeFile(builder.toString(), opfile, false);
		FileHelper.writeFile(rmBuilder.toString(), dirOutput+"removed_inequal_slinks.txt", false);		
		System.out.println(System.currentTimeMillis()+": completed");
	}
	
	public SortedMap<String, String> parseAllUrlNames(Set<String> urls, String proxy, String outputDir) {
		SortedMap<String, String> results = Collections.synchronizedSortedMap(new TreeMap<String, String>());
		ExecutorService pool = Executors.newFixedThreadPool(100);
    	
    	int howManyUrl = urls.size();
    	
    	try {
	        for (String url:urls) {    
	        	pool.execute(new Parser(url, results, proxy));

	        }
    	} catch(Exception e) {
    		e.printStackTrace();
    		pool.shutdown();
    	}
    	
    	int count = 0;
    	int previousSize = 0;
    	StringBuilder builder = new StringBuilder();
        while(results.keySet().size() < howManyUrl) {
        	int currentSize = results.keySet().size();
        	if(currentSize == previousSize && count > 20) {
        		for(String url:urls) {
        			if(!results.containsKey(url)) {
        				builder.append(url);
        				builder.append(System.lineSeparator());
        			}
        		}
        		FileHelper.writeFile(builder.toString(), outputDir+"unparsed_slinks.txt", false);
        		break;
        	} else if(currentSize == previousSize && count <= 20) {
	        	count++;
        	} else if(currentSize != previousSize && count == 0) {
        		previousSize = results.keySet().size();
        	} else if(currentSize != previousSize && count > 0) {
        		count = 0;        		
        		previousSize = results.keySet().size();
        	}
        	
        	try {
        		Thread.sleep(1000);
        		System.out.println("Already parsed " + results.keySet().size() + " subject links...");
        	}
        	catch(Exception e) {
        	
        	}
        	
        }
        System.out.println("done");
        
		return results;
	}
	
	public static SortedMap<Integer, Boolean> compareAll(Map<String, String> sourceNames, Map<String, String> targetNames, Map<Integer, String> originalLines, String proxy, String outputDir) {
		SortedMap<Integer, Boolean> results = Collections.synchronizedSortedMap(new TreeMap<Integer, Boolean>());
		ExecutorService pool = Executors.newFixedThreadPool(100);
    	
    	int howManyUrl = originalLines.size();
    	
        for (Integer lineId:originalLines.keySet()) {
        	String line = originalLines.get(lineId);
        	String[] split = line.split("\t");
        	String sourceSubjectName = sourceNames.get(split[1]);
        	String targetSubjectName = targetNames.get(split[2]);
        	if(sourceSubjectName != null && targetSubjectName != null)
        		pool.execute(new Comparer(lineId, sourceSubjectName, targetSubjectName, line, results, proxy));
        	else
        		results.put(lineId, true);
        }
        pool.shutdown();
        
        int count = 0;
    	int previousSize = 0;
    	StringBuilder builder = new StringBuilder();
        while(results.keySet().size() < howManyUrl) {
        	int currentSize = results.keySet().size();
        	if(currentSize == previousSize && count > 20) {
        		System.out.println("current size of results: " + currentSize);
        		System.out.println("total uris: " + howManyUrl);
        		System.out.println("unprocessed slink pairs: " + (howManyUrl - currentSize));
        		for(Integer lineId:originalLines.keySet()) {
        			if(!results.containsKey(lineId)) {
        				builder.append(originalLines.get(lineId));
        				builder.append(System.lineSeparator());
        			}
        		}
        		FileHelper.writeFile(builder.toString(), outputDir+"uncompared_slinks.txt", false);
        		break;
        	} else if(currentSize == previousSize && count <= 20) {
	        	count++;
        	} else if(currentSize != previousSize && count == 0) {
        		previousSize = results.keySet().size();
        	} else if(currentSize != previousSize && count > 0) {
        		count = 0;        		
        		previousSize = results.keySet().size();
        	}
        	
        	try {
        		Thread.sleep(1000);
        		System.out.println("Already parsed " + results.keySet().size() + " subject links...");
        	}
        	catch(Exception e) {
        	
        	}
        	
        }
        
        System.out.println("Comparison of subject links is done.");
        
        return results;
	}	
	
	public static boolean equalSubjectNames(String sname, String tname, String targetUrl, String proxy) {		
		if(!tname.isEmpty() && !sname.isEmpty()) {
			if(!sname.equalsIgnoreCase(tname))
				return false;
		} else {
			SPARQLHTTPClient c = new SPARQLHTTPClient();
			try {
				String rdf = c.httpGet(targetUrl, proxy);
				if(!sname.isEmpty() && !rdf.contains(sname))
					return false;				
			} catch (IOException e) {
				log.error("get equalSubjectNames error: ", e);
				log.info("can not parse input url: " + targetUrl);
			}
		}
		
		return true;
	}
	
	/*
	 * parse RDF resources that use rdfs:label or other vocabularies to describe
	 * the name of the resources
	 */
	public static String parseResLabel(String url) {
		String label = "";			
		Model model = ModelFactory.createDefaultModel();		
		try {
		if (url.contains("freebase"))
			model.read(url, "TTL");
		else
			model.read(url);
		} catch (java.lang.Exception e) {
			log.error("parse Res Label error: " + url);
			return label;
		}
				
		Resource subject = model.getResource(url);		
		Property p1 = model.getProperty("http://www.w3.org/2000/01/rdf-schema#label");		
		String o1 = null;
		SimpleSelector sel1 = new SimpleSelector(subject, p1, o1);
		Model m1 = model.query(sel1);
		
		StmtIterator it = m1.listStatements();
		while(it.hasNext()) {
			Statement stmt = it.nextStatement();
			RDFNode obj = stmt.getObject();			
			if(obj.toString().endsWith("@en")) {
				label = obj.toString().substring(0, obj.toString().length() - 3);
			}
		}
		
		if(label == "") {
			Property wikicategory = model.getProperty("http://www.wikidata.org/prop/direct/P373");
			String obj = null;
			SimpleSelector sel = new SimpleSelector(subject, wikicategory, obj);
			Model m = model.query(sel);			
			StmtIterator wdit = m.listStatements();
			while(wdit.hasNext()) {
				Statement stmt = wdit.nextStatement();
				RDFNode n = stmt.getObject();							
				label = n.toString();					
			}

		}
		
		if(label == "") {
			Property gnppty = model.getProperty("http://www.geonames.org/ontology#name");
			String obj = null;
			SimpleSelector sel = new SimpleSelector(subject, gnppty, obj);
			Model m = model.query(sel);			
			StmtIterator gnit = m.listStatements();
			while(gnit.hasNext()) {
				Statement stmt = gnit.nextStatement();
				RDFNode n = stmt.getObject();							
				label = n.toString();	
			}
		}
		
		if(label == "") {
			StmtIterator complete = model.listStatements();
			while(complete.hasNext()) {
				Statement stmt = complete.nextStatement();
				Property ppty = stmt.getPredicate();				
				RDFNode obj = stmt.getObject();
				if(ppty.getLocalName().contains("Name") || ppty.getLocalName().contains("Label") || 
						ppty.getLocalName().contains("name") || ppty.getLocalName().contains("label")) {
					label = obj.toString().split("@")[0];
					break;
				}
			}
		}

		return label;
	}
	
	public synchronized void resultToFile(List<String> result, String fname) {
		File file = new File(fname);
		file.getParentFile().mkdirs();
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(fname, true));
			for(String str: result) {
				bw.write(str);
				bw.write(System.lineSeparator());
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	public void run(String proxy) throws IOException, URISyntaxException {
		SmallSetAnalyser ssa = new SmallSetAnalyser();
		ssa.addLineids(Paths.get(dirOutput+"subjectlinks.txt"), dirOutput+"subjectlinks.txt");
		validate(dirOutput+"subjectlinks.txt", dirOutput+"validated_slinks.txt"); 
		removeLineIds(Paths.get(dirOutput+"subjectlinks.txt"), dirOutput+"subjectlinks.txt");
		removeLineIds(Paths.get(dirOutput+"validated_slinks.txt"), dirOutput+"validated_slinks.txt");
		
		rmDuplicate(dirOutput+"validated_slinks.txt", dirOutput+"noduplicates_slinks.txt");
		rmMultipleDB(dirOutput+"noduplicates_slinks.txt", dirOutput+"nolangs_slinks.txt");
		
		ssa.addLineids(Paths.get(dirOutput+"nolangs_slinks.txt"), dirOutput+"nolangs_slinks.txt");
		rmWrongSlinks(dirOutput+"nolangs_slinks.txt", dirOutput+"cleaned_slinks.txt", proxy, dirOutput);
		removeLineIds(Paths.get(dirOutput+"nolangs_slinks.txt"), dirOutput+"nolangs_slinks.txt");
		removeLineIds(Paths.get(dirOutput+"cleaned_slinks.txt"), dirOutput+"cleaned_slinks.txt");		
		
	}
	
	public void removeLineIds(Path path, String ouFile) {
		StringBuilder builder = new StringBuilder();
		
		try {
			List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			for(String l:lines) {
				String[] str = l.split("\t");	
				int i=0;
				while (i < str.length-1) {
					if(i == str.length -2) {
						builder.append(str[i++]);
					} else {
						builder.append(str[i++]+"\t");
					}
				}
				builder.append(System.lineSeparator());
			}
		} catch (IOException e) {
			log.error("remove line ids error", e);
		}		
		FileHelper.writeFile(builder.toString(), ouFile, false);
	}

}

class Pinger implements Runnable {
	private Integer lineId;
    private String url;
	private SortedMap<Integer, Boolean> results;

    Pinger(Integer lineId, String url, SortedMap<Integer, Boolean> results) {
    	this.lineId = lineId;
        this.url = url;
        this.results = results;
    }

    public void run() {
    	this.results.put(this.lineId, Cleaner.ping(this.url, 20000));
    }
}
