package kmi.taa.core;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.nnsoft.sameas4j.DefaultSameAsServiceFactory;
import org.nnsoft.sameas4j.Equivalence;
import org.nnsoft.sameas4j.SameAsService;
import org.nnsoft.sameas4j.SameAsServiceException;


public class Crawler {
	final static Logger log = Logger.getLogger(Crawler.class.getName());

	public static String[] crawlsaspty(String res, String service, String proxy) throws ClientProtocolException, IOException {
		/*
		 *  The String query is the SPARQL query used to get the owl:sameAs subject links
		 *  from the knowledge graph where the source triples are from.
		 */
		String query = "prefix owl: <http://www.w3.org/2002/07/owl#>"+System.getProperty("line.separator")
				+ "select ?obj"+System.getProperty("line.separator")
				+ "where {"+System.getProperty("line.separator")
				+ "<"+res+"> owl:sameAs ?obj . "+System.getProperty("line.separator")
				+ "}";

		// converting a String to the application/x-www-form-urlencoded MIME format
		String get = service+"?query="
				+ URLEncoder.encode(query, "utf-8")
				+ "&output=csv";
				
		SPARQLHTTPClient client = new SPARQLHTTPClient();
		String slinks = client.httpGet(get, proxy);		
		
		/*
		 * remove the first line of slinks, i.e., "obj"
		 * also removes the double quotes wrapped around uris 
		 */
		String[] preformated = slinks.split(System.getProperty("line.separator"));		
		String[] formated = new String[preformated.length-1];
		int j = 0;
		for(int i = 0; i < preformated.length; i++){
			if(preformated[i].matches("\"http\\S+\"")){
				formated[j++] = preformated[i].substring(1, preformated[i].length()-1);
			}
		}

		return formated;
		
	}
	
	public Equivalence sameAsService(String res) throws URISyntaxException {
		SameAsService sameAsService = DefaultSameAsServiceFactory.createNew();
		Equivalence equivalence = null;
		try {
			equivalence = sameAsService.getDuplicates( new URI(res) );
		} catch (SameAsServiceException e) {			
			log.error("SameAsService error", e);
		}
		
		return equivalence;

	}
	
	/**
	 * Read source triples into a TreeMap
	 * TreeMap is RBTree based, the map is sorted according to the natural 
	 * ordering of the keys, time complexity is O(lg(n)).
	 * However, HashMap does not keep the ordering of keys in addition order,
	 * we want to keep the order of the source triples as it is in the triples file. 
	 * Key should be type of Integer as String has a different sort method 
	 */
	public TreeMap<Integer, String> readSourceTriples(String file) {
		TreeMap<Integer, String> map = new TreeMap<>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = "\t";

		try {
			br = new BufferedReader(new FileReader(file));
			while ((line = br.readLine()) != null) {
				String[] resource = line.split(cvsSplitBy);
				map.put(new Integer(resource[0]), resource[1]);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return map;
	 
	}
	
	public void run(String triplesFile, String dirOutput, String service, String proxy) throws ClientProtocolException, IOException, URISyntaxException {
		TreeMap<Integer, String> map = new TreeMap<>();		
		map = readSourceTriples(triplesFile);		
		log.info("Crawling subject links...");		
		String file = dirOutput+"subjectlinks.txt";		
		Files.deleteIfExists(Paths.get(dirOutput+"subjectlinks.txt"));		
		crawlAll(map, service, proxy, file);				
		log.info("Crawling subject links completed. ");
		
	}
	
	public void crawlAll(TreeMap<Integer, String> targetUrls, String service, String proxy, String otfile) {
		SortedSet<Integer> results = Collections.synchronizedSortedSet(new TreeSet<Integer>());
		ExecutorService pool = Executors.newFixedThreadPool(100);
		
		int howManyUrls = targetUrls.size();
		System.out.println("total " + howManyUrls + " to be processed");
		
		List<String> output = Collections.synchronizedList(new ArrayList<String>());
		for (Integer targetId : targetUrls.navigableKeySet()) {
			String uri = targetUrls.get(targetId);
			pool.execute(new Explorer(targetId, uri, service, proxy, results, otfile, output));
		}
		pool.shutdown();
	    		
		while(results.size() < howManyUrls) {
			System.out.println("already processed " + results.size() + " subject links");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error("crawlAll error", e);
			}
			
		}
		
		resultToFile(output, otfile);		
		System.out.println("already processed " + results.size() + " subject links");
	
	}
	
	// this is not synchronized and not append
	public void resultToFile(List<String> result, String fname) {
		File file = new File(fname);
		file.getParentFile().mkdirs();
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(fname, false));
			synchronized (result) {
				for(String str:result){
					bw.write(str);					
					bw.write(System.lineSeparator()); 
				}
			}
			bw.close();
		} catch (IOException e) {
			log.error("write fetched subject links to file error", e);
		}
		
		
	}
}

class Explorer implements Runnable {
	final static Logger log = Logger.getLogger(Explorer.class.getName());
	Integer targetId;
	String uri;
	String service;
	String proxy;
	SortedSet<Integer> results;
	String fname;
	List<String> output;
	
	public Explorer(Integer targetId, String uri, String service, String proxy, SortedSet<Integer> results, String fname, List<String> output) {
		this.targetId = targetId;
		this.uri = uri;
		this.service = service;
		this.proxy = proxy;
		this.results = results;
		this.fname = fname;
		this.output = output;
	}
	
	@Override
	public void run() {
		List<String> candidateUrls = new ArrayList<String>();
		if(this.service.contains("sparql")) {
				String[] formated;
				
					try {
						formated = Crawler.crawlsaspty(this.uri, this.service, this.proxy);
						for (String str : formated) {
							candidateUrls.add(this.targetId + "\t" + this.uri + "\t" + str + "\t"
									+ "owl:sameAs");
						}
					} catch (IOException e) {
						log.error("crawling via sameAs property error", e);
					}				

		}
		
		Equivalence equivalence;
		Crawler c = new Crawler();
		try {			
			equivalence = c.sameAsService(this.uri);
			if(equivalence != null) {
				for (URI euri : equivalence) {
					
					candidateUrls.add(this.targetId + "\t" + this.uri + "\t" + euri + "\t"
								+ "sameas.org service");									
				}
			}
		} catch (URISyntaxException e) {
			log.error("crawling via sameAs service error", e);
		}
		
		for(String url:candidateUrls) {
			output.add(url);
		}
		
		// add a processed URI to results
		this.results.add(this.targetId);

	}
	
}
