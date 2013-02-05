package org.oclc.gateman;

import java.io.IOException;
import java.io.StringWriter;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import java.util.logging.Logger;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.data.CharacterSet;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Resource that tags and stores resources
 * 
 */
public class TaggerResource extends CommonResource {

	private static ArrayList<String> validPostMediaType = new ArrayList<String>(2);
	static {
		validPostMediaType.add("text/plain");
		validPostMediaType.add("application/x-ner-batch");
	}

	public TaggerResource(Context context, Request request, Response response) {
		super(context, request, response);
		// Declare the representations supported by this resource.
		// The first one added is used for preferredMediaType = */*
 		List vars = getVariants();
		vars.add(new Variant(MediaType.TEXT_HTML));
		vars.add(new Variant(MediaType.TEXT_URI_LIST));
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	/**
	 * Handle POST requests: create a new item.
	 * POST &amp; process the data, return the new location
	 */
	@Override
	public void post(Representation entity) {
		String id;
		String mt = makeBaseMediaType(entity);
		Response rsp = getResponse();

		if ( ! validPostMediaType.contains(mt) ) {
			// only support text/plain currently
			rsp.setStatus(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);
			return;
		}

		if ( mt.equals("text/plain") ) {
			if ( null != (id = insertNewResource(entity) ) ) {
				rsp.redirectSeeOther(baseURI + "/" + id); // URL of created resource
				rsp.setStatus(Status.SUCCESS_CREATED);

				// The default response to POST is NER/CONLL
				MediaType retMT = TEXT_NER;
				rsp.setEntity(new StringRepresentation(getTaggedResource(id, retMT.toString()), retMT));
			}
			else {
				logger.warning("Error tagging resource");
			}
		}
		else if ( mt.equals(APP_NER_BATCH.toString())) {
			if ( null != (id = insertNewBatchResource(entity) ) ) {
				rsp.redirectSeeOther(baseURI + "/" + id); // URL of created resource
				rsp.setStatus(Status.SUCCESS_CREATED);

				MediaType retMT = APP_NER_BATCH;
				StringRepresentation sr = new StringRepresentation(getTaggedResource(id, retMT.toString()), retMT);
				sr.setCharacterSet(CharacterSet.UTF_8);
				rsp.setEntity(sr);
			}
			else {
				logger.warning("Error tagging batch resource");
			}
		}
	}

	/**
	 * Return a description of the service or a list of tagged resources.
	 *
	 * text/html returns description, and an html list
	 * text/uri-list returns resource list
	 */
	@Override
	public Representation getRepresentation(Variant variant) {
		ArrayList resources = new ArrayList();
		Iterator<String> i = new TreeSet<String>(getResourceList()).iterator();
		while (i.hasNext()) {
			resources.add(baseURI + "/" + i.next().toString());
		}
		Map template_model = new HashMap();
		template_model.put("resources", resources);

		StringWriter buf = new StringWriter();
		MediaType buf_mediatype = null;
		try {
			String templateName = null;
			if (MediaType.TEXT_URI_LIST.equals(variant.getMediaType())) {
				buf_mediatype = MediaType.TEXT_URI_LIST;
				templateName = "resource_list.uri-list";
			}
			else if (MediaType.TEXT_HTML.equals(variant.getMediaType())) {
				buf_mediatype = MediaType.TEXT_HTML;
				templateName = "resource_list.html";
			}
			Template template = templateConfig.getTemplate(templateName);
			template.process(template_model, buf);
			buf.flush();
		}
		catch (TemplateException te) {
			logger.warning(te.toString());
		}
		catch (IOException ioe) {
			logger.warning(ioe.toString());
		}
		return new StringRepresentation(buf.toString(), buf_mediatype);
	}


}
// vim: ts=4 indentexpr=""
