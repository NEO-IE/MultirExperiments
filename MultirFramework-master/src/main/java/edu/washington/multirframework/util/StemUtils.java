package edu.washington.multirframework.util;

import org.tartarus.snowball.SnowballStemmer;



public class StemUtils {
	public static SnowballStemmer stemmer = null;
	
	static{
		@SuppressWarnings("rawtypes")
		Class stemClass = null;
		try {
			stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			stemmer = (SnowballStemmer) stemClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public static String getStemWord(String word){
		 stemmer.setCurrent(word);
		 stemmer.stem();
		 return stemmer.getCurrent();
	}
}
