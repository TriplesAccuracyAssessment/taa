package kmi.taa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import kmi.taa.core.AbbreviationChecker;
import kmi.taa.core.Cleaner;
import kmi.taa.core.CompoundSplitter;
import kmi.taa.core.ConfidenceMeasurer;
import kmi.taa.core.Crawler;
import kmi.taa.core.FactTriples;
import kmi.taa.core.FileHelper;
import kmi.taa.core.SmallSetAnalyser;
import kmi.taa.core.OutlierDetector;
import kmi.taa.core.OutlierFilter;
import kmi.taa.core.PredicateObjectRetriever;
import kmi.taa.core.PropertyTypeComparison;
import kmi.taa.core.PropertyValueSelector;
import kmi.taa.core.SemanticSimilarityCalculator;
import kmi.taa.core.SemanticSimilarityFilter;
import kmi.taa.core.SubjectWeighter;
import kmi.taa.stringsimilarity.StringSimilarityMeasurer;

public class App {
	final static Logger log = Logger.getLogger(App.class.getName());
	//fixed_params:
	private String proxy;
	private String wklabels; //path to resource file of wikidata properties labels
	private String pyPath; //absolute path to the python software
	private String mPath; //absolute path to python scripts e.g. compF1.py to calculate F-measure
	//instance and extra_params:
    private String instance; //source triples file name
    private String pptysn;
    private String pptytype;
    private String workDir; //path as working dir and where retrieved triples are kept
    private String endpoint; //endpoint for source KG
    //config_params:
    private double alpha;
    private double beta;
    
    private static HashMap<String, Double> simMap = new HashMap<>();
    
    static {
    	try {
    		simMap = loadSimilarityMap();
		} catch (IOException e) {
			log.error("Load similarity map error", e);
		}
    }
	
	public static void main(String[] args) throws IOException {
		//args: ${FIXED_PARAMS} -i $INSTANCE ${EXTRA_PARAMS} ${CONFIG_PARAMS}
		App r = new App();
		r.parse_cmdline(args);				
		boolean flag = true;
		try {
			flag = r.matching(r.wklabels, r.instance, r.workDir, r.pptysn, r.pptytype, r.endpoint, r.proxy, r.alpha);
			writeSimilarityMap(simMap);
		} catch (URISyntaxException e) {
			log.error("Matching target tripls error", e);
		}
		if(!flag) {
			System.out.println("No matched triples are found.");
			log.info("No matched triples are found.");
			return;
		}
		HashMap<Integer,String> confMap = new HashMap<>();
		r.calculateConfidence(r.workDir, r.pptysn, r.pptytype, confMap);
		HashMap<Integer, String> stMap = new HashMap<>();
		FileHelper.readFileToMap(r.instance, stMap);
		StringBuilder builder = new StringBuilder();
		for(Integer id:stMap.keySet()) {
			if(confMap.get(id) == null) {
				builder.append(stMap.get(id) + "\t No matched triples found" + System.lineSeparator());
				continue;
			}
			if(Double.parseDouble(confMap.get(id)) >= r.beta)
				builder.append(stMap.get(id) + "\t True" + System.lineSeparator());
			else 
				builder.append(stMap.get(id) + "\t False" + System.lineSeparator());
		}		
		FileHelper.writeFile(builder.toString(), r.workDir+"confidence_"+r.beta+".txt", false);		
		
	}

	public void parse_cmdline(String[] args) {
		for(int i=0; i<args.length; i++) {
			switch(args[i]) {
			case "--proxy":
				this.proxy = args[++i];
				break;
			case "--wklabels":
				wklabels = args[++i];
				break;
			case "--pyPath":
				pyPath = args[++i];
				break;
			case "--mPath":
				mPath = args[++i];
				break;
			case "-i":
				instance = args[++i];
				break;
			case "--pptysn":
				pptysn = args[++i];
				break;
			case "--pptytype":
				pptytype = args[++i];
				break;
			case "--workDir":
				workDir = args[++i];
				break;
			case "--endpoint":
				endpoint = args[++i];
				break; 
			case "--alpha":
				alpha = Double.valueOf(args[++i]);
				break;
			case "--beta":
				beta = Double.valueOf(args[++i]);
				break;
			}				
		}		
	}
	
	// predicate matching between source triples and target triples
	public boolean matching(String wklabels, String sourceTriples, String workDir, String pptysn, String pptytype, String endpoint, String proxy, double alpha) throws IOException, URISyntaxException {
			
		Crawler crawler = new Crawler();
		crawler.run(sourceTriples, workDir, endpoint, proxy);

		Cleaner clearner = new Cleaner(workDir);
		clearner.run(proxy);

		PredicateObjectRetriever retriever = new PredicateObjectRetriever();
		retriever.execute(workDir + "cleaned_slinks.txt", workDir+ "predicates_objects.txt", proxy);
		
		//sort predicates_objects.txt by source triple id
		FileHelper.sort(workDir+ "predicates_objects.txt", 0, workDir+ "predicates_objects_sorted.txt");

		CompoundSplitter splitter = new CompoundSplitter(wklabels);
		splitter.run(workDir);
				
		SemanticSimilarityCalculator sscalculator = new SemanticSimilarityCalculator(pyPath, mPath);

		AbbreviationChecker abbchecker = new AbbreviationChecker();

		SemanticSimilarityFilter ssfilter = new SemanticSimilarityFilter();

		PropertyTypeComparison ptcom = new PropertyTypeComparison();

		PropertyValueSelector ps = new PropertyValueSelector();

		OutlierDetector outdet = new OutlierDetector();

		OutlierFilter outfil = new OutlierFilter();

		SmallSetAnalyser ssa = new SmallSetAnalyser();		

		// convert date property into the second format
		if (pptysn.toLowerCase().contains("date")) {
			FactTriples ft = new FactTriples();
			ft.convertDate(sourceTriples, workDir + "sourceTriples_converted" + pptysn + ".txt");
		} else {
			Path source = Paths.get(sourceTriples);
			Path target = Paths.get(workDir + "sourceTriples_converted" + pptysn + ".txt");
			try {
				Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				log.error("Convert source triples error", e);
			}
		}

		// predicate semantic similarity matching
		String split = splitter.formatLabel(pptysn);
		sscalculator.run(simMap, split, workDir + "formatted_predicates.txt", workDir + "similarity_" + pptysn + "_all.txt");
		abbchecker.check(pptysn, workDir + "similarity_" + pptysn + "_all.txt", workDir + "similarity_" + pptysn + "_abbr.txt");
		ssfilter.filter(alpha, workDir + "similarity_" + pptysn + "_abbr.txt",
				workDir + "similarity_" + pptysn + "_after_threshold.txt");

		/*
		 * after applying a threshold to filter low similarity properties, the
		 * resulting similarity file may not contain any candidate triples.
		 */
		if (isFileEmpty(workDir + "similarity_" + pptysn + "_after_threshold.txt")) {
			System.out.println("Similarity file is empty.");
			return false;
		}

		// property type comparison
		ptcom.run(pptytype, workDir + "similarity_" + pptysn + "_after_threshold.txt",
				workDir + "similarity_" + pptysn + "_typecomp.txt");

		// calculate objects' string similarity for String type objects
		if (pptytype.equalsIgnoreCase("String")) {
			StringSimilarityMeasurer ngm = new StringSimilarityMeasurer();
			try {
				// add line id at the beginning
				FileHelper.addLineIds(workDir + "similarity_" + pptysn + "_typecomp.txt",
						workDir + "similarity_" + pptysn + "_typecomp.txt");
				ngm.doComparison(workDir + "sourceTriples_converted" + pptysn + ".txt",
						workDir + "similarity_" + pptysn + "_typecomp.txt",
						workDir + "similarity_" + pptysn + "_typecomp.txt");
			} catch (IOException e) {
				log.error("Comparing string similarity of objects error", e);
			}
		}

		/*
		 * when one or more evidence values for one property, select the closer
		 * one. this step needs to be done before the outlier detection.
		 */
		ssa.addLineids(Paths.get(workDir + "similarity_" + pptysn + "_typecomp.txt"),
				workDir + "similarity_" + pptysn + "_typecomp.txt");
		if (pptytype.equalsIgnoreCase("String")) {
			ps.selectStringTypeSingleValue(workDir + "similarity_" + pptysn + "_typecomp.txt",
					workDir + "similarity_" + pptysn + "_singlepropvalue.txt");
		} else if (pptytype.equalsIgnoreCase("numerical") || pptytype.equalsIgnoreCase("date")) {
			ps.select(workDir + "sourceTriples_converted" + pptysn + ".txt", workDir + "similarity_" + pptysn + "_typecomp.txt",
					pptytype, workDir + "similarity_" + pptysn + "_singlepropvalue.txt");
		}

		/*
		 * outlier detection only for numerical or date object values
		 */
		if (pptytype.equalsIgnoreCase("numerical") || pptytype.equalsIgnoreCase("date")) {
			outdet.start(pptytype, workDir + "similarity_" + pptysn + "_singlepropvalue.txt", 5,
					workDir + "sourceTriples_converted" + pptysn + ".txt", workDir + "similarity_" + pptysn + "_outliers.txt");
			// outliers filter - remove outliers (which are marked as 'yes')
			outfil.run(workDir + "similarity_" + pptysn + "_outliers.txt",
					workDir + "similarity_" + pptysn + "_outliers_filtered.txt");

			// outlier detection for small set of data values
			ssa.run(workDir + "sourceTriples_converted" + pptysn + ".txt", workDir + "similarity_" + pptysn + "_outliers_filtered.txt",
					workDir + "similarity_" + pptysn + "_ssa.txt", 1, pptytype);
			outfil.run(workDir + "similarity_" + pptysn + "_ssa.txt",
					workDir + "similarity_" + pptysn + "_ssa_filtered.txt");
			outfil.clean(workDir + "similarity_" + pptysn + "_ssa_filtered.txt", workDir + "similarity_" + pptysn + ".txt");
		} else {
			Path source = Paths.get(workDir + "similarity_" + pptysn + "_singlepropvalue.txt");
			Path target = Paths.get(workDir + "similarity_" + pptysn + ".txt");
			try {
				Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
				FileHelper.removeLineIds(workDir + "similarity_" + pptysn + ".txt",
						workDir + "similarity_" + pptysn + ".txt");
			} catch (IOException e) {
				log.error("Copy simililarity file error", e);
			}
		}
		
		return true;

	}
	
	public static boolean isFileEmpty(String file) {
		boolean empty = false;
		try {
			List<String> contentList = Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8);
			if(contentList.isEmpty()) {
				empty = true;
			} else {
				empty = false;
			}
		} catch (IOException e) {
//			e.printStackTrace();
		}
		
		return empty;
	}
	
	
	public void calculateConfidence(String workDir, String pptysn, String pptytype, HashMap<Integer,String> map)
			throws IOException {

		SubjectWeighter sweighter = new SubjectWeighter();

		ConfidenceMeasurer cfmeasurer = new ConfidenceMeasurer();

		// assigning reliability of the equivalent subject links
		sweighter.run(workDir + "similarity_" + pptysn + ".txt", workDir + "matched_triples_" + pptysn + ".txt");

		// calculating confidence scores
		if (pptytype.equalsIgnoreCase("numerical") || pptytype.equalsIgnoreCase("date")) {
			cfmeasurer.run(workDir + "sourceTriples_converted" + pptysn + ".txt", workDir + "matched_triples_" + pptysn + ".txt",
					workDir + "confidence_" + pptysn + ".txt",
					workDir + "evidenceTriples_property_type_not_matched_" + pptysn + ".txt",
					workDir + "targetTripleNotProvided_" + pptysn + ".txt", true);
		} else {
			cfmeasurer.run(workDir + "sourceTriples_converted" + pptysn + ".txt", workDir + "matched_triples_" + pptysn + ".txt",
					workDir + "confidence_" + pptysn + ".txt",
					workDir + "evidenceTriples_property_type_not_matched_" + pptysn + ".txt",
					workDir + "targetTripleNotProvided_" + pptysn + ".txt", false);
		}

		// read confidence scores from file to ArrayList object
		readConfidence(workDir + "confidence_" + pptysn + ".txt", map);

	}
	
	public void readConfidence(String ipFile, HashMap<Integer,String> map) throws IOException {
		List<String> confList = Files.readAllLines(Paths.get(ipFile), StandardCharsets.UTF_8);
		for(String line:confList) {
			String[] split = line.split("\t");
			map.put(Integer.valueOf(split[0]), split[4]);
		}
	}
	
	public String readArg2(String gsLabelFile, HashMap<Integer, String> confMap) throws IOException {
		StringBuilder gsLs = new StringBuilder();
		List<String> gsList = Files.readAllLines(Paths.get(gsLabelFile), StandardCharsets.UTF_8);	
		for(Integer id:confMap.keySet()) {
			gsLs.append(gsList.get(id-1) + ",");
		}
		return gsLs.toString();
	}
	
	private static HashMap<String, Double> loadSimilarityMap() throws IOException {
		HashMap<String, Double> map = new HashMap<>();
		if(Files.notExists(Paths.get("./index/similarity.txt"))) {
			File file = new File("./index/similarity.txt");	
			file.getParentFile().mkdirs();
			file.createNewFile();
		} else {
			List<String> cont = Files.readAllLines(Paths.get("./index/similarity.txt"), StandardCharsets.UTF_8);
			for(String s:cont) {
				if(s.equalsIgnoreCase("pair,score")) continue;
				String[] split = s.split(",");
				map.put(split[0], Double.valueOf(split[1]));
			}
		}
		return map;
	}
	
	private static void writeSimilarityMap(HashMap<String, Double> map) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter("./index/similarity.txt", false));
			writer.write("pair,score");
			writer.newLine();
			for(String pair:map.keySet()) {											
				writer.write(pair+","+map.get(pair));	
				writer.newLine();
			}
		} catch (IOException e) {
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
			}
		}
		
	}

}
