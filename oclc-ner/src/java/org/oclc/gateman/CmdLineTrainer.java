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
 * Command line tool for training NER models, layered over UIUC NER code.
 */
public class CmdLineTrainer {

	private static Logger logger;

	private static String usage = "Usage: org.oclc.gateman.CmdLineTrainer file-format properties-file training-file testing-file\n\tfile-format: -c or -r";
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

		File configProps = new File(args[1]);
		Map<String,String> configMap = null;
		if ( configProps.isFile() && configProps.canRead() ) {
			configMap = loadConfiguration(configProps);
			configMap.put(Trainer.ROOTDIR_KEY, configProps.getParentFile().getParent());
		}
		else {
			logger.severe("Can't read training properties file. Located at: \"" + configProps.toString() + "\"");
			System.exit(1);
		}

		File training = new File(args[2]);
		File testing = new File(args[3]);

		if ( ! (training.isFile() && training.canRead()) ) {
			logger.severe("Error reading training gold file: \"" + training.toString() + "\"");
			System.exit(1);
		}

		if ( ! (testing.isFile() && testing.canRead()) ) {
			logger.severe("Error reading testing file: \"" + testing.toString() + "\"");
			System.exit(1);
		}

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
