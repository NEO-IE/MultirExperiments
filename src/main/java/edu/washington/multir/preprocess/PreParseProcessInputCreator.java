package edu.washington.multir.preprocess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multir.preprocess.marker.CountryMarker;
import edu.washington.multir.preprocess.marker.NumberMarker;

/**
 * 
 * This class is responsible for pre-parse processing of Input.
 * 
 * @author : Ashish
 */
public class PreParseProcessInputCreator {

	public String directory; // directory to be processed.
	private static StanfordCoreNLP pipeline; // the annotator pipeline

	public PreParseProcessInputCreator(String directory) {
		this.directory = directory;
		Properties props = new Properties();
		props.put("annotators", "tokenize,ssplit");
		props.put("sutime.binders", "0");
		pipeline = new StanfordCoreNLP(props, false);

	}

	public Iterator<Document> getDocStringDir() throws IOException {
		ArrayList<Document> docString = new ArrayList<Document>();

		File baseD = new File(directory);
		if (baseD.isDirectory()) {
			File files[] = baseD.listFiles();
			if (files != null) {
				for (File file : files) {
					// process each document.
					Document doc = new Document(file.toString());
					doc.fillDocText();
					docString.add(doc);
				}
			}
		}
		return docString.iterator();
	}

	public void writeSentToDisk(String directory) {
		List<String> paragraphs = CorpusPreprocessing
				.cleanDocument(documentString);
		StringBuilder docTextBuilder = new StringBuilder();
		for (String par : paragraphs) {
			docTextBuilder.append(par);
			docTextBuilder.append("\n");
		}

		Annotation doc = new Annotation(docText);
		pipeline.annotate(doc);
		for (CoreMap sentence : doc
				.get(CoreAnnotations.SentencesAnnotation.class)) {
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
			}
		}
	}

	public static void main(String args[]) throws IOException {
		String directory = "/mnt/a99/d0/ashishm/workspace/test";
		PreParseProcessInputCreator pppic = new PreParseProcessInputCreator(
				directory);
		Iterator<Document> itr = pppic.getDocStringDir();
		while (itr.hasNext()) {
			Document doc = itr.next();
			System.out.println("DocName: " + doc.docName);
			System.out.println("DocString: \n" + doc.docText);
		}

	}

}
