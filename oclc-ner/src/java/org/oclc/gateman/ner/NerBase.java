package org.oclc.gateman.ner;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;

import java.lang.IllegalArgumentException;

import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Iterator;

import java.util.zip.GZIPInputStream;

import java.net.URL;

//import IO.ReaderUtil;
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
public class NerBase {

	protected static final Vector<String> dicts = new Vector<String>();
	static {
		dicts.addElement("WikiArtWork.lst");
		dicts.addElement("WikiArtWorkRedirects.lst");
		dicts.addElement("WikiCompetitionsBattlesEvents.lst");
		dicts.addElement("WikiCompetitionsBattlesEventsRedirects.lst");
		dicts.addElement("WikiFilms.lst");
		dicts.addElement("WikiFilmsRedirects.lst");
		dicts.addElement("WikiLocations.lst");
		dicts.addElement("WikiLocationsRedirects.lst");
		dicts.addElement("WikiManMadeObjectNames.lst");
		dicts.addElement("WikiManMadeObjectNamesRedirects.lst");
		dicts.addElement("WikiOrganizations.lst");
		dicts.addElement("WikiOrganizationsRedirects.lst");
		dicts.addElement("WikiPeople.lst");
		dicts.addElement("WikiPeopleRedirects.lst");
		dicts.addElement("WikiSongs.lst");
		dicts.addElement("WikiSongsRedirects.lst");
		dicts.addElement("cardinalNumber.txt");
		dicts.addElement("currencyFinal.txt");
		dicts.addElement("known_corporations.lst");
		dicts.addElement("known_country.lst");
		dicts.addElement("known_jobs.lst");
		dicts.addElement("known_name.lst");
		dicts.addElement("known_names.big.lst");
		dicts.addElement("known_nationalities.lst");
		dicts.addElement("known_place.lst");
		dicts.addElement("known_state.lst");
		dicts.addElement("known_title.lst");
		dicts.addElement("measurments.txt");
		dicts.addElement("ordinalNumber.txt");
		dicts.addElement("temporal_words.txt");
	}

	protected BufferedReader configReader = null;

	protected NETaggerLevel1 tagger1;
	protected NETaggerLevel2 tagger2;

	public static final String TRAINER_KEY = "uiuc.ner.trainer";
	public static final String FORCE_SENTENCE_KEY = "uiuc.ner.parameter.forceSentenceOnLineBreak";

	protected static HashMap<String,String> defaultConfiguration = new HashMap<String,String>();
	static {
		defaultConfiguration.put("configLocation", "/Config/allFeaturesBigTrainingSet.config");
		defaultConfiguration.put("configLocationType", "resource");
		defaultConfiguration.put("brownClusters", "/Data/BrownHierarchicalWordClusters/brownBllipClusters");
		defaultConfiguration.put("brownClustersType", "resource");
		defaultConfiguration.put("gazzetteerDirectory", "/Data/KnownLists");
		defaultConfiguration.put("gazzetteerDirectoryType", "resource");
		defaultConfiguration.put("shapeClassifier", "/Data/Models/shapeClassifier");
		defaultConfiguration.put("shapeClassifierType", "resource");
		defaultConfiguration.put("modelType", "resource");
	}

	protected boolean isConfigurationComplete(Map<String,String> config) {
		// Make sure all the keys in the default config are present in the supplied config
		Iterator<String> i = defaultConfiguration.keySet().iterator();
		while ( i.hasNext() ) {
			String k = i.next();
			if ( ! config.containsKey(k) ) {
				System.err.println("Missing key: " + k);
				return false;
			}
		}

		// Make sure we can get the configuration file
		String location = config.get("configLocation");
		String type = config.get("configLocationType");
		if ( type.equals("resource") ) {
			// System.err.println(location);
			configReader = new BufferedReader(getResource(location));
		}
		else if ( type.equals("file") ) {
			try { configReader = new BufferedReader( new FileReader(location)); }
			catch (FileNotFoundException e) { return false; }
		}

		return true;
	}

	/* Configure the UIUC NER library
	 */
	protected void configure(Map<String,String> config) {
		if ( null == configReader ) {
			return;
		}

		try {
			Parameters.taggingScheme = configReader.readLine();
			Parameters.pathToModelFile = configReader.readLine();
			Parameters.tokenizationScheme = configReader.readLine();
			String line = configReader.readLine();
	
			StringTokenizer st = new StringTokenizer(line,"\t ");
			st.nextToken();
			Parameters.trainingRounds = Integer.parseInt(st.nextToken());
			Parameters.featuresToUse = new Hashtable<String,Boolean>();
			line = configReader.readLine();
			while (line != null){
				st = new StringTokenizer(line,"\t");
				String feature = st.nextToken();
				if (st.nextToken().equals("1")) {
					Parameters.featuresToUse.put(feature,true);
				}
				line = configReader.readLine();
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("Error reading NER configuration file.");
			System.exit(1);
		}

		// Default to true
		Parameters.forceNewSentenceOnLineBreaks = true;
		if ( config.containsKey(FORCE_SENTENCE_KEY) ) {
			Parameters.forceNewSentenceOnLineBreaks = Boolean.parseBoolean(config.get(FORCE_SENTENCE_KEY));
		}
//		if ( config.containsKey("uiuc.ner.parameter.forceSentenceOnLineBreak") ) {
//			Parameters.forceNewSentenceOnLineBreaks = Boolean.parseBoolean(config.get("uiuc.ner.parameter.forceSentenceOnLineBreak"));
//		}

		String cluster = config.get("brownClusters");
		String type = config.get("brownClustersType");
		if ( type.equals("resource") ) {
			BrownClusters.init(getResource(cluster));
		}
		else if ( type.equals("file") ) {
			BrownClusters.init(new File(cluster));
		}
		type = "";

		if (Parameters.featuresToUse.containsKey("GazetteersFeatures")) {
			type = config.get("gazzetteerDirectoryType");
			if ( type.equals("resource") ) {
				Gazzetteers.init(config.get("gazzetteerDirectory"), dicts);
			}
			else if ( type.equals("file") ) {
				Gazzetteers.init(config.get("gazzetteerDirectory"));
			}
		}
		type = "";

		if (Parameters.featuresToUse.containsKey("NEShapeTaggerFeatures")) {
			String shape = config.get("shapeClassifier");
			type = config.get("shapeClassifierType");
			if ( type.equals("resource") ) {
				ShapeClassifierManager.load(getResourceAsStream(shape));
			}
			else if ( type.equals("file") ) {
				ShapeClassifierManager.load(new File(shape));
			}
		}

		// If TRAINER_KEY not in config OR TRAINER_KEY = false, then set up for tagging
		if ( ! config.containsKey(TRAINER_KEY) || ! Boolean.parseBoolean(config.get(TRAINER_KEY)) ) {
			String modelLevel1 = Parameters.pathToModelFile + ".level1";
			String modelLevel2 = Parameters.pathToModelFile + ".level2";
			tagger1 = new NETaggerLevel1();
			tagger2 = new NETaggerLevel2();

			// taggers can be on the file system or as a resource
			if ( config.containsKey("modelType") && config.get("modelType").equals("resource") ) {
				try {
					tagger1 = (NETaggerLevel1)Classifier.binaryRead(new ObjectInputStream(getResourceAsGZIPStream("/" + modelLevel1)), "NETaggerLevel1");
					tagger2 = (NETaggerLevel2)Classifier.binaryRead(new ObjectInputStream(getResourceAsGZIPStream("/" + modelLevel2)), "NETaggerLevel2");
				}
				catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			else {
				// If modelType not supplied, assume file
				tagger1 = (NETaggerLevel1)Classifier.binaryRead(modelLevel1, "NETaggerLevel1");
				tagger2 = (NETaggerLevel2)Classifier.binaryRead(modelLevel2, "NETaggerLevel2");
			}
		}
	}

	protected GZIPInputStream getResourceAsGZIPStream(String resource) throws IOException {
		return new GZIPInputStream(getResourceAsStream(resource));
	}

	protected InputStreamReader getResource(String resource) {
		return new InputStreamReader(getResourceAsStream(resource));
	}

	protected InputStream getResourceAsStream(String resource) {
		return getClass().getResourceAsStream(resource);
	}

}

