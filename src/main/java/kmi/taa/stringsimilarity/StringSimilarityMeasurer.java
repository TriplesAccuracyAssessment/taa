package kmi.taa.stringsimilarity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

import databionics.text.SimilarString;
import kmi.taa.core.FileHelper;

public class StringSimilarityMeasurer extends NGramMeasurer{

	public void doComparison(String sourceTriples, String candidateTriples, String output) throws IOException {		
		StringBuilder builder = new StringBuilder();		
		List<String> list = Files.readAllLines(Paths.get(candidateTriples), StandardCharsets.UTF_8);
		System.out.println(System.currentTimeMillis() + ": comparing object string similarity...");
		
		SortedMap<Integer, Double> results = compareAll(sourceTriples, list);
		
		for(Integer id : results.keySet()) {
			String[] line = list.get(id-1).split("\t");
			for(int i=1; i < line.length; i++) {
				builder.append(line[i] + "\t");
			}
			builder.append(results.get(id));
			builder.append(System.lineSeparator());
		}		
		FileHelper.writeFile(builder.toString(), output, false);				
		System.out.println(System.currentTimeMillis() + ": completed.");
	}

	public HashMap<Integer, String> readSTripleObjects(String sourceTriples) throws IOException {
		HashMap<Integer, String> pairs = new HashMap<>();		
		List<String> lines = Files.readAllLines(Paths.get(sourceTriples), StandardCharsets.UTF_8);
		for(String line : lines) {
			String[] str = line.split("\t");
			pairs.put(Integer.valueOf(str[0]), str[3]);
		}				
		return pairs;
	}
	
	public SortedMap<Integer, Double> compareAll(String sTriplesFile, List<String> list) {
		SortedMap<Integer, Double> results = Collections.synchronizedSortedMap(new TreeMap<Integer, Double>());
		ExecutorService pool = Executors.newFixedThreadPool(200);
		
		int howManyLines = list.size();		
		HashMap<Integer, String> targetpairs = new HashMap<>();
		try {
			targetpairs = readSTripleObjects(sTriplesFile);
		} catch (IOException e1) {
			e1.getMessage();
		}
		
		for(String line : list) {
			String[] str = line.split("\t");
			String target = targetpairs.get(Integer.valueOf(str[1]));
			String candidate = str[6];
			String lineId = str[0];
			pool.execute(new ssMeasurer(lineId, target, candidate, results));

		} 
		pool.shutdown();
		
		int count = 0;
		int previousResultSize = 0;
		while(results.size() < howManyLines && count < 100) {
        	try {
        		Thread.sleep(1000);        		
        		count += 1;
				if (results.size() != previousResultSize) {
					previousResultSize = results.size();
					count = 0;
				}
        	}
        	catch(Exception e) {
        	
        	}
		}
        return results;
	}
	
	public static double compare(String source, String target) {
		double score = -1;		
		if(isURL(source)) {
			source = parseRDFSLabel(source);
		}	
		if(isURL(target)) {
			target = parseRDFSLabel(target);
		}
		if(source != null && target != null) {
			score = measure(source, target);					
		} else {
			
		}		
		return score;
	}
	
	public static boolean isURL(String str) {
		boolean isUrl = false;
		try {
			URL url = new URL(str);
			isUrl = true;
		} catch (MalformedURLException e) {

		}
		return isUrl;
	}
	
	/*
	 * parse the rdfs:label of an URL
	 */
	public static String parseRDFSLabel(String url) {
		String label = null;	
		
		Model model = ModelFactory.createDefaultModel();
		try {
		if (url.contains("freebase"))
			model.read(url, "TTL");
		else
			model.read(url);
		} catch (java.lang.Exception e) {
		}
		
		Resource subject = model.getResource(url);
		
		Property p1 = model.getProperty("http://www.wikidata.org/prop/direct/P373");
		String o1 = null;
		SimpleSelector sel1 = new SimpleSelector(subject, p1, o1);
		Model m1 = model.query(sel1);
		if(m1.size() > 0) {
			StmtIterator it = m1.listStatements();
			while(it.hasNext()) {
				Statement stmt1 = it.nextStatement();
				Resource subj1 = stmt1.getSubject();
				RDFNode obj1 = stmt1.getObject();							
				label = obj1.toString();	
			}
			
		} else {	
			Property p2 = model.getProperty("http://www.w3.org/2000/01/rdf-schema#label");		
			String o2 = null;
			SimpleSelector sel2 = new SimpleSelector(subject, p2, o2);
			Model m2 = model.query(sel2);
			
			StmtIterator it = m2.listStatements();
			while(it.hasNext()) {
				Statement stmt2 = it.nextStatement();
				Resource subj2 = stmt2.getSubject();
				RDFNode obj2 = stmt2.getObject();			
				if(obj2.toString().endsWith("@en")) {
					label = obj2.toString().substring(0, obj2.toString().length() - 3);
				}
			}
		}
		return label;
	}
}

class ssMeasurer implements Runnable {
	String id;
	String targ;
	String cand;
	SortedMap<Integer, Double> results;
	
	public ssMeasurer(String lineId, String target, String candidate, SortedMap<Integer, Double> results) {
		id = lineId;
		targ = target;
		cand = candidate;
		this.results = results;
	}

	@Override
	public void run() {
		this.results.put(Integer.valueOf(id), Double.valueOf(StringSimilarityMeasurer.compare(targ, cand)));
		
	}
	
}