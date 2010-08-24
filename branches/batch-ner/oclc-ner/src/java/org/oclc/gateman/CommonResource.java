package org.oclc.gateman;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Logger;

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

/**
 * Base resource class that supports common behaviours or attributes shared by
 * all resources.
 * 
 */
public abstract class CommonResource extends Resource {

	public static final MediaType TEXT_NER = new MediaType("text/x-ner-markup");
	public static final MediaType TEXT_NER_ENT = new MediaType("text/x-ner-entities");

	protected Logger logger;
	protected String baseURI;
	protected Tagger tagger;
	protected Storage store;

	public CommonResource(Context context, Request request, Response response) {
		super(context, request, response);
		logger = context.getLogger();
		baseURI = request.getResourceRef().getIdentifier();
		store = (Storage) getContext().getAttributes().get("GatemanStorage");
		tagger = (Tagger) getContext().getAttributes().get("GatemanTagger");
	}

	protected String insertNewResource(Representation entity) {
		try {
			String mt = entity.getMediaType().getMainType() + "/" + entity.getMediaType().getSubType();
			String text = entity.getText();

			// store the untagged representation of the text
			String id = store.putText(mt, text);
			logger.info("Resource id = '" + id + "::" + mt + "'");
			logger.finer("   charset = " + entity.getCharacterSet());
			logger.finer("      text = '" + text + "'");
			tagAndStore(id, text);

			return id;
		}
		catch (IOException ioe) { }
		return null;
	}

	protected void deleteResource(String id) {
		store.delete(id);
	}


	/**
	 * Tag a text and store it
	 *
	 */
	protected void tagAndStore(String id, String text) {
		tagger.setSource(new StringReader(text));
		tagger.prepareText();
		logger.info("Start tagging : id = " + id);
		tagger.tagText();
		logger.info("End   tagging : id = " + id);
		String taggedText = tagger.formatResultsXML();
		store.putText(id, MediaType.APPLICATION_XML.toString(), taggedText);

		taggedText = tagger.formatResultsHTML(baseURI + "/" + id);
		store.putText(id, MediaType.TEXT_HTML.toString(), taggedText);

		taggedText = tagger.formatResultsENT();
		store.putText(id, TEXT_NER_ENT.toString(), taggedText);

		taggedText = tagger.formatResults();
		store.putText(id, TEXT_NER.toString(), taggedText);
	}

	/**
	 * Return a resource
	 * 
	 * @return the resource corresponding to id and mediaType
	 */
	protected String getTaggedResource(String id, String mediaType) {
		// throw new InvalidMediaTypeException();
		logger.info("Get ID: " + makeIdentifier(id, mediaType));
		if ( store.hasResource(id, mediaType) ) {
			String t = store.getText(id, mediaType);
			logger.finer("	text = " + t);
			return t;
		}
		return null;
	}

	private String makeIdentifier(String id, String mediaType) {
		return id + "::" + mediaType;
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
