package org.oclc.gateman.ner;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Map;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Hashtable;

import java.net.URL;

import LBJ2.parse.LinkedVector;
import LBJ2.classify.Classifier;
import lbj.NETaggerLevel1;
import lbj.NETaggerLevel2;
import LbjTagger.BracketFileManager;
import LbjTagger.Parameters;
import LbjTagger.BrownClusters;
import LbjTagger.Gazzetteers;
import LbjTagger.NETester;
import LbjTagger.NEWord;
import LbjTagger.ShapeClassifierManager;

/** 
 * Bridge class between Gateman service and the UIUC NER Tagging App
 * @author Devon Smith
 * @date 2009-02-16
 *
 */
public class Tagger extends NerBase {

	private BufferedReader reader = null;

	private StringBuilder prepBuf;
	private boolean prepared = false;

	private Vector<LinkedVector> data = null;

	/**
	 * Stub Constructor Documentation
	 */
	private Tagger() { }
	public static Tagger getInstance() throws IllegalArgumentException {
		return getInstance(defaultConfiguration);
	}
	public static Tagger getInstance(Map<String, String> config) throws IllegalArgumentException {
		Tagger tagger = new Tagger();
		if ( null == config ) {
			throw new IllegalArgumentException("NULL configuration in Tagger.");
		}
		if ( ! tagger.isConfigurationComplete(config) ) {
			throw new IllegalArgumentException("Incomplete configuration in Tagger.");
		}
		tagger.configure(config);
		return tagger;
	}


	/* Set the input source
	 */
	public void setSource(Reader reader) {
		this.reader = new BufferedReader(reader);
		prepBuf = new StringBuilder();
		prepared = false;
	}

	/* Prepare the text in the source to be tagged
	 */
	public void prepareText() {
		if ( null == reader ) {
			return;
		}

		try {
			String line = reader.readLine();
			while (line != null) {
				prepBuf.append(line + " \n");
				line = reader.readLine();
			}
		}
		catch (IOException ioe) {
		}
		prepBuf.append(" ");
		prepared = true;
	}

	/* Tag the text
	 */
	public void tagText() {
		if ( ! prepared ) { this.prepareText(); }
		data = BracketFileManager.parseText(prepBuf.toString());
		NETester.annotateBothLevels(data,tagger1,tagger2);
	}


	/* Format the results as HTML
	 * Find a template language?
	 */
	public String formatResultsHTML(String link) {
		StringBuilder result = new StringBuilder("<html><head>")
			.append("<style type='text/css'>th{text-align:left;}span{font-weight:bold;}")
			.append(".per{color:red;}.loc{color:blue;}.org{color:green;}.misc{color:purple;}")
			.append("table#key{float:left;font-size:small;margin-right:5pt;margin-bottom:5pt;border-right:solid thin black;border-bottom:solid thin black;")
			.append("</style></head><body>");
		if ( null != link && ! link.equals("") ) {
			result.append("<p><a href=\"").append(link).append("\">").append(link).append("</a></p>");
		}
		result.append("<hr/><div id='content'>")
				.append("<table id='key'>")
				.append("<tr><th class='per'>Person</th></tr>")
				.append("<tr><th class='loc'>Location</th></tr>")
				.append("<tr><th class='org'>Organization</th></tr>")
				.append("<tr><th class='misc'>Misc</th></tr>")
				.append("</table>");

		for (int i = 0; i < data.size(); i++) {
			LinkedVector vector = data.elementAt(i);
			StringBuilder res = new StringBuilder();
			boolean open = false;
			String[] predictions = new String[vector.size()];
			String[] words = new String[vector.size()];
			for (int j = 0; j < vector.size(); j++) {
				predictions[j] = bilou2bio(((NEWord)vector.get(j)).neTypeLevel2);
				words[j] = ((NEWord)vector.get(j)).form;
			}
			String spanClass = "error"; // If not overridden, but used, it's an error
			for (int j = 0; j < vector.size(); j++) {
				if (predictions[j].startsWith("B-")
					|| (j > 0 && predictions[j].startsWith("I-")
					&& (! predictions[j-1].endsWith(predictions[j].substring(2))))){
						res.append("<span class=\"" + predictions[j].substring(2).toLowerCase() + "\">");
						open = true;
				}
				res.append(words[j]);
				if (open) {
					boolean close = false;
					if (j == vector.size()-1) {
						close = true;
					}
					else if (predictions[j+1].startsWith("B-")) {
						close=true;
					}
					else if (predictions[j+1].equals("O")) {
						close=true;
					}
					else if (predictions[j+1].indexOf('-')>-1&&(!predictions[j].endsWith(predictions[j+1].substring(2)))) {
						close=true;
					}
					if (close) {
						res.append("</span> ");
						open = false;
					}
					else {
						res.append(" ");
					}
				}
				else {
					res.append(" ");
				}
			}
			result.append(res.toString());
		}
		return result.append("</div><hr/></body></html>").toString();
	}
 
	/* Format the results as XML
	 * Find a template language?
	 */
	public String formatResultsXML() {
		StringBuilder result = new StringBuilder("<ner:content xmlns:ner=\"uri:ns:ner\">");

		for (int i = 0; i < data.size(); i++) {
			LinkedVector vector = data.elementAt(i);
			StringBuilder res = new StringBuilder();
			boolean open = false;
			String[] predictions = new String[vector.size()];
			String[] words = new String[vector.size()];
			for (int j = 0; j < vector.size(); j++) {
				predictions[j] = bilou2bio(((NEWord)vector.get(j)).neTypeLevel2);
				words[j] = ((NEWord)vector.get(j)).form;
			}
			String tag = "ner:ERROR"; // If not overridden, but used, it's an error
			for (int j = 0; j < vector.size(); j++) {
				if (predictions[j].startsWith("B-")
					|| (j > 0 && predictions[j].startsWith("I-")
					&& (! predictions[j-1].endsWith(predictions[j].substring(2))))){
						tag = "ner:" + predictions[j].substring(2).toLowerCase(); 
						res.append("<" + tag + ">");
						open = true;
						
				}
				res.append(words[j]);
				if (open) {
					boolean close = false;
					if (j == vector.size()-1) {
						close = true;
					}
					else if (predictions[j+1].startsWith("B-")) {
						close=true;
					}
					else if (predictions[j+1].equals("O")) {
						close=true;
					}
					else if (predictions[j+1].indexOf('-')>-1&&(!predictions[j].endsWith(predictions[j+1].substring(2)))) {
						close=true;
					}
					if (close) {
						res.append("</" + tag + "> ");
						open = false;
					}
					else {
						res.append(" ");
					}
				}
				else {
					res.append(" ");
				}
			}
			result.append(res.toString());
		}
		return result.append("</ner:content>").toString();
	}
    

	/* Format the results as text/x-ner-markup
	 */
	public String formatResults() {
		StringBuilder result = new StringBuilder();

		for (int i = 0; i < data.size(); i++) {
			LinkedVector vector = data.elementAt(i);
			StringBuilder res = new StringBuilder();
			boolean open = false;
			String[] predictions = new String[vector.size()];
			String[] words = new String[vector.size()];
			for (int j = 0; j < vector.size(); j++) {
				predictions[j] = bilou2bio(((NEWord)vector.get(j)).neTypeLevel2);
				words[j] = ((NEWord)vector.get(j)).form;
			}
			for (int j = 0; j < vector.size(); j++) {
				if (predictions[j].startsWith("B-")
					|| (j > 0 && predictions[j].startsWith("I-") && (! predictions[j-1].endsWith(predictions[j].substring(2))))){

					res.append("[" + predictions[j].substring(2) + " ");
					open = true;
				}
				res.append(words[j]+ " ");
				if (open) {
					boolean close = false;
					if (j == vector.size()-1) {
						close = true;
					}
					else {
						if (predictions[j+1].startsWith("B-")) {
							close=true;
						}
						if (predictions[j+1].equals("O")) {
							close=true;
						}
						if (predictions[j+1].indexOf('-')>-1&&(!predictions[j].endsWith(predictions[j+1].substring(2)))) {
							close=true;
						}
					}
					if (close) {
						res.append(" ] ");
						open = false;
					}
				}
			}
			result.append(res.toString()).append("\n");
		}
		return result.toString();
	}

	/*
 	 * "Format" the results to present the raw data
	 */
	public String formatResultsENT() {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < data.size(); i++) {
			LinkedVector vector = data.elementAt(i);
			for (int j = 0; j < vector.size(); j++) {
				String prediction = ((NEWord)vector.get(j)).neTypeLevel2;
				if ( prediction.equals("O") ) {
					continue;
				}
				prediction = bilou2bio(prediction);

				if (prediction.startsWith("B-")) {
					result.append( prediction.substring(2) ).append("\t");
				}
				result.append( ((NEWord)vector.get(j)).form );

				if ( j < vector.size() ) {
					NEWord la = (NEWord)vector.get(j+1);
					if ( null != la ) {
						String lookahead = la.neTypeLevel2;
						lookahead = bilou2bio(lookahead);
	
						if ( null != lookahead && lookahead.startsWith("I-") ) {
							result.append(" ");
						}
						else {
							result.append("\n");
						}
					}
					else {
						result.append("\n");
					}
				}
			}
		}
		return result.toString();
	}
    
	/* ? Not sure, copied from UIUC code, used by formatResults
	 */
	private String bilou2bio(String prediction){
		if (Parameters.taggingScheme.equalsIgnoreCase(Parameters.BILOU)) {
			if (prediction.startsWith("U-")) {
				prediction="B-"+prediction.substring(2);
			}
			if (prediction.startsWith("L-")) {
				prediction="I-"+prediction.substring(2);
			}
		}
		return prediction;
	}
 
}

