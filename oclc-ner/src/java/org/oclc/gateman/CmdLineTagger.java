package org.oclc.gateman;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Reader;
import java.io.StringReader;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import java.util.logging.Logger;
import java.util.logging.LogManager;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import org.restlet.data.MediaType;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.DefaultObjectWrapper;

import org.oclc.gateman.ner.Formatter;
import org.oclc.gateman.CommonResource;
import org.oclc.gateman.ner.Tagger;
import org.oclc.gateman.logging.LogFormatter;

/**
 * Command line tool for tagging named entities in text.
 */
public class CmdLineTagger {

	private static Logger logger;
	private static Tagger tagger;
	private static Configuration templateConfig;
	private static File taggerPropFile = new File("tagger.prop");

	private static String usage = "Usage: org.oclc.gateman.CmdLineTagger -d input-dir output-dir [ format ]\n                                      -f batch-file output-file properties-file";

	public static void main(String[] args) throws IOException, FileNotFoundException {
		LogManager lm = LogManager.getLogManager();
		lm.readConfiguration(ClassLoader.getSystemClassLoader().getSystemResourceAsStream("properties/cmdline.logging.properties"));
		logger = Logger.getLogger("CmdLineTagger");
		if ( null == args || null == args[0] ) {
			usage(true);
		}

		/* Create and adjust the configuration */
		templateConfig = new Configuration();
		templateConfig.setDirectoryForTemplateLoading(new File("/home/smithde/proj/ndiipp/emp-batch-ner/oclc-ner/src/template/"));
		templateConfig.setObjectWrapper(new DefaultObjectWrapper());

		if ( "-d".equals(args[0])) {
			if ( args.length < 3 || args.length > 4) {
				usage(true); // error. must have at least two args, but no more than 3
			}
			tagDirectory(args[1], args[2], args[3]);
		}
		else if ( "-f".equals(args[0])) {
			tagBatchFile(args[1], args[2], args[3]);
		}
	}

	private static void initializeTagger(String taggerPropFilename) {
		//File configProps = new File(inputDir, taggerPropFile.toString());
		File configProps = new File(taggerPropFilename);
		if ( ! configProps.isFile() ) {
			logger.severe("Tagging properties file not found. Expected at: \"" + configProps.toString() + "\"");
			System.exit(1);
		}

		Map<String,String> configMap = null;
		if ( configProps.canRead() ) {
			try {
				configMap = loadConfiguration(configProps);
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}
		}
		else {
			logger.severe("Can't read tagging properties file. Located at: \"" + configProps.toString() + "\"");
			System.exit(1);
		}

		logger.info("Initializing tagger (this may take a few minutes) ...");
		tagger = Tagger.getInstance(configMap); // this will take a few _minutes_
		logger.info("Tagger initialized");
	}

	private static void tagBatchFile(String batchName, String outputFilename, String taggerPropFilename) {
		initializeTagger(taggerPropFilename);
		File batch = new File(batchName);

		MediaType batchItemFormat = CommonResource.TEXT_NER;
		batchItemFormat = CommonResource.TEXT_NER;
		DocumentBuilderFactory bs = DocumentBuilderFactory.newInstance();

		Map template_model = new HashMap();
		ArrayList t_items = new ArrayList();
		template_model.put("items", t_items);

		try {
			DocumentBuilder builder = null;
			Document doc = null;
			try {
				builder = bs.newDocumentBuilder();
				//doc = builder.parse(new InputSource(new StringReader(text)));
				doc = builder.parse(new InputSource(new FileReader(batch)));
			}
			catch(SAXException se) {
				logger.warning(se.toString());
			}
			catch(ParserConfigurationException pce) {
				logger.warning(pce.toString());
			}

			NodeList items = doc.getElementsByTagName("item");
			for (int i = 0; i < items.getLength(); i++ ) {
				Node item = items.item(i);
				String itemid = "";
				Node idAttr = item.getAttributes().getNamedItem("id");
				if ( null != idAttr ) {
					itemid = idAttr.getNodeValue();
				}
				tag(new StringReader(item.getTextContent()));

				String content = format(tagger, batchItemFormat);

				HashMap t_item = new HashMap();
				t_item.put("id", itemid);
				t_item.put("format", batchItemFormat.toString());
				t_item.put("content", content);
				t_items.add(t_item);
			}

			StringWriter buf = new StringWriter();
			try {

				Template template = templateConfig.getTemplate("ner_batch.xml");
				template.process(template_model, buf);
				buf.flush();
			}
			catch (TemplateException te) {
				logger.warning(te.toString());
			}
			catch (IOException ioe) {
				logger.warning(ioe.toString());
			}
			catch (Exception e) {
			e.printStackTrace();
			}

			FileWriter fw = new FileWriter(outputFilename);
			fw.write(buf.toString());
			fw.flush();
		}
		catch (IOException ioe) {
			logger.info(ioe.toString());
		}
	}



	//private static void tagDirectory(File inputDir, File outputDir, String format) {
	private static void tagDirectory(String inputDirName, String outputDirName, String formatString) {
		File inputDir = new File(inputDirName);
		File outputDir = new File(outputDirName);
		MediaType format = null != formatString ? new MediaType(formatString) : CommonResource.TEXT_NER;

		if ( ! (inputDir.isDirectory() && outputDir.isDirectory() ) ) {
			// error. both must be directories
			logger.severe("Supplied input and output are not directories: input: \"" + inputDir.toString() + "\"; output: \"" + outputDir.toString() + "\"");
			usage(true);
		}

		initializeTagger( (new File(inputDir, taggerPropFile.toString())).toString() );

		File[] inputs = inputDir.listFiles();
		for (File f : inputs) {
			if ( f.getName().equals(taggerPropFile.getName()) ) { continue; } // skip the prop file
			if ( f.isHidden() ) {
				logger.info("Skipping " + f.getName() + " - hidden file");
				continue;
			}
			logger.info("Tagging " + f.getName() + " ...");
			try {
				tagFile(f, new File(outputDir, f.getName()), format);
			}
			catch (Exception e) {
				logger.info("Skipping " + f.getName() + " - exception: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private static Map loadConfiguration(File file) throws FileNotFoundException, IOException {
		FileReader pr = new FileReader(file);
		Properties p = new Properties();
		p.load(pr);
		return p;
	}

	/**
	 * Tag a file
	 *
	 */
	private static void tagFile(File in, File out, MediaType format) throws IOException, FileNotFoundException {
		tag(new FileReader(in));
		FileWriter fw = new FileWriter(out);
		fw.write(format(tagger, format));
		fw.close();
	}

	private static void tag(Reader reader) {
		tagger.setSource(reader);
		tagger.prepareText();
		tagger.tagText();
	}

	private static String format(Tagger tagger, MediaType format) {
		try {
			Formatter formatter = Formatter.getInstance(templateConfig);
			if ( format.equals(MediaType.APPLICATION_XML) ) {
				return formatter.xml(tagger.getData());
			}
			else if ( format.equals(MediaType.TEXT_HTML) ) {
				return formatter.html(tagger.getData(), null);
			}
			else if ( format.equals(CommonResource.TEXT_NER_ENT) ) {
				return formatter.list(tagger.getData());
			}
			else if ( format.equals(CommonResource.TEXT_NER) ) {
				return formatter.conll(tagger.getData());
			}
			else {
				logger.warning("Unsupported Media Type in output formatting");
			}
		}
		catch (IOException ioe) {
		}
		catch (TemplateException te) {
		}
		return "";
	}

	private static void usage(boolean exit) {
		System.err.println(usage);
		if ( exit ) { System.exit(1); }
	}

}
// vim:ts=4:noet:indentexpr=""
