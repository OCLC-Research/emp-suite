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
public class Tagger extends NerBase {

	private BufferedReader reader = null;

	protected static Logger logger = Logger.getLogger("org.oclc.gateman.GatemanApplication");;

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


	public Vector<LinkedVector> getData() { return data; }

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
}

