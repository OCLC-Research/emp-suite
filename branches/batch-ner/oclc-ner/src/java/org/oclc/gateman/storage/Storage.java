package org.oclc.gateman.storage;

import java.util.Set;
import java.util.HashMap;

/** 
 * Simple HashMap based storage.
 *
 * Allows for elements to be stored/retrieved by an { identifier, media-type } tuple.
 *
 * @author Devon Smith
 * @date 2009-02-13
 *
 */
public class Storage {

	// The database
	private HashMap<String,HashMap> db = new HashMap<String,HashMap>();

	// The maximum number of elements that can be stored at one time.
	private int size = 0;
	// For picking the next identifier
	private int counter = 0;

	/**
	 * Stub Constructor Documentation
	 *
	 */
	public Storage(int size) { this.size = size; }

	/* Get the next identifier
	 */
	private String nextIdentifier() {
		if ( ++counter == size + 1 ) { counter = 1; } // reset counter
		return Integer.toString(counter);
	}

	/* Get a Set of identifiers in the store
	 */
	public Set getIdentifiers() {
		return db.keySet();
	}

	/* Store a representation in the store
	 */
	public String putText(String mediaType, String text) {
		return putText(nextIdentifier(), mediaType, text);
	}

	/* Store a representation in the store
	 */
	public String putText(String id, String mediaType, String text) {
		if ( null == mediaType || null == text ) { throw new NullPointerException(); }
		if ( null == id ) { id = nextIdentifier(); }

		HashMap<String,String> second;
		if ( db.containsKey(id) ) {
			second = db.get(id);
		}
		else {
			second = new HashMap<String,String>();
			db.put(id, second);
		}
		second.put(mediaType, text);

		return id;
	}

	/* Return a representation for the identifier
	 */
	public String getText(String id, String mediaType) {
		if ( null == id || null == mediaType ) { throw new NullPointerException(); }
		if ( hasResource(id, mediaType) ) {
			HashMap<String,String> second = db.get(id);
			return second.get(mediaType);
		}
		else {
			return "";
		}
	}

	/* Delete the identified element from the store
	 */
	public void delete(String id) {
		if ( null != id ) {
			db.remove(id);
		}
	}

	/* Determine if the store has at least one representation for the identifier
	 */
	public boolean hasResource(String id) {
		if ( null == id ) { throw new NullPointerException(); }
		return db.containsKey(id);
	}

	/* Determine if the store has a specific representation for the identifier
	 */
	public boolean hasResource(String id, String mediaType) {
		if ( null == id || null == mediaType ) { throw new NullPointerException(); }
		if ( db.containsKey(id) ) {
			HashMap<String,String> second = db.get(id);
			if ( second.containsKey(mediaType) ) {
				return true;
			}
		}
		return false;
	}

}

