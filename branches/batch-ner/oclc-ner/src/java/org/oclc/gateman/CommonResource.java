package org.oclc.gateman;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.parsers.*;

import LBJ2.parse.LinkedVector;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Preference;
import org.restlet.data.MediaType;
import org.restlet.resource.Resource;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import org.oclc.gateman.storage.Storage;
import org.oclc.gateman.ner.Tagger;
import org.oclc.gateman.ner.Formatter;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Base resource class that supports common behaviours or attributes shared by
 * all resources.
 * 
 */
public abstract class CommonResource extends Resource {

	public static final MediaType TEXT_NER = new MediaType("text/x-ner-markup");
	public static final MediaType TEXT_NER_ENT = new MediaType("text/x-ner-entities");
	public static final MediaType APP_NER_BATCH = new MediaType("application/x-ner-batch");

	protected Logger logger;
	protected String baseURI;
	protected Tagger tagger;
	protected Storage store;
	protected Configuration templateConfig;
	protected Formatter formatter;

	public CommonResource(Context context, Request request, Response response) {
		super(context, request, response);
		logger = context.getLogger();
		baseURI = request.getResourceRef().getIdentifier();
		store = (Storage) getContext().getAttributes().get("GatemanStorage");
		tagger = (Tagger) getContext().getAttributes().get("GatemanTagger");
		templateConfig = (Configuration) getContext().getAttributes().get("GatemanTemplateConfig");
	}


	protected String insertNewBatchResource(Representation entity) {
		long startTime = System.currentTimeMillis();
		MediaType batchItemFormat = TEXT_NER;

		try {
			String mt = makeBaseMediaType(entity);
			String id = store.putText(mt, entity.getText());
			logger.info("Resource id = " + id + " (" + makeFullMediaType(entity) + ")");

			List items = BatchParser.parseString(entity.getText());
			Iterator<Map> item_iterator = items.iterator();
			while (item_iterator.hasNext()) {
				Map<String,String> item = item_iterator.next();
				String tagged = tagAndFormat(item.get("content"), batchItemFormat);
				item.put("format", batchItemFormat.toString());
				item.put("content", tagged); // replace untagged with tagged version
			}

			Map template_model = new HashMap();
			template_model.put("items", items);

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
		
			store.putText(id, APP_NER_BATCH.toString(), buf.toString());
			long totalTime = System.currentTimeMillis() - startTime;
			long averageTime = totalTime / items.size();
			logger.info("Tagging took " + totalTime + " milliseconds (avg " + averageTime + ")");
			return id;
		}
		catch (IOException ioe) {
			logger.info(ioe.toString());

		}
		return null;
	}

	protected String insertNewResource(Representation entity) {
		try {
			String mt = makeBaseMediaType(entity);
			String text = entity.getText();

			// store the untagged representation of the text
			String id = store.putText(mt, text);
			logger.info("Resource id = " + id + " (" + makeFullMediaType(entity) + ")");
			logger.finer("      text = '" + text + "'");

			// Tag the result
			Vector<LinkedVector> tagged = tag(text);

			// store the xml version
			store.putText(id, MediaType.APPLICATION_XML.toString(), format(tagged, MediaType.APPLICATION_XML) );

			// store the html version
			store.putText(id, MediaType.TEXT_HTML.toString(), format(tagged, MediaType.TEXT_HTML) );

			// store the entities list version
			store.putText(id, TEXT_NER_ENT.toString(), format(tagged, TEXT_NER_ENT) );

			// store the NER/conll version
			store.putText(id, TEXT_NER.toString(), format(tagged, TEXT_NER) );

			return id;
		}
		catch (IOException ioe) { }
		return null;
	}

	protected void deleteResource(String id) {
		store.delete(id);
	}


	/**
	 * Tag a text and return it
	 *
	 */
	protected Vector<LinkedVector> tag(String text) {
		tagger.setSource(new StringReader(text));
		tagger.prepareText();
		tagger.tagText();
		return tagger.getData();
	}

	protected String tagAndFormat(String text, MediaType format) {
		Vector<LinkedVector> tagged_data = tag(text);
		return format(tagged_data, format);
	}

	protected String format(Tagger tagger, MediaType format) {
		Vector<LinkedVector> tagged_data = tagger.getData();
		return format(tagged_data, format);
	}

	protected String format(Vector<LinkedVector> tagged_data, MediaType format) {
		try {
			formatter = Formatter.getInstance(templateConfig);
			if ( format.equals(MediaType.APPLICATION_XML) ) {
				return formatter.xml(tagged_data);
			}
			else if ( format.equals(MediaType.TEXT_HTML) ) {
				return formatter.html(tagged_data, baseURI);
			}
			else if ( format.equals(TEXT_NER_ENT) ) {
				return formatter.list(tagged_data);
			}
			else if ( format.equals(TEXT_NER) ) {
				return formatter.conll(tagged_data);
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

	/**
	 * Return a resource
	 * 
	 * @return the resource corresponding to id and mediaType
	 */
	protected String getTaggedResource(String id, String mediaType) {
		// throw new InvalidMediaTypeException();
		logger.info("Get ID: " + id + " (" + makeFullMediaType(mediaType, null) + ")");
		if ( store.hasResource(id, mediaType) ) {
			String t = store.getText(id, mediaType);
			logger.finer("	text = " + t);
			return t;
		}
		return null;
	}

	// Make a basic version of a MediaType
	protected String makeBaseMediaType(MediaType mediaType) {
		if ( null == mediaType ) { return ""; }
		return mediaType.getMainType() + "/" + mediaType.getSubType();
	}
	protected String makeBaseMediaType(Representation rep) {
		if ( null == rep ) { return ""; }
		return makeBaseMediaType(rep.getMediaType());
	}

	// Make a "full" version of a MediaType, includes character set
	protected String makeFullMediaType(String baseMediaType, String charset) {
		if ( null == baseMediaType ) { return ""; }
		if ( null == charset ) { return baseMediaType; }
		return baseMediaType + "; " + charset;
	}
	protected String makeFullMediaType(MediaType mediaType, String charset) {
		if ( null == mediaType ) { return ""; }
		String base = makeBaseMediaType(mediaType);
		return makeFullMediaType(base, charset);
	}
	protected String makeFullMediaType(Representation rep) {
		if ( null == rep ) { return ""; }
		return makeFullMediaType(rep.getMediaType(), rep.getCharacterSet().toString());
	}

	protected Set getResourceList() {
		return store.getIdentifiers();
	}

	protected void printPreferredMediaTypes(Request request) {
		List<Preference<MediaType>> lpm = request.getClientInfo().getAcceptedMediaTypes();
		Iterator<Preference<MediaType>> li = lpm.iterator();
		while ( li.hasNext() ) {
			System.out.println("MediaType: '" + li.next().toString() + "'");
			// vars.add(new Variant(li.next().getMetadata()));
		}
	}

}
// vim: ts=4 indentexpr=""
