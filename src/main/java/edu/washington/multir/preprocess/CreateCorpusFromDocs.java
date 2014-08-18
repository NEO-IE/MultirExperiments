//sg
package edu.washington.multir.preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;

import scala.actors.threadpool.Arrays;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.WordToSentenceProcessor;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.preprocess.marker.CountryMarker;
import edu.washington.multir.preprocess.marker.Marking;
import edu.washington.multir.preprocess.marker.MarkingUtils;
import edu.washington.multir.preprocess.marker.NumberMarker;
import edu.washington.multirframework.corpus.CorpusInformationSpecification.SentDocNameInformation.SentDocName;
import edu.washington.multirframework.corpus.SentDependencyInformation;
import edu.washington.multirframework.corpus.SentOffsetInformation.SentStartOffset;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetBeginAnnotation;
import edu.washington.multirframework.corpus.TokenOffsetInformation.SentenceRelativeCharacterOffsetEndAnnotation;

public class CreateCorpusFromDocs {
	private static String options = "invertible=true,ptb3Escaping=true";
	private static Pattern ldcPattern = Pattern.compile("<DOCID>\\s+.+LDC");
	private static Pattern xmlParagraphPattern = Pattern
			.compile("<P>((?:[\\s\\S](?!<P>))+)</P>");
	final int NANO = 1000000000;
	private static LexedTokenFactory<CoreLabel> ltf = new CoreLabelTokenFactory(
			true);
	private static WordToSentenceProcessor<CoreLabel> sen = new WordToSentenceProcessor<CoreLabel>();
	private static Properties props = new Properties();
	private static TreebankLanguagePack tlp = new PennTreebankLanguagePack();
	private static GrammaticalStructureFactory gsf = tlp
			.grammaticalStructureFactory();
	private static boolean initializedParser = false;
	private static String BLLIP_PARSER_PATH = "bllip-parser-master";
	private static StanfordCoreNLP pipeline; // the annotator pipeline
	private static String COUNTRIES_FILE =  "meta/country_freebase_mapping";
	
	/*
	 * The markers are our entity linkers for this sample exercise
	 */
	private CountryMarker cnMarker;
	private NumberMarker numMarker;
	
/**
 * This is done to avoid loading the pipeline for each doc, which takes up a lot of time
 * @throws IOException 
 */
	public CreateCorpusFromDocs() throws IOException {
		Properties props = new Properties();
		cnMarker = new CountryMarker(COUNTRIES_FILE);
		props.put("annotators", "tokenize,ssplit,pos,parse,lemma,ner");
		props.put("sutime.binders", "0");
		pipeline = new StanfordCoreNLP(props, false);
		numMarker = new NumberMarker();
	}

	Annotation createTestString(String documentString, String docName)
			throws IOException, InterruptedException {

		assert (pipeline != null);
		List<String> paragraphs = CorpusPreprocessing
				.cleanDocument(documentString);
		StringBuilder docTextBuilder = new StringBuilder();
		for (String par : paragraphs) {
			docTextBuilder.append(par);
			docTextBuilder.append("\n");
		}
		String docText = docTextBuilder.toString().trim();

		File cjInputFile = File.createTempFile(docName.replaceAll("/", ""),
				"cjinput");
		File cjOutputFile = File.createTempFile(docName.replaceAll("/", ""),
				"cjoutput");
		cjOutputFile.deleteOnExit();
		cjInputFile.deleteOnExit();

		BufferedWriter bw = new BufferedWriter(new FileWriter(cjInputFile));

		Annotation doc = new Annotation(docText);
		// get pos and ner information from stanford processing
		//Check if its really this that's taking time
		System.out.println("Starting annotation using coreNLP");
		long startTime = System.nanoTime();
		pipeline.annotate(doc);
		long endTime = System.nanoTime();
		System.out.println("SFU CoreNLP took : " + (endTime - startTime) / NANO + " seconds");
		for (CoreMap sentence : doc
				.get(CoreAnnotations.SentencesAnnotation.class)) {
			StringBuilder tokenStringBuilder = new StringBuilder();
			for (CoreLabel token : sentence
					.get(CoreAnnotations.TokensAnnotation.class)) {
				Integer sentStart = sentence
						.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
				Integer tokenStart = token
						.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
				Integer tokenEnd = token
						.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
				token.set(SentenceRelativeCharacterOffsetBeginAnnotation.class,
						tokenStart - sentStart);
				token.set(SentenceRelativeCharacterOffsetEndAnnotation.class,
						tokenEnd - sentStart);
				tokenStringBuilder.append(token.value());
				tokenStringBuilder.append(" ");
			}
			sentence.set(SentStartOffset.class, sentence
					.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class));
			String cjPreprocessedString = CorpusPreprocessing
					.cjPreprocessSentence(tokenStringBuilder.toString().trim());
			bw.write(cjPreprocessedString + "\n");
		}

		bw.close();

		// run charniak johnson parser
		File parserDirectory = new File(BLLIP_PARSER_PATH);
		ProcessBuilder pb = new ProcessBuilder();
		List<String> commandArguments = new ArrayList<String>();

		pb = new ProcessBuilder();
		commandArguments = new ArrayList<>();
		commandArguments.add("./parse.sh");
		commandArguments.add("-T50");
		commandArguments.add("-K");
		commandArguments.add("-S");
		pb.command(commandArguments);
		pb.directory(parserDirectory);
		pb.redirectInput(cjInputFile);
		pb.redirectOutput(cjOutputFile);
		pb.redirectError(new File("test.err"));
		Process p = pb.start();
		p.waitFor();

		List<CoreMap> sentences = doc
				.get(CoreAnnotations.SentencesAnnotation.class);
		// read cj parser output and run stanford dependency parse
		BufferedReader in = new BufferedReader(new FileReader(cjOutputFile));
		String nextLine;
		int index = 0;
		while ((nextLine = in.readLine()) != null) {
			// initialize custom Dependency Parse Structure
			List<Triple<Integer, String, Integer>> dependencyInformation = new ArrayList<>();

			// put parse information in a tree and get dependency parses
			Tree parse = Tree.valueOf(nextLine.replaceAll("\\|", " "));
			GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
			Collection<TypedDependency> tdl = null;
			try {
				tdl = gs.allTypedDependencies(); // gs.typedDependenciesCCprocessed();
			} catch (NullPointerException e) {
				// there has to be a bug in
				// EnglishGrammaticalStructure.collapseFlatMWP
				tdl = new ArrayList<TypedDependency>();
			}

			// convert dependency information into custom annotation
			List<TypedDependency> l = new ArrayList<TypedDependency>();
			l.addAll(tdl);
			for (int i = 0; i < tdl.size(); i++) {
				TypedDependency td = l.get(i);
				String name = td.reln().getShortName();
				if (td.reln().getSpecific() != null)
					name += "-" + td.reln().getSpecific();
				Integer governor = td.gov().index();
				String type = name;
				Integer child = td.dep().index();
				// if(!name.equals("root")){
				// type = type.replace("-", "_");
				// Triple<Integer,String,Integer> t = new
				// Triple<>(governor,type,child);
				// dependencyInformation.add(t);
				// }
				Triple<Integer, String, Integer> t = new Triple<>(governor,
						type, child);
				dependencyInformation.add(t);

			}
			// set annotation on sentence
			CoreMap sentence = sentences.get(index);
			sentence.set(SentDependencyInformation.DependencyAnnotation.class,
					dependencyInformation);

			index++;
		}
		in.close();
		doc.set(SentDocName.class, docName);
		return doc;
	}



	public static void main(String args[]) throws IOException,
			InterruptedException {
		CreateCorpusFromDocs cc = new CreateCorpusFromDocs();
		boolean debug = true;
		while(debug) {
			debug = false;
		String corpusPath = "/mnt/a99/d0/aman/pruned-nw";
		String outputFile = "data/derbyFlatFile3";
		cc.preprocessCorpus(corpusPath, outputFile);
		}
	}
	
	/**
	 * This function is used to process the html corpus
	 * @param htmlText
	 * @return
	 */
	public String html2text(String htmlText) {
	    return Jsoup.parse(htmlText).text();
	}
	
	/**
	 * Preprocess Corpus takes path to a directory where the files reside, iterates over them and 
	 * attaches meta data to each of the docs. The schema in which we need each of the sentences is 
	 * as shown in meta/schema.txt
	 * @param path : Path to the input corpus
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	
	void preprocessCorpus(String path, String outputFile) throws IOException, InterruptedException {
		File inputFiles[] = new File(path).listFiles();
		if(inputFiles == null) {
			return;
		}
		File outFile = new File(outputFile);
		PrintWriter bw = new PrintWriter(new FileOutputStream(outFile));
		String SEP = "\t";
			
		
		boolean debug = true;
		while(debug) {
			debug = false;
			int sentId = 1;
			for(File inputFile : inputFiles) {
				String docName = inputFile.getName();
				FileInputStream fisTargetFile = new FileInputStream(inputFile);
				System.out.println("Processing " + inputFile.getAbsolutePath());
				String targetFileStr = IOUtils.toString(fisTargetFile, "UTF-8");
				targetFileStr = html2text(targetFileStr);
				long startTime = System.nanoTime();
				Annotation doc =createTestString(targetFileStr, docName);
				long endTime = System.nanoTime();
				System.out.println("Processing Finished, Time: " + (endTime - startTime) / NANO);
				List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
			
				for(CoreMap sentence : sentences) {
					//System.out.println("\t sentence : " + sentId + " : " + sentence);
					bw.write(createDerbyRow(sentId++, docName, sentence).stringSep(SEP) + "\n");
					
				}
			}

		}
		System.out.println("Results Written to " + outputFile);
		bw.close();
		
	}

	/**
	 * Responsible for creating a set of corpus rows given a doc
	 * @param doc
	 * @return
	 */
	ArrayList<CorpusRow> createDerbyRowSet(Annotation doc, String docName) {
		ArrayList<CorpusRow> rowSet = new ArrayList<>();
		int sentId = 0;
		List<CoreMap> sentences = doc
				.get(CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			rowSet.add(createDerbyRow(sentId++, docName, sentence));
		}
		return rowSet;
	}


	/**
	 * Takes an annotated documented and iterates over the various annotations
	 * to create a row for derby
	 * 
	 * @param sentence
	 *            : The annotated sentence
	 * @return
	 */	
	CorpusRow createDerbyRow(Integer sentId, String docName, CoreMap sentence) {
		StringBuilder depInfo = new StringBuilder();
		
		String targetedChunks[] = {"NP", "VP", "PP"};
		HashSet<String> targetChunk = new HashSet<String>(Arrays.asList(targetedChunks));
		StringBuilder offSetInfo = new StringBuilder();
		StringBuilder nerInfo = new StringBuilder();
		StringBuilder posTagInfo = new StringBuilder();
		StringBuilder tokenInformation = new StringBuilder();
		Integer sentBeginOffSet = sentence.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
		Integer sentEndOffSet = sentence.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
		Tree t = sentence.get(TreeAnnotation.class);
		List<Triple<Integer, String, Integer>> depInfoTripleList = sentence
				.get(SentDependencyInformation.DependencyAnnotation.class);
	
		for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
			// this is the text of the token
			String word = token.get(TextAnnotation.class);
			// this is the POS tag of the token
			String pos = token.get(PartOfSpeechAnnotation.class);
			// this is the NER label of the token	
			String ne = token
					.get(CoreAnnotations.NamedEntityTagAnnotation.class);
		
			Integer startOffset = token
					.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
			Integer endOffset = token
					.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
			posTagInfo.append(pos + " ");
			nerInfo.append(ne + " ");
			tokenInformation.append(word + " ");
			offSetInfo.append(startOffset + ":" + endOffset + " ");
			// System.out.println(word + " POS " + pos + " NER " + ne + " " +
			// startOffset + ":" + endOffset);
		}

		for (Triple<Integer, String, Integer> deps : depInfoTripleList) {
			depInfo.append(deps.first + " " + deps.second + " " + deps.third
					+ "|");
		}
		/*
		 * Get typing information
		 * TODO: See if passing 	
		 */
		ArrayList<ArrayList<Marking>> markingsList = new ArrayList<ArrayList<Marking>>();
		markingsList.add(cnMarker.mark(sentence.toString()));
		markingsList.add(numMarker.mark(sentence.toString()));
		ArrayList<String> linkTypeInfo = MarkingUtils.getMarkingStrings(MarkingUtils.mergeMarkings(markingsList));
		/*
		 * Get chunking information
		 */
		StringBuilder chunkBuilder = new StringBuilder();
		
		String currPhrase = "", prevPharse = "X";
		boolean newPhrase = false;
		for (Tree child: t) {
			if(child.isPhrasal() && targetChunk.contains(child.value())) { //if this is a phrase we are interested in
				currPhrase = child.value();
				if(!currPhrase.equals(prevPharse)) {
					prevPharse = currPhrase;
					newPhrase = true;
				}
			} else if(child.isLeaf()) {
				if(newPhrase) {
					chunkBuilder.append("B-" + currPhrase + " ");
					newPhrase = false;
				} else {
					chunkBuilder.append("I-" + currPhrase + " ");
				}
			}
		}

	
		return new CorpusRow(sentId, docName, tokenInformation.toString()
				.trim(), sentence.toString(), sentBeginOffSet + " " + sentEndOffSet,
				depInfo.toString().trim(), linkTypeInfo.get(MarkingUtils.LINKOFFSET), linkTypeInfo.get(MarkingUtils.TYPEOFFSET), nerInfo.toString().trim(),
				offSetInfo.toString().trim(), posTagInfo.toString().trim(), chunkBuilder.toString());
	}
}
