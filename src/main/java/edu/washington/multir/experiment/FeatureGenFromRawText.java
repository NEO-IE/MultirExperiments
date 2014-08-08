//sg
package edu.washington.multir.experiment;

import java.io.IOException;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.washington.multir.preprocess.CorpusPreprocessing;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.DefaultSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.NERArgumentIdentification;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.corpus.SentDependencyInformation;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.featuregeneration.DefaultFeatureGenerator;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;

public class FeatureGenFromRawText {
	public static void getFeatures(String sentence, String docName) {
		try {
			Annotation doc = CorpusPreprocessing.getTestDocumentFromRawString(sentence, docName);
			//Do argument identification and generate features
			//do argument identification
			ArgumentIdentification ai = NERArgumentIdentification.getInstance();
			SententialInstanceGeneration sig = DefaultSententialInstanceGeneration.getInstance();
			FeatureGenerator fg = new DefaultFeatureGenerator();
			
			List<CoreMap> docSentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
			int count =0;
			for(CoreMap sent: docSentences){
				System.out.println("Sentence " + count);
				List<Argument> arguments = ai.identifyArguments(doc, sent);
				System.out.println("Arguments");
				for(Argument arg : arguments){
					System.out.println(arg.getArgName());
				}
				List<CoreLabel> tokens = sent.get(CoreAnnotations.TokensAnnotation.class);
				System.out.println("Token size = " + tokens.size());
				System.out.println("TOKENS");
				for(CoreLabel t: tokens){
					System.out.print(t + " ");
				}
				
				List<Pair<Argument,Argument>> sententialInstances = sig.generateSententialInstances(arguments, sent);
				for(Pair<Argument,Argument> argPair : sententialInstances){
					Argument arg1 = argPair.first;
					Argument arg2 = argPair.second;
					String arg1ID = null;
					String arg2ID = null;
					if(arg1 instanceof KBArgument){
						arg1ID = ((KBArgument)arg1).getKbId();
					}
					if(arg2 instanceof KBArgument){
						arg2ID = ((KBArgument)arg2).getKbId();
					}
					List<String> features =fg.generateFeatures(arg1.getStartOffset(),
											arg1.getEndOffset(),
											arg2.getStartOffset(),
											arg2.getEndOffset(), 
											arg1ID,arg2ID,
											sent, doc);
					System.out.print(arg1.getArgName() + "\t" + arg2.getArgName());
					for(String feature: features){
						System.out.print("\t" + feature);
					}
					System.out.println();
				}
				count++;
				
			}

		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) throws IOException, InterruptedException {
		String sent = "A brand new and bigger Air China was launched in Beijing on Monday .";
		String docName = "doc";
		Annotation doc = CorpusPreprocessing.getTestDocumentFromRawString(sent, docName);
		List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
		System.out.println(sentences.get(0).keySet());
		for(CoreMap sentence : sentences) { //
			System.out.println(sentence);
			System.out.println(sentence.get(SentDependencyInformation.DependencyAnnotation.class));
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
		        // this is the text of the token
		        String word = token.get(TextAnnotation.class);
		        // this is the POS tag of the token
		        String pos = token.get(PartOfSpeechAnnotation.class);
		        // this is the NER label of the token
		        String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
		     
		        Integer startOffset = token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
		        Integer endOffset = token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
		        
		        System.out.println(word + " POS " + pos + " NER " + ne + " " + startOffset + ":" + endOffset);
		      }
		}
		
		System.out.println(doc.keySet());
	}
	
}