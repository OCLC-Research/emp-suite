package org.oclc.gateman;

import java.io.IOException;

import java.util.List;
import java.util.Iterator;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

/**
 * Class for text items that have been NER tagged
 * 
 */
public class TaggedResource extends CommonResource {

	// Relative identifier for the resources 
	private String resourceIdentifier;
	// Representation for the resource
	private String representation;

	// Message for when there isn't a representation for an identifier
	private String notFound = "<html><head><title>404 Not Found</title></head><body><h1>404 Not Found</h1></body></html>";

	public TaggedResource(Context context, Request request, Response response) {
		super(context, request, response);

		// Get the "gid" attribute value taken from the URI template /gateman/{gid}.
		this.resourceIdentifier = (String) getRequest().getAttributes().get("gid");
		logger.info("Resource id = " + resourceIdentifier);

		// Make sure the requested resource exists in the service
		if ( ! store.hasResource(resourceIdentifier) ) {
			response.setEntity(notFound, MediaType.TEXT_HTML);
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return;
		}

 		// Declare the representations supported by this resource.
		// The first one added is used for preferredMediaType = */*
		List vars = getVariants();
		vars.add(new Variant(MediaType.TEXT_HTML));
		vars.add(new Variant(MediaType.APPLICATION_XML));
		vars.add(new Variant(MediaType.TEXT_PLAIN));
		vars.add(new Variant(TEXT_NER));
		vars.add(new Variant(TEXT_NER_ENT));
	}

	@Override
	public Representation getRepresentation(Variant variant) {
		MediaType vmt = variant.getMediaType();
		logger.info("Variant: " + vmt.toString());
		if ( !(vmt.equals(TEXT_NER)
				|| vmt.equals(TEXT_NER_ENT)
				|| vmt.equals(APP_NER_BATCH)
				|| vmt.equals(MediaType.TEXT_HTML)
				|| vmt.equals(MediaType.TEXT_PLAIN)
				|| vmt.equals(MediaType.APPLICATION_XML))) {

			logger.info("Invalid Variant: " + vmt.toString());
			return null;
		}
		this.representation = getTaggedResource(this.resourceIdentifier, vmt.toString());
		return new StringRepresentation(this.representation, vmt);
	}

	/**
	 * This resource supports DELETE requests.
	 */
	@Override
	public boolean allowDelete() {
		return true;
	}

	/**
	 * Handle DELETE requests.
	 */
	@Override
	public void delete() {
		deleteResource(resourceIdentifier);
		// Tells the client that the request has been successfully fulfilled.
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}


}
// vim: ts=4 indentexpr=""
