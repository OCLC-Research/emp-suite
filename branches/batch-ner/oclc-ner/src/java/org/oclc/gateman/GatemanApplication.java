package org.oclc.gateman;

import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.File;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.Router;

import org.oclc.gateman.storage.Storage;
import org.oclc.gateman.ner.Tagger;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;

public class GatemanApplication extends Application {

	public GatemanApplication(Context parentContext) {
		super(parentContext);
	}

	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */
	@Override
	public synchronized Restlet createRoot() {
		Context context = getContext();
		Logger logger = context.getLogger();
		logger.info("Logger: name = \"" + logger.getName() + "\" @ level = " + logger.getLevel());

		Map<String,Object> attr = context.getAttributes();
		logger.info("Starting Gateman Application ...");

		attr.put("GatemanTagger", Tagger.getInstance());
		logger.info("... Tagger built");

		// 11 is the number resources that can be stored at a time;
		// will overwrite oldest resource when it runs out of space
		attr.put("GatemanStorage", new Storage(11));
		logger.info("... Storage built");

        /* Create and adjust the configuration */
        Configuration cfg = new Configuration();
		try {
        	cfg.setDirectoryForTemplateLoading(new File("/home/smithde/proj/ndiipp/emp-batch-ner/oclc-ner/src/template/"));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
        cfg.setObjectWrapper(new DefaultObjectWrapper());
		attr.put("GatemanTemplateConfig", cfg);
		logger.info("... Templates configured");

		// Create a router Restlet that defines routes.
		Router router = new Router(getContext());
		router.attach("/gateman", TaggerResource.class);
		router.attach("/gateman/{gid}", TaggedResource.class);
		logger.info("... Router built");
		logger.info("Gateman Ready");
		return router;
	}
}
