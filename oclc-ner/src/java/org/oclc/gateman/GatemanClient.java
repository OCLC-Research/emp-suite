package org.oclc.gateman;

import java.io.IOException;

import java.util.Map;
import java.util.List;
import java.util.Iterator;

import java.util.logging.Logger;

import org.restlet.Client;
import org.restlet.data.Form;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;

import org.restlet.data.Request;
import org.restlet.data.Method;
import org.restlet.data.Preference;
import org.restlet.data.MediaType;

import org.restlet.data.Parameter;
import org.restlet.util.Series;

public class GatemanClient {
	private static Logger logger;

	// The URI of the resource "list of items".
	private static Reference service = new Reference("http://localhost:8182/webservices/gateman");

	public static void main(String[] args) throws IOException {
		// Define our Restlet HTTP client.
		Client client = new Client(Protocol.HTTP);
		logger = client.getContext().getLogger();

		String text ="The British demand for portraiture increased rapidly in the eighteenth century as members of the wealthy middle class became art patrons in their own right. Arthur Devis was a provincial artist who came to live in London, where sophisticated portraitists of the upper class such as Reynolds and Gainsborough dominated the art world. Devis received commissions from the middle-class landowning families, merchants, and officials who lived in smaller cities outside London. This informal portrait is a conversation piece, a genre favored by Devis. The figures, while full-length, are relatively small and are placed somewhat back in the landscape; the background is larger and more detailed than in traditional portraiture and describes the subjects' personal and social context. Devis devised a repertoire of postures and gestures that he used to express the social status of his sitter. Arthur Holdsworth, governor of Dartmouth Castle, is shown seated, an alert, attentive expression on his face. The ship sailing into the mouth of the River Dart in the background may be a reference to the Holdsworth family's trading business. Holdsworth's brother-in-law, Thomas Taylor, stands behind him in riding clothes. The third man is Captain Stancombe.";

		String url = postSample(client, text);
		try { Thread.sleep(3000); } catch (InterruptedException ie) {}

		if ( null != url ) {
			logger.info("URL: " + url);
			Reference resource = new Reference(url);
			getTaggedResource(client, resource);
		}
	}

	/**
	 * Print out a tagged resource.
	 *
	 * @param client
	 *            client Restlet.
	 * @param reference
	 *            the resource's URI
	 * @throws IOException
	 */
	public static void getTaggedResource(Client client, Reference reference) throws IOException {
		Request req = new Request(Method.GET, reference);
		addPreferredMediaType(req, new MediaType("text/x-ner-markup"), 1);
		Response response = new Response(req);
		client.handle(req, response);
		logger.info("GET " + reference.toString() + " status = " + response.getStatus().toString());
		if (response.getStatus().isSuccess()) {
			if (response.isEntityAvailable()) {
				logger.info("Entity available");
				//response.getEntity().write(System.out);
				String a = response.getEntity().getText();
				System.out.println("'" + a + "'");
			}
			else {
				logger.info("Entity NOT available");
			}
		}
	}


	/**
	 * Post a sample text to the service
	 *
	 * @param client
	 *            client Restlet.
	 * @throws IOException
	 */
	public static String postSample(Client client, String text) throws IOException {
		Representation rep = new StringRepresentation(text, MediaType.TEXT_PLAIN);
		Request req = new Request(Method.POST, service, rep);
		Response resp = new Response(req);

		client.handle(req, resp);
		logger.info("Status: " + resp.getStatus().toString());

		if (resp.getStatus().isSuccess()) { 
			logger.info("Created Resource: " + resp.getRedirectRef().getIdentifier());
			return resp.getRedirectRef().getIdentifier();
		}

		return null;
	}

	/**
	 * Print list of tagged resources currently available
	 * 
	 * @param client
	 *            client Restlet.
	 * @throws IOException
	 */
	public static void printResourceList(Client client) throws IOException {

		Request req = new Request(Method.GET, service);
		addPreferredMediaType(req, MediaType.TEXT_URI_LIST, 1);
		addPreferredMediaType(req, MediaType.TEXT_HTML, (float).9);

		Response response = new Response(req);
		client.handle(req, response);
		if (response.getStatus().isSuccess()) {
			if (response.isEntityAvailable()) {
				response.getEntity().write(System.out);
				// System.out.println("Media Type: " + response.getEntity().getMediaType().toString());
			}
		}
	}

	private static void addPreferredMediaType(Request r, MediaType m, float quality) {
		Preference<MediaType> preferred = new Preference<MediaType>();
		preferred.setMetadata(m);
		preferred.setQuality(quality);

		List<Preference<MediaType>> mediaTypes = r.getClientInfo().getAcceptedMediaTypes();
		mediaTypes.add(preferred);
	}


	private static void printParameters(Client c) {
		Series<Parameter> s = c.getContext().getParameters();
		Iterator<Parameter> i = s.iterator();
		System.err.println("Client Parameters:");
		while (i.hasNext()) {
			Parameter p = i.next();
			System.err.println(p.getName());
		}
	}

	private static void printAttributes(Client c) {
		Map<String,Object> m = c.getContext().getAttributes();
		Iterator<String> i = m.keySet().iterator();
		System.err.println("Client Attributes:");
		while (i.hasNext()) {
			String key = i.next();
			System.err.println(key);
		}
	}
}
