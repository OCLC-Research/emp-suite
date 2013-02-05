package org.oclc.gateman;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.util.logging.Logger;

public class BatchParser {
	protected static Logger logger = Logger.getLogger("org.oclc.gateman.GatemanApplication");;


	//public static List<Map> parseReader(Reader reader) {
	//	CharBuffer cb = CharBuffer.allocate(4096);
	//	while ( reader.read(cb) > 0 ) {
	//	}
	//}

	public static List<Map> parseString(String text) {
		ArrayList items = new ArrayList();

		text = text
			.replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
		logger.finer("      text = '" + text + "'");

		int item_start, item_end, id_start, id_end, content_start, content_end;
		String item, id, content;

		item_start = text.indexOf("<item");

		while (true) {
			item_end = text.indexOf("</item>", item_start) + 7;
			if ( item_end < 7 ) { break; } // < 7 (instead of 0) because of "+ 7" above

			item = text.substring(item_start, item_end);
			logger.finer("Item: \"\"" + item + "\"\"");

			id_start = item.indexOf("id=\'") + 4;
			id_end = item.indexOf("\'", id_start);
			id = item.substring(id_start, id_end);
			logger.finer("       id=\"" + id + "\"");

			content_start = item.indexOf(">") + 1;
			content_end = item.indexOf("<", content_start);
			content = item.substring(content_start, content_end);
			logger.finer("  content=\"" + content + "\"\n---");

			item_start = item_end + 1;

			HashMap entry = new HashMap();
			entry.put("id", id);
			entry.put("content", content);
			items.add(entry);
		}
		return items;
	}


	//	Parsing was done originally with DocumentBuilder. It had a bug it in that resulted
	//	in xml elements in the parsed data. Rolled my own "parser" above.
//	private static void tagBatch(String batchName, String outputFilename, String taggerPropFilename) {
//		initializeTagger(taggerPropFilename);
//		File batch = new File(batchName);
//
//		MediaType batchItemFormat = CommonResource.TEXT_NER;
//		batchItemFormat = CommonResource.TEXT_NER;
//		DocumentBuilderFactory bs = DocumentBuilderFactory.newInstance();
//
//		Map template_model = new HashMap();
//		ArrayList t_items = new ArrayList();
//		template_model.put("items", t_items);
//
//		try {
//			DocumentBuilder builder = null;
//			Document doc = null;
//			try {
//				builder = bs.newDocumentBuilder();
//				//doc = builder.parse(new InputSource(new StringReader(text)));
//				doc = builder.parse(new InputSource(new FileReader(batch)));
//			}
//			catch(SAXException se) {
//				logger.warning(se.toString());
//			}
//			catch(ParserConfigurationException pce) {
//				logger.warning(pce.toString());
//			}
//
//			NodeList items = doc.getElementsByTagName("item");
//			for (int i = 0; i < items.getLength(); i++ ) {
//				Node item = items.item(i);
//				String itemid = "";
//				Node idAttr = item.getAttributes().getNamedItem("id");
//				if ( null != idAttr ) {
//					itemid = idAttr.getNodeValue();
//				}
//				tag(new StringReader(item.getTextContent()));
//
//				String content = format(tagger, batchItemFormat);
//
//				HashMap t_item = new HashMap();
//				t_item.put("id", itemid);
//				t_item.put("format", batchItemFormat.toString());
//				t_item.put("content", content);
//				t_items.add(t_item);
//			}
//
//			StringWriter buf = new StringWriter();
//			try {
//
//				Template template = templateConfig.getTemplate("ner_batch.xml");
//				template.process(template_model, buf);
//				buf.flush();
//			}
//			catch (TemplateException te) {
//				logger.warning(te.toString());
//			}
//			catch (IOException ioe) {
//				logger.warning(ioe.toString());
//			}
//			catch (Exception e) {
//			e.printStackTrace();
//			}
//
//			FileWriter fw = new FileWriter(outputFilename);
//			fw.write(buf.toString());
//			fw.flush();
//		}
//		catch (IOException ioe) {
//			logger.info(ioe.toString());
//		}
//	}

}

