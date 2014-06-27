package com.cloudant;

import org.codehaus.jackson.annotate.*;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.support.CouchDbDocument;
import org.ektorp.support.CouchDbRepositorySupport;

public class CrudWithEktorp {

    public static void main( String[] args ) throws Exception {
    	String user, pass, db;

    	// read the user's credentials
		System.out.print("Enter your Cloudant username: ");
		user = System.console().readLine();
		System.out.print("Enter your Cloudant password: ");
		pass = new String(System.console().readPassword());
		System.out.print("Enter your database name: ");
		db = System.console().readLine();

        // create the http connection
        HttpClient httpClient = new StdHttpClient.Builder()
        				.url("https://" + user + ".cloudant.com")
        				.username(user)
        				.password(pass)
        				.build();

        // the new document id and age
        String id = "John Doe";
        int age = 35;

        // initialize couch instance and connector
    	CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
    	CouchDbConnector dbc = new StdCouchDbConnector(db, dbInstance);

    	// create a new "repo" that allows for built-in CRUD functionality
    	CrudRepository repo = new CrudRepository(dbc);

    	// create a document for use in this example
    	CrudDocument doc = new CrudDocument();
    	doc.setId(id);
    	doc.setAge(age);

    	// add the doc to the database
    	System.out.println("Adding new document to database...");
    	repo.add(doc);

    	// neat method that returns a boolean on whether the id exists in the database
    	if(repo.contains(id))
    		System.out.println("Successfully wrote document!");
    	else
    		System.out.println("FAILED to write document to database!");

    	// read the document back based on id
    	System.out.println("Reading document back from database...");
    	doc = repo.get(id);
    	System.out.println("Revision of the document is: " + doc.getRevision());

    	// update the doc and re-commit
    	System.out.println("Updating document with new value...");
    	doc.setAge(50);
    	repo.update(doc);
    	System.out.println("Wrote updated document to database!");

    	// delete the document
    	System.out.println("Deleting document from the database...");
    	repo.remove(doc);
    	System.out.println("Successfully deleted the document!");
    }

    // Our custon "repo" class which extends the Ektorp base class.  The CouchDbRepositorySupport<T> class
    // contains built in CRUD methods.  The Ektorp library as a whole contains more functionality than what
    // is represented in this example.
    public static class CrudRepository extends CouchDbRepositorySupport<CrudDocument> {
		public CrudRepository(CouchDbConnector dbc) {
			super(CrudDocument.class, dbc, true);
		}
	}

	// Class to abstract the JSON document.
	public static class CrudDocument extends CouchDbDocument {
		private int age;

		@JsonProperty("age")
		public void setAge(int a) {
			age = a;
		}

		@JsonProperty("age")
		public int getAge() {
			return age;
		}
	}
}
