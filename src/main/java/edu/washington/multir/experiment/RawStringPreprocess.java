//sg
package edu.washington.multir.experiment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.preprocess.CorpusPreprocessing;
import edu.washington.multir.sententialextraction.DocumentExtractor;
import edu.washington.multir.util.EvaluationUtils;
import edu.washington.multir.util.ModelUtils;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.RelationMatching;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.CorpusInformationSpecification;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentDocNameInformation.SentDocName;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multirframework.corpus.CustomCorpusInformationSpecification;
import edu.washington.multirframework.corpus.DocumentInformationI;
import edu.washington.multirframework.corpus.SentInformationI;
import edu.washington.multirframework.corpus.TokenInformationI;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.Extraction;
import edu.washington.multirframework.distantsupervision.NegativeExampleCollection;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;
import edu.washington.multirframework.knowledgebase.KnowledgeBase;

public class RawStringPreprocess {
	static Set<String> targetRelations;
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
	
    public RawStringPreprocess(String propertiesFile) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		String jsonProperties = IOUtils.toString(new FileInputStream(new File(propertiesFile)));
		Map<String,Object> properties = JsonReader.jsonToMaps(jsonProperties);
		
		corpusPath = getStringProperty(properties,"corpusPath");
		evalOutputName = getStringProperty(properties,"evalOutputName");
		testDocumentsFile = getStringProperty(properties,"testDocumentsFile");
		String train = getStringProperty(properties,"train");
		double necRatio = 4.0;
		if(train!=null){
			if(train.equals("false")){
				this.train = false;
			}
			else if(train.equals("true")){
				this.train = true;
			}
		}
		
		String strictNegativeGenerationString = getStringProperty(properties,"strictNegativeGeneration");
		if(strictNegativeGenerationString != null){
			if(strictNegativeGenerationString.equals("true")){
				strictNegativeGeneration = true;
			}
		}
		
		String featThresholdString = getStringProperty(properties,"featureThreshold");
		if(featThresholdString!=null){
			this.featureThreshold = Integer.parseInt(featThresholdString);
		}
		
		String useFiger = getStringProperty(properties,"useFiger");
		if(useFiger!=null){
			if(useFiger.equals("true")){
				this.useFiger = true;
			}
		}
		String featureGeneratorClass = getStringProperty(properties,"fg");
		if(featureGeneratorClass != null){
			fg = (FeatureGenerator) ClassLoader.getSystemClassLoader().loadClass(featureGeneratorClass).newInstance();
		}
		
		String aiClass = getStringProperty(properties,"ai");
		if(aiClass != null){
			ai = (ArgumentIdentification) ClassLoader.getSystemClassLoader().loadClass(aiClass).getMethod("getInstance").invoke(null);
		}
		
		String rmClass = getStringProperty(properties,"rm");
		if(rmClass != null){
			rm = (RelationMatching) ClassLoader.getSystemClassLoader().loadClass(rmClass).getMethod("getInstance").invoke(null);
		}
		
		String necRatioString = getStringProperty(properties,"necRatio");
		if(necRatioString!=null){
			necRatio = Double.parseDouble(necRatioString);
		}
		
		String necClass = getStringProperty(properties,"nec");
		if(necClass != null){
			nec = (NegativeExampleCollection) ClassLoader.getSystemClassLoader().loadClass(necClass).getMethod("getInstance", double.class).invoke(null,necRatio);
		}
		
		String kbRelFile = getStringProperty(properties,"kbRelFile");
		String kbEntityFile = getStringProperty(properties,"kbEntityFile");
		String targetRelFile = getStringProperty(properties,"targetRelFile");
		if(kbRelFile!=null && kbEntityFile!=null && targetRelFile != null){
			kb = new KnowledgeBase(kbRelFile,kbEntityFile,targetRelFile);
		}
		
		List<String> sigClasses = getListProperty(properties,"sigs");
		sigs = new ArrayList<>();
		for(String sigClass : sigClasses){
			sigs.add((SententialInstanceGeneration)ClassLoader.getSystemClassLoader().loadClass(sigClass).getMethod("getInstance").invoke(null));
		}
		
		List<String> dsFileNames = getListProperty(properties,"dsFiles");
		DSFiles = new ArrayList<>();
		for(String dsFileName : dsFileNames){
			DSFiles.add(dsFileName);
		}
		
		List<String> oldFeatureFileNames = getListProperty(properties,"oldFeatureFiles");
		oldFeatureFiles = new ArrayList<>();
		for(String oldFeatureFileName : oldFeatureFileNames){
			oldFeatureFiles.add(oldFeatureFileName);
		}
		
		List<String> featureFileNames = getListProperty(properties,"featureFiles");
		featureFiles = new ArrayList<>();
		for(String featureFileName : featureFileNames){
			featureFiles.add(featureFileName);
		}
		
		List<String> oldMultirDirNames = getListProperty(properties,"oldModels");
		oldMultirDirs = new ArrayList<>();
		for(String oldMultirDirName : oldMultirDirNames){
			oldMultirDirs.add(oldMultirDirName);
		}
		
		multirDirs = new ArrayList<>();
		List<String> multirDirNames = getListProperty(properties,"models");
		for(String multirDirName : multirDirNames){
			multirDirs.add(multirDirName);
		}
		
		cis = new CustomCorpusInformationSpecification();
		
		String altCisString = getStringProperty(properties,"cis");
		if(altCisString != null){
			cis = (CustomCorpusInformationSpecification)ClassLoader.getSystemClassLoader().loadClass(altCisString).newInstance();
		}
		
		//CorpusInformationSpecification
		List<String> tokenInformationClassNames = getListProperty(properties,"ti");
		List<TokenInformationI> tokenInfoList = new ArrayList<>();
		for(String tokenInformationClassName : tokenInformationClassNames){
			tokenInfoList.add((TokenInformationI)ClassLoader.getSystemClassLoader().loadClass(tokenInformationClassName).newInstance());
		}
		
		List<String> sentInformationClassNames = getListProperty(properties,"si");
		List<SentInformationI> sentInfoList = new ArrayList<>();
		for(String sentInformationClassName : sentInformationClassNames){
			sentInfoList.add((SentInformationI)ClassLoader.getSystemClassLoader().loadClass(sentInformationClassName).newInstance());
		}
		
		List<String> docInformationClassNames = getListProperty(properties,"di");
		List<DocumentInformationI> docInfoList = new ArrayList<>();
		for(String docInformationClassName : docInformationClassNames){
			docInfoList.add((DocumentInformationI)ClassLoader.getSystemClassLoader().loadClass(docInformationClassName).newInstance());
		}
		
		CustomCorpusInformationSpecification ccis = (CustomCorpusInformationSpecification)cis;
		ccis.addDocumentInformation(docInfoList);
		ccis.addTokenInformation(tokenInfoList);
		ccis.addSentenceInformation(sentInfoList);
		
		
		typeRelMapPath = getStringProperty(properties,"typeRelMap");
		
	}
	public static void main(String args[]) {
		try {
			
			String[] relations = {"/people/person/gender", "/people/person/place_of_birth", "/people/person/profession", "/people/person/nationality"};
			targetRelations = new HashSet<String>(Arrays.asList(relations));
			RawStringPreprocess rsp = new RawStringPreprocess("sgconfig.json");
			Annotation a = CorpusPreprocessing.getTestDocumentFromRawString("Moscow is the capital of Russia", "doc1");
			List<CoreMap> sentences = a.get(CoreAnnotations.SentencesAnnotation.class);
			DocumentExtractor de = new DocumentExtractor("data/multir-extractor", rsp.fg, rsp.ai, rsp.sigs.get(0));
			/*
			for(CoreMap sentence : sentences) { //
				for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
			        // this is the text of the token
			        String word = token.get(TextAnnotation.class);
			        // this is the POS tag of the token
			        String pos = token.get(PartOfSpeechAnnotation.class);
			        // this is the NER label of the token
			        String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class); 
			        System.out.println(word + " POS " + pos + " NER " + ne);
			      }
			}
			*/
			ArrayList<Annotation> docCollection = new ArrayList<Annotation>();
			docCollection.add(a);
			List<Extraction> extrns = getExtractions(docCollection.iterator(), rsp.ai, rsp.sigs.get(0), de);
			System.out.println(extrns);
		} catch (IOException | InterruptedException | InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | SQLException e) {
			// TODO Auto-generated catch block		
			e.printStackTrace();
		}
	}
	/**
	 * Customized version of extractor taken from ManualExtraction.java
	 * @param Iterator<Annotation> : A list of annotations
	 * @param ArgumentIdentificationCode 
	 * @param sig
	 * @param de
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	private static List<Extraction> getExtractions(Iterator<Annotation> docs,
			ArgumentIdentification ai, SententialInstanceGeneration sig,
			DocumentExtractor de) throws SQLException, IOException {
		List<Extraction> extrs = new ArrayList<Extraction>();
		Map<Integer,String> ftID2ftMap = ModelUtils.getFeatureIDToFeatureMap(de.getMapping());
		while(docs.hasNext()){
			Annotation doc = docs.next(); //get the current doc
			//One annotation is one document, basically one span of text
			/*
			 * Annotation basically stores a map from various properties of the text span to the corresponding values,
			 * http://nlp.stanford.edu/software/corenlp.shtml contains more	
			 */
			List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class); //Get all the sentences
			int sentenceCount =1;
			for(CoreMap sentence : sentences) { //
				//argument identification
				System.out.println(sentence.size());
				List<Argument> arguments =  ai.identifyArguments(doc,sentence);
				System.out.println("Arguments : " + arguments);
				//sentential instance generation
				List<Pair<Argument,Argument>> sententialInstances = sig.generateSententialInstances(arguments, sentence);
				System.out.println("Pairs: " + sententialInstances);
				for(Pair<Argument,Argument> p : sententialInstances){
					Pair<Triple<String,Double,Double>,Map<Integer,Double>> extrResult = 
					de.extractFromSententialInstanceWithFeatureScores(p.first, p.second, sentence, doc);
					if(extrResult != null){
						Triple<String,Double,Double> extrScoreTripe = extrResult.first;
						Map<Integer,Double> featureScores = extrResult.second;
						String rel = extrScoreTripe.first;
						if(targetRelations.contains(rel)){
							String docName = sentence.get(SentDocName.class);
							String senText = sentence.get(CoreAnnotations.TextAnnotation.class);
							Integer sentNum = sentence.get(SentGlobalID.class);
							Extraction e = new Extraction(p.first,p.second,docName,rel,sentNum,extrScoreTripe.third,senText);
							e.setFeatureScoreList(EvaluationUtils.getFeatureScoreList(featureScores, ftID2ftMap));
							extrs.add(e);
						}
					}
				}
				sentenceCount++;
			}
		}
		return EvaluationUtils.getUniqueList(extrs);
	}
	
	private String getStringProperty(Map<String,Object> properties, String str) {
		if(properties.containsKey(str)){
			if(properties.get(str)== null){
				return null;
			}
			else{
				return properties.get(str).toString();
			}
		}
		return null;
	}
	
	private List<String> getListProperty(Map<String, Object> properties,
			String string) {
		if(properties.containsKey(string)){
			JsonObject obj = (JsonObject) properties.get(string);
			List<String> returnValues = new ArrayList<>();
			for(Object o : obj.getArray()){
				returnValues.add(o.toString());
			}
			return returnValues;
		}
		return new ArrayList<>();
	}

}