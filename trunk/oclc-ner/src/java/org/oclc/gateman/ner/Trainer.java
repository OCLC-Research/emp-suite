package org.oclc.gateman.ner;

import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.Map;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;

import LbjTagger.LearningCurve;
import LbjTagger.Parameters;
import LbjTagger.BrownClusters;
import LbjTagger.Gazzetteers;
import LbjTagger.ShapeClassifierManager;

/** 
 * Bridge class between Gateman service and the UIUC NER Tagging App
 * @author Devon Smith
 * @date 2010-01-07
 */
public class Trainer extends NerBase {
	private Trainer() { }

	public static Trainer getInstance() throws IllegalArgumentException {
		return getInstance(defaultConfiguration);
	}

	public static Trainer getInstance(Map<String, String> config) throws IllegalArgumentException {
		Trainer trainer = new Trainer();

		if ( null == config ) {
			throw new IllegalArgumentException("NULL configuration in Trainer.");
		}

		if ( ! trainer.isConfigurationComplete(config) ) {
			throw new IllegalArgumentException("Incomplete configuration in Trainer.");
		}
		trainer.configure(config);
		return trainer;
	}

	public void train(String trainingFilename, String testFilename, String filesFormat) {
		LearningCurve.getLearningCurve(trainingFilename, testFilename, filesFormat);
	}
}

