package org.oclc.gateman.ner;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.logging.Logger;

import java.net.URL;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

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
public class Formatter {

	private Configuration templateConfiguration = null;

	private String conll_template = "ner.conll";
	private String list_template = "ner.list";
	private String html_template = "ner.html";
	private String xml_template = "ner.xml";

	protected static Logger logger = Logger.getLogger("org.oclc.gateman.GatemanApplication");;

	/**
	 * Stub Constructor Documentation
	 */
	private Formatter() { }
	public static Formatter getInstance(Configuration config) throws IllegalArgumentException {
		Formatter formatter = new Formatter();
		if ( null == config ) {
			throw new IllegalArgumentException("NULL configuration in Formatter.");
		}
		formatter.templateConfiguration = config;
		return formatter;
	}


	/* Format the results as CONLL
	 */
	public String conll(Vector<LinkedVector> data) throws TemplateException, IOException {
		Map template_model = new HashMap();
		List nodes = extract_nodes(data);
		template_model.put("nodes", nodes);

		StringWriter buf = new StringWriter();
		Template template = templateConfiguration.getTemplate(conll_template);
		template.process(template_model, buf);
		buf.flush();
		return buf.toString();
	}
 
	/* Format the results as HTML
	 */
	public String html(Vector<LinkedVector> data, String link) throws TemplateException, IOException {
		Map template_model = new HashMap();
		if ( null != link && ! link.equals("") ) {
			template_model.put("link", link);
		}
		List nodes = extract_nodes(data);
		template_model.put("nodes", nodes);

		StringWriter buf = new StringWriter();
		Template template = templateConfiguration.getTemplate(html_template);
		template.process(template_model, buf);
		buf.flush();
		return buf.toString();
	}
 
	/* Format the results as XML
	 */
	public String xml(Vector<LinkedVector> data) throws TemplateException, IOException {
		Map template_model = new HashMap();
		List nodes = extract_nodes(data);
		template_model.put("nodes", nodes);

		StringWriter buf = new StringWriter();
		Template template = templateConfiguration.getTemplate(xml_template);
		template.process(template_model, buf);
		buf.flush();
		return buf.toString();
	}
 
	/* Format the results as CONLL
	 */
	public String list(Vector<LinkedVector> data) throws TemplateException, IOException {
		Map template_model = new HashMap();
		List nodes = extract_nodes(data);
		template_model.put("nodes", nodes);

		StringWriter buf = new StringWriter();
		Template template = templateConfiguration.getTemplate(list_template);
		template.process(template_model, buf);
		buf.flush();
		return buf.toString();
	}

	/*
	 * extract nodes from the Vector created during tagging
	 */
	private List extract_nodes(Vector<LinkedVector> data) {
		List nodes = new ArrayList();

		for (int i = 0; i < data.size(); i++) {
			LinkedVector vector = data.elementAt(i); // each LinkedVector in data is a sentence
			boolean open = false;
			String[] predictions = new String[vector.size()];
			String[] words = new String[vector.size()];
			for (int j = 0; j < vector.size(); j++) {
				predictions[j] = bilou2bio(((NEWord)vector.get(j)).neTypeLevel2);
				words[j] = ((NEWord)vector.get(j)).form;
				//System.err.println("(P/" + predictions[j] + ") (W/" + words[j] + ")");
			}

			StringBuilder accum = new StringBuilder();
			String state = "normal";
			Map node = new HashMap();
			node.put("type", "text");
			for (int j = 0; j < vector.size(); j++) {
				if ( ! isPunctuation(words[j].charAt(0)) ) {
					// if it DOES NOT start with punctuation, prefix a space
					accum.append(" ");
				}
				if ( state.equals("normal") ) {
					if ( predictions[j].equals("O") ) {
						//accum.append(" ").append(words[j]);
						accum.append(words[j]);
					}
					if (     predictions[j].startsWith("B-")
						|| ( j > 0 && predictions[j].startsWith("I-") && (! predictions[j-1].endsWith(predictions[j].substring(2)) ) ) ){

						if ( accum.length() > 0 ) { 
							node = node_transition(node, accum.toString(), nodes, predictions[j].substring(2).toLowerCase()); 
							accum = new StringBuilder();
						}
						else {
							node = new HashMap();
							node.put("type", predictions[j].substring(2).toLowerCase());
						}
						accum.append(words[j]);
						state = "tag";
					}
				}
				else if ( state.equals("tag") ) {
					if ( predictions[j].startsWith("I-") ) {
						// continue with existing tag
						accum.append(words[j]);
					}
					if ( predictions[j].startsWith("B-") ) {
						// start a new tag
						node = node_transition(node, accum.toString(), nodes, predictions[j].substring(2).toLowerCase()); 
						accum = new StringBuilder();
						accum.append(words[j]);
					}
					if ( predictions[j].equals("O") ) {
						node = node_transition(node, accum.toString(), nodes, "text");

						if ( isPunctuation(words[j].charAt(0)) ) {
							// if it DOES NOT start with punctuation, prefix a space
							//accum.append(" ");
							accum = new StringBuilder();
						}
						else {
							accum = new StringBuilder();
						}
						accum.append(words[j]);
						state = "normal";
					}
				}
			}
			// finalize the last node
			node.put("content", accum.toString());
			nodes.add(node);
		}
		return nodes;
	}

 
	private Map node_transition(Map node, String content, List nodes, String type) {
		node.put("content", content); // put content in current node
		nodes.add(node);              // add current node to list
		node = new HashMap();         // create a new node for new state
		node.put("type", type);       // set type for new node
		return node;                  // return the new node
	}

	private boolean isPunctuation(char c) {
		switch (Character.getType(c)) {
			case Character.CONNECTOR_PUNCTUATION: 
			case Character.DASH_PUNCTUATION: 
			case Character.START_PUNCTUATION: 
			case Character.END_PUNCTUATION: 
			case Character.INITIAL_QUOTE_PUNCTUATION: 
			case Character.FINAL_QUOTE_PUNCTUATION: 
			case Character.OTHER_PUNCTUATION: 
				return true;
		}
		return false;
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



/*
				if (predictions[j].startsWith("B-")
					|| (j > 0 && predictions[j].startsWith("I-")
					&& (! predictions[j-1].endsWith(predictions[j].substring(2))))){

						if ( null != node ) { nodes.add(node); }
						node = new HashMap();
						node.put("type", predictions[j].substring(2).toLowerCase());
						node.put("content", words[j]);

						//accum.append("<span class=\"" + predictions[j].substring(2).toLowerCase() + "\">");
						open = true;
				}
				accum.append(words[j]);
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
					else if (predictions[j+1].indexOf('-') > -1 && ( !predictions[j].endsWith(predictions[j+1].substring(2)) ) ) {
						close=true;
					}

					if (close) {
						node.put("content", accum.toString());
						nodes.add(node);
						node = new HashMap(); node.put("type", "text");
						//accum.append("</span> ");
						open = false;
					}
					else {
						accum.append(" ");
					}
				}
				else {
					accum.append(" ");
				}
*/
 
}

