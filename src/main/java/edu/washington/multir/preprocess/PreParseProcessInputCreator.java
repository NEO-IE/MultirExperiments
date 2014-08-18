package edu.washington.multir.preprocess;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.derby.tools.sysinfo;

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

	public Iterator<Document> getDocStringDir(String directory) throws IOException {
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

	public void writeSentToDisk(String directory, String outFile) throws IOException {
		
		Iterator<Document> docs = getDocStringDir(directory);
		PrintWriter pw = new PrintWriter(new File(outFile));
		Integer sentID = 1;
			
		while(docs.hasNext()){
			Document document = docs.next();
			System.out.println("Processing " + document.docName);
			List<String> paragraphs = CorpusPreprocessing
					.cleanDocument(document.docText);
			StringBuilder docTextBuilder = new StringBuilder();
			for (String par : paragraphs) {
				docTextBuilder.append(par);
				docTextBuilder.append("\n");
			}
			String docText = docTextBuilder.toString().trim();
			//for every doc string	
			Annotation doc = new Annotation(docText);
			pipeline.annotate(doc);
			for (CoreMap sentence : doc
					.get(CoreAnnotations.SentencesAnnotation.class)) {
				
				Integer sentBeginOffSet = sentence.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
				Integer sentEndOffSet = sentence.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
				StringBuilder tokenString = new StringBuilder();
				StringBuilder offsetString = new StringBuilder();
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					
					// this is the text of the token
					String word	 = token.get(TextAnnotation.class);
					tokenString.append(word+" ");
					
/*					// this is the POS tag of the token
					String pos = token.get(PartOfSpeechAnnotation.class);
					
					// this is the NER label of the token
					String ne = token
							.get(CoreAnnotations.NamedEntityTagAnnotation.class);
*/
					Integer startOffset = token
							.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
					Integer endOffset = token
							.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
					
					offsetString.append(startOffset+":"+endOffset+" ");
					
				}
				String filename = (new File(document.docName)).getName();
				pw.write(sentID+"\t"+filename	+"\t"+tokenString.toString().trim()+
						"\t"+offsetString.toString().trim()+"\t"+sentBeginOffSet+":"+sentEndOffSet+"\t"+sentence.toString()+"\n");
				sentID ++;
			}
		}
		pw.close();
	}

	public static void main(String args[]) throws IOException {
		String directory = "/mnt/a99/d0/aman/pruned-nw/";
		PreParseProcessInputCreator pppic = new PreParseProcessInputCreator(
				directory);
//			Iterator<Document> itr = pppic.getDocStringDir(directory);
		/*while (itr.hasNext()) {
			Document doc = itr.next();
			System.out.println("DocName: " + doc.docName);
			System.out.println("DocString: \n" + doc.docText);
		}*/
		
		pppic.writeSentToDisk(directory, "/mnt/a99/d0/ashishm/ipFile");
	}

}
