package kmi.taa.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.ClientProtocolException;

import com.google.gson.Gson;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import kmi.taa.wikidata.Response;
import kmi.taa.wikidata.ResponseResults.ResponseBindings;

public class PredicateObjectRetriever {

	private HashMap<List, String> mdmapmultikey = new HashMap<List, String>(); // multiple keys
	private HashMap<String, String> mdmapsglkey = new HashMap<String, String>(); // single key

	public PredicateObjectRetriever() {
		ArrayList<String> l1 = new ArrayList<String>();
		l1.add("www.wikidata.org");
		l1.add("wikidata.org");
		mdmapmultikey.put(l1, "wikidata");
		ArrayList<String> l2 = new ArrayList<String>();
		l2.add("crime.psi.enakting.org");		
		l2.add("mortality.psi.enakting.org");
		l2.add("parliament.psi.enakting.org");
		l2.add("nuts.psi.enakting.org");
		mdmapmultikey.put(l2, "enakting");	
		
		mdmapsglkey.put("sws.geonames.org", "geonames");
		mdmapsglkey.put("gadm.geovocab.org", "geovocab");
		mdmapsglkey.put("linkedgeodata.org", "lgd");
		mdmapsglkey.put("www.bbc.co.uk", "bbc");
		mdmapsglkey.put("yago-knowledge.org", "yago");
		mdmapsglkey.put("dbpedialite.org", "dblite");
		mdmapsglkey.put("rdf.freebase.com", "freebase");
		mdmapsglkey.put("sw.opencyc.org", "opencyc");
		mdmapsglkey.put("data.ordnancesurvey.co.uk", "os");
		mdmapsglkey.put("data.linkedmdb.org", "lmdb");	
		mdmapsglkey.put("musicbrainz.org", "mbrain");				

	}

	public void execute(String input, String output, String proxy) throws IOException {
		BufferedReader br = null;
		String line = "";
		StringBuilder builder = new StringBuilder();
		
		try {
			br = new BufferedReader(new FileReader(input));
			System.out.println(System.currentTimeMillis()+": retrieving predicate links and objects ...");
			Map<Integer, String> originalLines = new LinkedHashMap<>();
			
			int lineId = 1;
			while((line = br.readLine()) != null) {
				originalLines.put(lineId++, line);
			}
			
			System.out.println(System.currentTimeMillis()+": starting multithreaded retrieving predicates and objects on all slinks ...");			
			SortedMap<Integer, String> results = retrieveAll(originalLines, proxy);
			
			for(Integer id : results.keySet()) {
				String result = results.get(id);
				if(!result.equals("")){ 
					String[] pairs = result.split(System.getProperty("line.separator"));
					for(String po:pairs) {
	 					builder.append(originalLines.get(id)+"\t"+po);
						builder.append(System.lineSeparator());
					}
				} else {
					builder.append(originalLines.get(id));
					builder.append(System.lineSeparator());
				}
				
			}
		} finally {
			if(br != null) {
				br.close();
			}
		}
		
		FileHelper.writeFile(builder.toString(), output, false);
		System.out.println(System.currentTimeMillis()+": po retrieving completed");
	}
	
	
	public SortedMap<Integer, String> retrieveAll(Map<Integer, String> originalLines, String proxy) {
		SortedMap<Integer, String> results = Collections.synchronizedSortedMap(new TreeMap<Integer, String>());
		ExecutorService pool = Executors.newFixedThreadPool(50);
		
		int howManyslinks = originalLines.size();
		
		for(Integer id : originalLines.keySet()) {
			String line = originalLines.get(id);
			String[] str = line.split("\t");
			String candidateUrl = str[2];
			pool.execute(new Retriever(id, candidateUrl, proxy, results));
		}
		pool.shutdown();
		
		int count = 0;
		int previousResultSize = 0;
		while(results.size() < howManyslinks && count < 100) {
			try {
				Thread.sleep(1000);
				count += 1;
				if (results.size() != previousResultSize) {
					previousResultSize = results.size();
					count = 0;
				}
				System.out.println("Already retrieved " + results.size() + " triples ...");
			} catch (InterruptedException e) {

			}
		}
		
		System.out.println("All slinks are queried");
        return results;
	}
	
	public String poRetrieve(String url, String proxy) throws ClientProtocolException, IOException {		
		String module = moduleFinder(url);
		return retrieve(module, url, proxy);
	}
	
	public String moduleFinder(String url) {
		String[] str = url.split("/");
		String service = str[2];
		if(service == null)
			return "not found!";
		if(mdmapsglkey.get(service) != null)
			return mdmapsglkey.get(service);		
		else {
			for(List<String> list: mdmapmultikey.keySet()){
				for(String key:list){
					if(key.equals(service)) {
						return mdmapmultikey.get(list);
					}
				}
			}
		}
		return "not found!";
		
	}
	
	/**
	 * retrieve all p and o for a given subject link
	 * @param module the value in the hashmap<Service, module>
	 * @param url the URL of the subject link
	 * 
	 */
	public String retrieve(String module, String url, String proxy) throws ClientProtocolException, IOException {
		String result = "";
		
		SPARQLHTTPClient c = new SPARQLHTTPClient();
		String query = "select ?p ?o"+System.getProperty("line.separator")
				+ "where {"+System.getProperty("line.separator")
				+ "<"+url+"> ?p ?o . "+System.getProperty("line.separator")
				+ "filter(?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> && ?p != <http://www.w3.org/2000/01/rdf-schema#label>"
				+ "	&& ?p != <http://yago-knowledge.org/resource/linksTo> && ?p != <http://yago-knowledge.org/resource/hasWikipediaAnchorText>"
				+ "&& ?p != <http://www.w3.org/2000/01/rdf-schema#comment> && ?p != <http://dbpedia.org/ontology/abstract> && ?p != <http://schema.org/description> )" + System.getProperty("line.separator")
				+ "}";
		
		String endpoint = "";
		String output = "";
		String get = URLEncoder.encode(query, "utf-8");
		
		
		switch (module) {
		case "lmdb":
			result = readRDF(url, null);
			result = rmSeeAlso(result);
			// content of 'comment' property span cross lines, combine together
			result = combineComment(result);
			break;
		case "wikidata":	
			endpoint = "https://query.wikidata.org/sparql?query=";
			output = "&format=json";
			result = c.httpGet(endpoint+get+output, proxy);
			result = JsonTransform(result);
			break;
		case "enakting": 
			result = readRDF(url, null);
			break;
		case "geonames":
			result = readRDF(url, null); 
			break;
		case "geovocab":
			result = readRDF(url, null);
			break;
		case "lgd": 
			result = readRDF(url, null);
			break;
		case "bbc":
			result = readRDF(url, null);
			break;
		case "yago":
			endpoint = "https://linkeddata1.calcul.u-psud.fr/sparql?query=";
			output = "&output=csv";
			result = c.httpGet(endpoint+get+output, proxy);			
			result = csvClean(result);
			break;
		case "dblite":
			result = readRDF(url, null);
			break;
		case "freebase":
			result = readRDF(url, "TTL");
			break;
		case "opencyc":
			result = readRDF(url, null);
			break;
		case "os":
			result = readRDF(url, null);
		case "mbrain":
			result = readRDF(url, null);
		default: 
			break;
		}	
		return result;
	}
	
	/*
	 * clean data retreived from a sparql endpoint
	 */
	public String csvClean(String origin) {
		if(origin.equals("")) return "";
		StringBuilder builder = new StringBuilder();		
		String[] str = origin.split(System.getProperty("line.separator"));
		String[] line = new String[2];
		int i = 0;

		while(i < str.length) {
			line = str[i].split(",");
			try {
				// if this line is a remaining part of the previous line because of
				// a new line symbol, it will be added to the end of the previous line
				// without line break
				if(line.length == 1) {
					builder.deleteCharAt(builder.length()-1);				
					if(line[0].charAt(0) == '"' && line[0].charAt(line[0].length()-1) == '"') {
						builder.append(line[0].substring(1, line[0].length()-1));
					} else if(line[0].charAt(0) == '"' && line[0].charAt(line[0].length()-1) != '"') {
						builder.append(line[0].substring(1, line[0].length()));
					} else if(line[0].charAt(line[0].length()-1) == '"' && line[0].charAt(0) != '"') {
						builder.append(line[0].substring(0, line[0].length()-1));
					} else {
						builder.append(line[0]);
					}				
				} else {
					if (line[0].charAt(0) != '"' && line[0].charAt(line[0].length()-1) != '"') {
						builder.deleteCharAt(builder.length()-1);
						builder.append(str[i].substring(0, str[i].length()-1));
				    } else if(line[1].charAt(0) == '"' && line[1].charAt(line[1].length()-1) == '"') {
						builder.append(line[0].substring(1, line[0].length()-1)+"\t"+line[1].substring(1, line[1].length()-1));
					} else if(line[1].charAt(0) == '"' && line[1].charAt(line[1].length()-1) != '"') {
						builder.append(line[0].substring(1, line[0].length()-1)+"\t"+line[1].substring(1, line[1].length()));
					} else if(line[1].charAt(line[1].length()-1) == '"' && line[1].charAt(0) != '"') {
						builder.append(line[0].substring(1, line[0].length()-1)+"\t"+line[1].substring(0, line[1].length()-1));
					} else {
						builder.append(line[0].substring(1, line[0].length()-1)+"\t"+line[1]);
					}
					
				}
				builder.append(System.lineSeparator());
					
			} catch(Exception e) {
				
			}
			
			i++;
		} 
				
		return builder.toString();
	}
	
	/*
	 *  transform JSON response from wikidata sparql endpoint
	 *  property-value pairs will be returned (statement values are removed)
	 */
	public String JsonTransform(String origin) {
		if(origin.equals("")) return "";
		StringBuilder builder = new StringBuilder();
		
		Gson gson = new Gson();
		Response response = gson.fromJson(origin, Response.class);
		
		ArrayList<ResponseBindings> bindings = response.getResults().getBindings();
		for(ResponseBindings rb:bindings) {
			String pred = rb.getP().get("value");			
			String obj = rb.getO().get("value");
			if(pred.contains("/prop/P")) {
				continue;
			}
			builder.append(pred+"\t"+obj);
			builder.append(System.lineSeparator());
		}
		
		return builder.toString();
	}

	public String combineComment(String response) {
		String[] str = response.split(System.lineSeparator());
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < str.length; i++) {
			String[] line = str[i].split("\t");
			if(line[0].equalsIgnoreCase("") && line[line.length-1].endsWith("details.")) {
				builder.append(line[line.length-1].trim());
				builder.append(System.lineSeparator());
			} else if(line[0].equalsIgnoreCase("") && !line[line.length-1].endsWith("details.")) {
				builder.append(line[line.length-1].trim()+" ");
			} else if (line[0].contains("#comment")) {
				builder.append(str[i]);
			} else {
				builder.append(str[i]);
				builder.append(System.lineSeparator());
			}
			
		}		
		return builder.toString();
	}
	
	public String rmSeeAlso(String predobjpair) {
		String[] str = predobjpair.split(System.lineSeparator());
		StringBuilder builder = new StringBuilder();
		
		for(String line:str) {
			if(line.contains("seeAlso") || line.contains("link_source")) {
				continue;
			}
			builder.append(line);
			builder.append(System.lineSeparator());
		}
		return builder.toString();
	}
	
	public String readRDF(String url, String lang) throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();
				
		Model model = ModelFactory.createDefaultModel();
		try {
			if(lang != null) {
				model.read(url, lang);
			} else {
				model.read(url);
			}
						
			StmtIterator it = model.listStatements();
			while(it.hasNext()) {
				Statement stmt = it.nextStatement();
				Property ppty = stmt.getPredicate();				
				RDFNode obj = stmt.getObject();
				String objcleaned = removeType(obj.toString());				
				builder.append(URLDecoder.decode(ppty.getURI(), "UTF-8")+"\t"+objcleaned);
				builder.append(System.lineSeparator());
			}
			
			return builder.toString();
		} catch (Exception e) {
			return "";
		}
	}
	
	public String readRDF(InputStream istream, String base) {
		StringBuilder builder = new StringBuilder();
		
		Model model = ModelFactory.createDefaultModel();
		try {
			
			model.read(istream, base);
						
			StmtIterator it = model.listStatements();
			while(it.hasNext()) {
				Statement stmt = it.nextStatement();
				Property ppty = stmt.getPredicate();				
				RDFNode obj = stmt.getObject();
				String objcleaned = removeType(obj.toString());				
				builder.append(URLDecoder.decode(ppty.getURI(), "UTF-8")+"\t"+objcleaned);
				builder.append(System.lineSeparator());
				
			}
			
			return builder.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public String getItemDocumentId(String url) {
		String[] str = url.split("/");
		return str[str.length-1];
	}

	public static String removeType(String str) {
		String[] split = str.split("\\^\\^");
		if(split.length == 1) {
			return str;
		} else {
			return split[0];
		}
	}
	
	/*
	 * update entity URLs if the domain is changed 
	 */
	public String formatUrl(String url) {
		StringBuilder builder = new StringBuilder();
		String[] str = url.split("/");
		for(int i = 0; i < str.length; i++) {
			if(str[i].equalsIgnoreCase("wikidata.org")) {
				builder.append("www.wikidata.org/");	
			} else if(i == str.length-1){
				builder.append(str[i]);
			} else {
				builder.append(str[i]+"/");
			}
		}		
		return builder.toString();		
	}

}

class Retriever implements Runnable{
	Integer lineId;
	String url;
	String proxy;
	SortedMap<Integer, String> results;
	
	Retriever(Integer lineId, String url, String proxy, SortedMap<Integer, String> results) {
		this.lineId = lineId;
		this.url = url;
		this.proxy = proxy;
		this.results = results;
	}
	
	public void run() {
		PredicateObjectRetriever por = new PredicateObjectRetriever();
		try {
			this.results.put(lineId, por.poRetrieve(this.url, this.proxy));
		} catch (ClientProtocolException e) {
			
		} catch (IOException e) {
			
		}
	}
}
