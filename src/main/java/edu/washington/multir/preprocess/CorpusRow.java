//sg
package edu.washington.multir.preprocess;
/**
 * This class represents a row in the apache derby corpus
 * @author aman
 *
 */
public class CorpusRow {
	//The columns in the corpus 
	int sentId;
	String docName;
	String tokenInformation;
	String textInformation;
	String sentOffSetInformation;
	String sentdependencyInformation;
	String sentNELinkingInformation;
	String sentFreebaseInformation;
	String tokenNERInformation;
	String tokenOffsetInformation;
	String tokenPOSInformation;
	String tokenChunkInformation;
	public CorpusRow(int sentId, String docName, String tokenInformation,
			String textInformation, String sentOffSetInformation,
			String sentdependencyInformation, String sentNELinkingInformation,
			String sentFreebaseInformation, String tokenNERInformation,
			String tokenOffsetInformation, String tokenPOSInformation,
			String tokenChunkInformation) {
		super();
		this.sentId = sentId;
		this.docName = docName;
		this.tokenInformation = tokenInformation;
		this.textInformation = textInformation;
		this.sentOffSetInformation = sentOffSetInformation;
		this.sentdependencyInformation = sentdependencyInformation;
		this.sentNELinkingInformation = sentNELinkingInformation;
		this.sentFreebaseInformation = sentFreebaseInformation;
		this.tokenNERInformation = tokenNERInformation;
		this.tokenOffsetInformation = tokenOffsetInformation;
		this.tokenPOSInformation = tokenPOSInformation;
		this.tokenChunkInformation = tokenChunkInformation;
	}
	
	/**
	 * 
	 * @param SEP separator
	 * @return corpus row separated by SEP
	 */
	public String stringSep(String SEP) {
		return  sentId + SEP + docName
				+ SEP + tokenInformation
				+ SEP + textInformation
				+ SEP + sentOffSetInformation
				+ SEP + sentdependencyInformation
				+ SEP + sentNELinkingInformation
				+ SEP + sentFreebaseInformation
				+ SEP + tokenNERInformation
				+ SEP + tokenOffsetInformation
				+ SEP + tokenPOSInformation
				+ SEP + tokenChunkInformation ;
	}
}
