package org.oclc.gateman;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Map;
import java.util.Properties;

import java.util.logging.Logger;
import java.util.logging.LogManager;

import org.oclc.gateman.ner.Trainer;
import org.oclc.gateman.logging.LogFormatter;

/**
 * Base resource class that supports common behaviours or attributes shared by
 * all resources.
 * 
 */
public class CmdLineTrainer {

	private static Logger logger;

	private static File trainingPropFile = new File("training.prop");

	private static String usage = "Usage: org.oclc.gateman.CmdLineTrainer file-format config-file training-file testing-file\n\tfile-format: -c or -r\n\tforce-sentence: true to make newlines force a new sentence";
	public static void main(String[] args) throws IOException, FileNotFoundException {
		LogManager lm = LogManager.getLogManager();
		lm.readConfiguration(ClassLoader.getSystemClassLoader().getSystemResourceAsStream("properties/cmdline.logging.properties"));
		logger = Logger.getLogger("CmdLineTrainer");

		if ( args.length < 4 ) {
			usage(true);
		}
		if ( ! (args[0].equals("-r") || args[0].equals("-c")) ) {
			usage(true);
		}

		//File configProps = new File(inputDir, taggerPropFile.toString());
		File configProps = new File(trainingPropFile.toString());
		Map<String,String> configMap = null;
		if ( configProps.canRead() ) {
			configMap = loadConfiguration(configProps);
		}
else {
System.err.println("Error reading configuration");
		}

		File config = new File(args[1]);
		File training = new File(args[2]);
		File testing = new File(args[3]);
		if ( ! (config.isFile() && config.canRead() && training.isFile() && training.canRead() && testing.isFile() && testing.canRead())) {
			usage(true);
		}

		//Trainer trainer = Trainer.getInstance(config.toString());
		Trainer trainer = Trainer.getInstance(configMap);
		trainer.train(training.toString(), testing.toString(), args[0]);
	}

	private static Map loadConfiguration(File file) throws FileNotFoundException, IOException {
		FileReader pr = new FileReader(file);
		Properties p = new Properties();
		p.load(pr);
		return p;
	}

	private static void usage(boolean exit) {
		System.err.println(usage);
		if ( exit ) { System.exit(1); }
	}
}
// vim:ts=4:noet:indentexpr=""
