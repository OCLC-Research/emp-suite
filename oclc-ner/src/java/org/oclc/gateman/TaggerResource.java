package org.oclc.gateman;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Iterator;
import java.util.TreeSet;

import java.util.logging.Logger;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

/**
 * Resource that tags and stores resources
 * 
 */
public class TaggerResource extends CommonResource {

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
		String mt = entity.getMediaType().getMainType() + "/" + entity.getMediaType().getSubType();
		Response rsp = getResponse();

		if ( ! mt.equals("text/plain") ) {
			// only support text/plain currently
			rsp.setStatus(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);
			return;
		}

		if ( null != (id = insertNewResource(entity) ) ) {
			//rsp.setRedirectRef( baseURI + "/" + id); // URL of created resource
			//logger.info("Redirect: " + rsp.getRedirectRef());
			rsp.redirectSeeOther(baseURI + "/" + id); // URL of created resource
			rsp.setStatus(Status.SUCCESS_CREATED);

			// The default response type is text/html, make default client dependent (html for browser/xml for other)
			MediaType retMT = MediaType.TEXT_HTML;
			rsp.setEntity(new StringRepresentation(getTaggedResource(id, retMT.toString()), retMT));
		}
	}

	/**
	 * Return a description of the service or a list of tagged resources.
	 *
	 * text/html returns description
	 * text/uri-list returns resource list
	 */
	@Override
	public Representation getRepresentation(Variant variant) {
		// Generate the right representation according to its media type.
		StringBuilder sb = new StringBuilder();
		Iterator<String> i = new TreeSet<String>(getResourceList()).iterator();

		if (MediaType.TEXT_URI_LIST.equals(variant.getMediaType())) {
			while (i.hasNext()) {
				sb.append(baseURI + "/" + i.next().toString() + "\r\n"); // text/uri-list requires \r\n
			}
			return new StringRepresentation(sb.toString(), MediaType.TEXT_URI_LIST);
		}
		else if (MediaType.TEXT_HTML.equals(variant.getMediaType())) {
			sb.append("<html><head><title>Gateman Named Entity Tagger</title></head><body>Resource List:<ul>");
			String url;
			while (i.hasNext()) {
				url = baseURI + "/" + i.next().toString();
				sb.append("<li><a href=\"" + url + "\">" + url + "</a></li>");
			}
			sb.append("</ul></body></html>");
			return new StringRepresentation(sb.toString(), MediaType.TEXT_HTML);
		}
		return null;
	}


}
// vim: ts=4 indentexpr=""
