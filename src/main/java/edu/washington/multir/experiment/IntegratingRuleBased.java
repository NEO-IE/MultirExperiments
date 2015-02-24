//sg
/**
 * An attempt at integrating rule based system with multir
 */
package edu.washington.multir.experiment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import main.RuleBasedDriver;

import org.apache.commons.io.IOUtils;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.RelationMatching;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.Corpus;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.corpus.DocumentInformationI;
import edu.washington.multirframework.corpus.SentDependencyInformation;
import edu.washington.multirframework.corpus.SentInformationI;
import edu.washington.multirframework.corpus.TokenInformationI;
import edu.washington.multirframework.distantsupervision.NegativeExampleCollection;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;

public class IntegratingRuleBased {
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

	public IntegratingRuleBased() {
	}

	public IntegratingRuleBased(String propertiesFile) throws IOException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		String jsonProperties = IOUtils.toString(new FileInputStream(new File(
				propertiesFile)));
		Map<String, Object> properties = JsonReader.jsonToMaps(jsonProperties);
		rbased = new RuleBasedDriver(false);
		corpusPath = getStringProperty(properties, "corpusPath");
		evalOutputName = getStringProperty(properties, "evalOutputName");
		testDocumentsFile = getStringProperty(properties, "testDocumentsFile");
		String train = getStringProperty(properties, "train");
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

	public static void main(String args[]) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException,
			IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, IOException, SQLException {
		IntegratingRuleBased irb = new IntegratingRuleBased(args[0]);
		irb.iterSentences();

	}

	void iterSentences() throws SQLException, IOException {
		
		PrintWriter pw = new PrintWriter("rulebased_output_file");
		Corpus c = new Corpus(corpusPath, cis, true);
		Iterator<Annotation> di = c.getDocumentIterator();
		if (null == di) {
			System.out.println("NULL");
		}

		while (di.hasNext()) {
			Annotation d = di.next();
			if (null == d) {
				System.out.println(d);
			}
			int sentenceNumber = 0;
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
			for(CoreMap sentence : sentences) {
				assert(sentence.has(SentDependencyInformation.DependencyAnnotation.class));
				sentenceNumber++;
				System.out.println("Here");
				List<Triple<Integer, String, Integer>> deps = sentence.get(SentDependencyInformation.DependencyAnnotation.class);
				List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
				if(sentenceNumber % 1000 == 0) {
					System.out.println("At: " + sentenceNumber);
				}
				pw.write(sentence.toString() + " -> \n");
				pw.write(rbased.extractFromMultiRDepString(deps, sentence) + "\n\n");

			}
		}
		pw.close();
	}
}