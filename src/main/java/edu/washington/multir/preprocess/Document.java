package edu.washington.multir.preprocess;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

public class Document {

		public String docName;
		public String docText;
		public String stripHTMLdocText;
		
		public Document(){
			docName = null;
			docText = null;
		}	
		public Document(String docName){
			this.docName = docName;
		}
		
		public void stripHtmlDocText() throws IOException{
//			fillDocText();
//			stripHTMLdocText = ArticleExtractor.INSTANCE.getText(docText);
		}
		
		public void fillDocText() throws IOException{
			
			FileInputStream fis = new FileInputStream(new File(docName));	
			docText = 	IOUtils.toString(fis, "utf-8");
			fis.close();
		}
	
}
