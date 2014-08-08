//sg
package edu.washington.multir.preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.derby.tools.sysinfo;

import scala.languageFeature.postfixOps;
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
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Triple;
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

	public CreateCorpusFromDocs() {
		Properties props = new Properties();
		props.put("annotators", "tokenize,ssplit,pos,lemma,ner");
		props.put("sutime.binders", "0");
		pipeline = new StanfordCoreNLP(props, false);
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
		pipeline.annotate(doc);

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

	/**
	 * This function takes the newly obtained document string and cleans the
	 * paragraphs. The code is lifted from CorpusPreprocessing.java TODO: One of
	 * these 2 classes should be deleted
	 * 
	 * @param documentString
	 * @param docName
	 * @throws IOException
	 */
	void cleanParagraphs(String documentString, String docName)
			throws IOException {
		List<String> paragraphs = CorpusPreprocessing
				.cleanDocument(documentString);
		StringBuilder docTextBuilder = new StringBuilder();
		for (String par : paragraphs) {
			docTextBuilder.append(par);
			docTextBuilder.append("\n");
		}
		documentString = docTextBuilder.toString().trim();
	}

	/**
	 * This method creates the temporary files needed for bllip-parser to work
	 * 
	 * @param documentString
	 * @param docName
	 * @throws IOException
	 */
	void setup(String documentString, String docName) throws IOException {

		File cjInputFile = File.createTempFile(docName, "cjinput");
		File cjOutputFile = File.createTempFile(docName, "cjoutput");
		cjOutputFile.deleteOnExit();
		cjInputFile.deleteOnExit();

		BufferedWriter bw = new BufferedWriter(new FileWriter(cjInputFile));

		Annotation doc = new Annotation(documentString);
	}

	public static void main(String args[]) throws IOException,
			InterruptedException {
		CreateCorpusFromDocs cc = new CreateCorpusFromDocs();
		String docName = "data/sgtest/testdoc1.txt";

		FileInputStream fisTargetFile = new FileInputStream(new File(docName));

		String targetFileStr = IOUtils.toString(fisTargetFile, "UTF-8");

		Annotation doc = cc.createTestString(targetFileStr, docName);
		//ArrayList<CorpusRow> rowSet = cc.createDerbyRowSet(doc, "doc1");
		System.out.println(cc.createDerbyRow(1, "sg", doc
				.get(CoreAnnotations.SentencesAnnotation.class).get(0)));
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
		StringBuilder offSetInfo = new StringBuilder();
		StringBuilder nerInfo = new StringBuilder();
		StringBuilder posTagInfo = new StringBuilder();
		StringBuilder tokenInformation = new StringBuilder();
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
			tokenInformation.append(word);
			offSetInfo.append(startOffset + ":" + endOffset + " ");
			// System.out.println(word + " POS " + pos + " NER " + ne + " " +
			// startOffset + ":" + endOffset);
		}

		for (Triple<Integer, String, Integer> deps : depInfoTripleList) {
			depInfo.append(deps.first + " " + deps.second + " " + deps.third
					+ "|");
		}
		return new CorpusRow(sentId, docName, tokenInformation.toString()
				.trim(), sentence.toString(), "",
				depInfo.toString().trim(), "", "", nerInfo.toString().trim(),
				offSetInfo.toString().trim(), posTagInfo.toString().trim(), "");
	}
}
