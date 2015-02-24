//sg
/**
 * An attempt at integrating rule based system with multir
 */
package edu.washington.multir.distantsupervision.RulebasedDistantSupervision;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import main.RuleBasedDriver;

import org.apache.commons.io.IOUtils;
import org.apache.derby.tools.sysinfo;

import util.Number;
import util.Relation;
import util.Word;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.RelationMatching;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.corpus.DocumentInformationI;
import edu.washington.multirframework.corpus.SentDependencyInformation;
import edu.washington.multirframework.corpus.SentInformationI;
import edu.washington.multirframework.corpus.TokenInformationI;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetBeginAnnotation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetEndAnnotation;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.distantsupervision.NegativeExampleCollection;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;

public class RulebasedDistantSupervision {
	private String corpusPath;
	private String typeRelMapPath;
	private ArgumentIdentification ai;
	private FeatureGenerator fg;
	private List<SententialInstanceGeneration> sigs;
	private List<String> DSFiles;
	private List<String> oldFeatureFiles;
	private List<String> featureFiles;
	private List<String> multirDirs;
	private List<String> oldMultirDirs;
	private RelationMatching rm;
	private NegativeExampleCollection nec;
	private KnowledgeBase kb;
	private String testDocumentsFile;
	private CorpusInformationSpecification cis;
	private String evalOutputName;
	private boolean train = false;
	private boolean useFiger = false;

	private Integer featureThreshold = 2;
	private boolean strictNegativeGeneration = false;
	private RuleBasedDriver rbased;
	private Map<String, String> countryFreebaseIdMap;

	public RulebasedDistantSupervision() {
	}

	public RulebasedDistantSupervision(String propertiesFile) throws Exception {

		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		String jsonProperties = IOUtils.toString(new FileInputStream(new File(
				propertiesFile)));
		Map<String, Object> properties = JsonReader.jsonToMaps(jsonProperties);

		rbased = new RuleBasedDriver(true);
		corpusPath = getStringProperty(properties, "corpusPath");
		evalOutputName = getStringProperty(properties, "evalOutputName");
		testDocumentsFile = getStringProperty(properties, "testDocumentsFile");
		String train = getStringProperty(properties, "train");

		/**
		 * Create the entity name to id map
		 */
		String countriesFile = "data/numericalkb/countries_list_ids";

		try {

			BufferedReader br = new BufferedReader(
					new FileReader(countriesFile));
			String countryRecord = null;
			countryFreebaseIdMap = new HashMap<String, String>();
			while ((countryRecord = br.readLine()) != null) {
				String vars[] = countryRecord.split("\t");
				String countryName = vars[1].toLowerCase();
				String countryId = vars[0];
				// System.out.println(countryName);
				countryFreebaseIdMap.put(countryName, countryId);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/**
		 * end creating the map
		 */

		double necRatio = 4.0;
		if (train != null) {
			if (train.equals("false")) {
				this.train = false;
			} else if (train.equals("true")) {
				this.train = true;
			}
		}

		String strictNegativeGenerationString = getStringProperty(properties,
				"strictNegativeGeneration");
		if (strictNegativeGenerationString != null) {
			if (strictNegativeGenerationString.equals("true")) {
				strictNegativeGeneration = true;
			}
		}

		String featThresholdString = getStringProperty(properties,
				"featureThreshold");
		if (featThresholdString != null) {
			this.featureThreshold = Integer.parseInt(featThresholdString);
		}

		String useFiger = getStringProperty(properties, "useFiger");
		if (useFiger != null) {
			if (useFiger.equals("true")) {
				this.useFiger = true;
			}
		}
		String featureGeneratorClass = getStringProperty(properties, "fg");
		if (featureGeneratorClass != null) {
			fg = (FeatureGenerator) ClassLoader.getSystemClassLoader()
					.loadClass(featureGeneratorClass).newInstance();
		}

		String aiClass = getStringProperty(properties, "ai");
		if (aiClass != null) {
			ai = (ArgumentIdentification) ClassLoader.getSystemClassLoader()
					.loadClass(aiClass).getMethod("getInstance").invoke(null);
		}

		String rmClass = getStringProperty(properties, "rm");
		if (rmClass != null) {
			rm = (RelationMatching) ClassLoader.getSystemClassLoader()
					.loadClass(rmClass).getMethod("getInstance").invoke(null);
		}

		String necRatioString = getStringProperty(properties, "necRatio");
		if (necRatioString != null) {
			necRatio = Double.parseDouble(necRatioString);
		}

		String necClass = getStringProperty(properties, "nec");
		if (necClass != null) {
			nec = (NegativeExampleCollection) ClassLoader
					.getSystemClassLoader().loadClass(necClass)
					.getMethod("getInstance", double.class)
					.invoke(null, necRatio);
		}

		String kbRelFile = getStringProperty(properties, "kbRelFile");
		String kbEntityFile = getStringProperty(properties, "kbEntityFile");
		String targetRelFile = getStringProperty(properties, "targetRelFile");
		if (kbRelFile != null && kbEntityFile != null && targetRelFile != null) {
			kb = new KnowledgeBase(kbRelFile, kbEntityFile, targetRelFile);
		}

		List<String> sigClasses = getListProperty(properties, "sigs");
		sigs = new ArrayList<>();
		for (String sigClass : sigClasses) {
			sigs.add((SententialInstanceGeneration) ClassLoader
					.getSystemClassLoader().loadClass(sigClass)
					.getMethod("getInstance").invoke(null));
		}

		List<String> dsFileNames = getListProperty(properties, "dsFiles");
		DSFiles = new ArrayList<>();
		for (String dsFileName : dsFileNames) {
			DSFiles.add(dsFileName);
		}

		List<String> oldFeatureFileNames = getListProperty(properties,
				"oldFeatureFiles");
		oldFeatureFiles = new ArrayList<>();
		for (String oldFeatureFileName : oldFeatureFileNames) {
			oldFeatureFiles.add(oldFeatureFileName);
		}

		List<String> featureFileNames = getListProperty(properties,
				"featureFiles");
		featureFiles = new ArrayList<>();
		for (String featureFileName : featureFileNames) {
			featureFiles.add(featureFileName);
		}

		List<String> oldMultirDirNames = getListProperty(properties,
				"oldModels");
		oldMultirDirs = new ArrayList<>();
		for (String oldMultirDirName : oldMultirDirNames) {
			oldMultirDirs.add(oldMultirDirName);
		}

		multirDirs = new ArrayList<>();
		List<String> multirDirNames = getListProperty(properties, "models");
		for (String multirDirName : multirDirNames) {
			multirDirs.add(multirDirName);
		}

		cis = new CustomCorpusInformationSpecification();

		String altCisString = getStringProperty(properties, "cis");
		if (altCisString != null) {
			cis = (CustomCorpusInformationSpecification) ClassLoader
					.getSystemClassLoader().loadClass(altCisString)
					.newInstance();
		}

		// CorpusInformationSpecification
		List<String> tokenInformationClassNames = getListProperty(properties,
				"ti");
		List<TokenInformationI> tokenInfoList = new ArrayList<>();
		for (String tokenInformationClassName : tokenInformationClassNames) {
			tokenInfoList.add((TokenInformationI) ClassLoader
					.getSystemClassLoader()
					.loadClass(tokenInformationClassName).newInstance());
		}

		List<String> sentInformationClassNames = getListProperty(properties,
				"si");
		List<SentInformationI> sentInfoList = new ArrayList<>();
		for (String sentInformationClassName : sentInformationClassNames) {
			sentInfoList.add((SentInformationI) ClassLoader
					.getSystemClassLoader().loadClass(sentInformationClassName)
					.newInstance());
		}

		List<String> docInformationClassNames = getListProperty(properties,
				"di");
		List<DocumentInformationI> docInfoList = new ArrayList<>();
		for (String docInformationClassName : docInformationClassNames) {
			docInfoList.add((DocumentInformationI) ClassLoader
					.getSystemClassLoader().loadClass(docInformationClassName)
					.newInstance());
		}

		CustomCorpusInformationSpecification ccis = (CustomCorpusInformationSpecification) cis;
		ccis.addDocumentInformation(docInfoList);
		ccis.addTokenInformation(tokenInfoList);
		ccis.addSentenceInformation(sentInfoList);

		typeRelMapPath = getStringProperty(properties, "typeRelMap");

	}

	private List<String> getListProperty(Map<String, Object> properties,
			String string) {
		if (properties.containsKey(string)) {
			JsonObject obj = (JsonObject) properties.get(string);
			List<String> returnValues = new ArrayList<>();
			for (Object o : obj.getArray()) {
				returnValues.add(o.toString());
			}
			return returnValues;
		}
		return new ArrayList<>();
	}

	private String getStringProperty(Map<String, Object> properties, String str) {
		if (properties.containsKey(str)) {
			if (properties.get(str) == null) {
				return null;
			} else {
				return properties.get(str).toString();
			}
		}
		return null;
	}

	public static void main(String args[]) throws Exception {
		RulebasedDistantSupervision irb = new RulebasedDistantSupervision(
				args[0]);
		irb.iterSentences();
	}

	void iterSentences() throws SQLException, IOException {
		PrintWriter pw = new PrintWriter("data/rulebased_output_file");
		Corpus c = new Corpus(corpusPath, cis, true);
		System.out.println("Reading the docs");
		Iterator<Annotation> di = c.getDocumentIterator();
		if (null == di) {
			System.out.println("NULL");
		}
		// this is where we store results
		List<Pair<Triple<KBArgument, KBArgument, String>, Integer>> kbArgRelnList = new ArrayList<Pair<Triple<KBArgument, KBArgument, String>, Integer>>();
		ArrayList<Pair<Integer, Relation>> relationList = new ArrayList<Pair<Integer, Relation>>();
		int sentID = 0;
		System.out.println("Starting rule based distant supervision");
		int numProcessed = 0, ignored = 0;
		while (di.hasNext()) {
			Annotation d = di.next();
			if (null == d) {
				System.out.println(d);
			}
			//System.out.println("Doc Shift");
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
			//System.out.println("Got " + sentences.size() + " sentences");
			
			for (CoreMap sentence : sentences) {
				numProcessed++;
			
				sentID = sentence.get(SentGlobalID.class);
				if(numProcessed % 100 == 0) {
					System.out.println("Processed: " + numProcessed + " , ignored: " + ignored);
				}
				
				if(sentence.toString().length() > 350) {
					ignored++;
					continue;
				}
				try {
					List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
					for(int i =0; i < tokens.size(); i++){
						CoreLabel token = tokens.get(i);
						int begOffset = token.get(SentenceRelativeCharacterOffsetBeginAnnotation.class);
						int endOffset = token.get(SentenceRelativeCharacterOffsetEndAnnotation.class);
						token.set(CoreAnnotations.TokenBeginAnnotation.class, begOffset);
						token.set(CoreAnnotations.TokenEndAnnotation.class, endOffset);
						
					}
					List<Triple<Integer, String, Integer>> deps = sentence.get(SentDependencyInformation.DependencyAnnotation.class);
					List<Relation> currRels = rbased.extractFromMultiRDepString(deps, sentence);
					for (Relation rel : currRels) {
						relationList.add(new Pair<Integer, Relation>(sentID, rel));
					}
					
				} catch (Exception e) {
					System.out.println(e);
					continue; //ssh
				}
			
			}
			
		}
		for (Pair<Integer, Relation> irPair : relationList) {
			Relation r = irPair.second;
			sentID = irPair.first;
			Word location = r.getCountry();
			Number number = r.getNumber();
			String relName = r.getRelName();
			// System.out.println(location);
			String countryId = countryFreebaseIdMap.get(location.getVal());
			KBArgument countryArg = new KBArgument(new Argument(
					location.getVal(), location.getStartOff(),
					location.getEndOff()), countryId);
			KBArgument numberArg = new KBArgument(new Argument(number.getVal(),
					number.getStartOff(), number.getEndOff()), number.getVal());
			Triple<KBArgument, KBArgument, String> relTrip = new Triple<KBArgument, KBArgument, String>(
					countryArg, numberArg, relName);
			Pair<Triple<KBArgument, KBArgument, String>, Integer> relTripSentIdPair = new Pair<>(
					relTrip, sentID);
			kbArgRelnList.add(relTripSentIdPair);
		}
		writeDistantSupervisionAnnotations(kbArgRelnList, pw);
		pw.close();
	}

	public static void writeDistantSupervisionAnnotations(
			List<Pair<Triple<KBArgument, KBArgument, String>, Integer>> distantSupervisionAnnotations,
			PrintWriter dsWriter) {
		int i = 0;
		for (Pair<Triple<KBArgument, KBArgument, String>, Integer> dsAnno : distantSupervisionAnnotations) {
			Triple<KBArgument, KBArgument, String> trip = dsAnno.first;
			Integer sentGlobalID = dsAnno.second;
			KBArgument arg1 = trip.first;
			KBArgument arg2 = trip.second;
			if (null == arg1) {
				System.out.println("here");
				continue;
			}
			String rel = trip.third;
			try {
				dsWriter.write(arg1.getKbId()); // for missing countries
			} catch (Exception e) {
				continue;
			}
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg1.getStartOffset()));
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg1.getEndOffset()));
			dsWriter.write("\t");
			dsWriter.write(arg1.getArgName());
			dsWriter.write("\t");
			dsWriter.write(arg2.getKbId());
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg2.getStartOffset()));
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg2.getEndOffset()));
			dsWriter.write("\t");
			dsWriter.write(arg2.getArgName());
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(sentGlobalID));
			dsWriter.write("\t");
			dsWriter.write(rel);
			dsWriter.write("\n");
		}
	}

	/**
	 * Just to test whether this arrangement will work or not
	 * 
	 * @param sent
	 */
	public void contactUnitServer(String sent) {
		try {
			Socket unitSocket = new Socket("localhost", 9080);
			PrintWriter out = new PrintWriter(unitSocket.getOutputStream(),
					true);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					unitSocket.getInputStream()));
			out.println("100:The speed of light is 100 m/s");
			String str = null;
			System.out.println(in.readLine());
			unitSocket.close();

		} catch (Exception e) {
			System.out.println(e);
		}
	}

}
