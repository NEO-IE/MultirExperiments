package edu.washington.multir.preprocess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * This class is responsible for pre-parse processing of Input.
 */
public class PreParseProcessInputCreator {

	public String directory; //directory to be processed.
	
	public PreParseProcessInputCreator(String directory){
		this.directory = directory;
	}
	
	public Iterator<Document> getDocStringDir() throws IOException{
		ArrayList<Document> docString = new ArrayList<Document>();
		
		File baseD = new File(directory);
		if(baseD.isDirectory()){
			File files[] = baseD.listFiles();
			if(files != null){
				for(File file: files)
				{
						//process each document.
						Document doc = new Document(file.toString());
						doc.fillDocText();
						docString.add(doc);
				}
			}
		}
		
		return docString.iterator();
	}
	
	public static void main(String args[]) throws IOException{
		String directory = "/mnt/a99/d0/ashishm/workspace/test";
		PreParseProcessInputCreator pppic = new PreParseProcessInputCreator(directory);
		Iterator<Document> itr = pppic.getDocStringDir();
		while(itr.hasNext()){
			Document doc = itr.next();
			System.out.println("DocName: "+doc.docName);
			System.out.println("DocString: \n"+doc.docText);
		}	
		
	}
	
}
