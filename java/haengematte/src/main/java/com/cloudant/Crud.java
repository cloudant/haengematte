package com.cloudant;

import java.io.*;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.*;
import org.apache.commons.codec.binary.Base64;

public class Crud {
	
	private static String user, db, pass, baseUrl;
	private static DefaultHttpClient httpClient = new DefaultHttpClient();
	private static ObjectMapper mapper = new ObjectMapper();
	
	// Common method to base64 encode the username and password combo.  Adds the encoded string to the passed request.
	private static void addAuth(HttpRequest req) {
		String encodedUserPass = new String(Base64.encodeBase64((user + ":" + pass).getBytes()));
		req.setHeader("Authorization", "Basic " + encodedUserPass);
	}
	
	// Create a new document
	private static String create(String jsonDoc) {
		// initialize the POST with the base URL
		HttpPost httpPost = new HttpPost(baseUrl);

		// add the JSON payload (i.e. the new document)
		httpPost.setEntity(new StringEntity(jsonDoc, ContentType.APPLICATION_JSON));

		// add your credentials to the request
		addAuth(httpPost);

		// initialize the id we want to return
		String id = "";

		try {
			// send the request and read the response
			HttpResponse postResp = httpClient.execute(httpPost);
			InputStream is = postResp.getEntity().getContent();
		  	ObjectNode postRespDoc = mapper.readValue(is, ObjectNode.class);
		  	id = ((TextNode)postRespDoc.get("id")).getTextValue();
		  	System.out.println("The new document's ID is " + id + ".");
		} catch(IOException e) {
			System.out.println("An error occurred while creating a new document.");
			System.out.println(e.getMessage());
			System.exit(-1);
		} finally {
			// release the connection
		  	httpPost.releaseConnection();
		}

	  	// return the id of the new document created
	  	return id;
	}

	// Read a document
	private static ObjectNode read(String id) {
		// initialize the GET with the base URL and the document ID
		HttpGet httpGet = new HttpGet(baseUrl + id);

		// add your credentials to the request
	  	addAuth(httpGet);

	  	// initialize the document we want to return
	  	ObjectNode doc = null;

	  	try {
		  	// send the request and read the response
		  	HttpResponse getResp = httpClient.execute(httpGet);
		  	doc = mapper.readValue(getResp.getEntity().getContent(), ObjectNode.class);
		  	String rev = ((TextNode)doc.get("_rev")).getTextValue();
		  	httpGet.releaseConnection();
		  	System.out.println("The revision after this read request is " + rev + ".");
		} catch(IOException e) {
			System.out.println("An error occurred while reading a document.");
			System.out.println(e.getMessage());
			System.exit(-1);
		} finally {
			// release the connection
			httpGet.releaseConnection();
		}

		// return the read doc
		return doc;
	}

	// Update a document
	private static String update(ObjectNode doc) {
		// pull the id from the document.  We'll need that for the PUT request
		String id = ((TextNode)doc.get("_id")).getTextValue();

		// initialize the PUT with the base URL and the document ID
		HttpPut httpPut = new HttpPut(baseUrl + id);

		// add your credentials to the request
		addAuth(httpPut);

		// initialize the rev we want to return
		String rev = "";

		try {
			// convert the doc back to a StringEntity
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mapper.writeValue(baos, doc);
		  	httpPut.setEntity(new StringEntity(baos.toString(), ContentType.APPLICATION_JSON));

		  	// send the request and read the response
		  	HttpResponse putResp = httpClient.execute(httpPut);
		  	ObjectNode putRespDoc = mapper.readValue(putResp.getEntity().getContent(), ObjectNode.class);
		  	rev = ((TextNode)putRespDoc.get("rev")).getTextValue();
		  	System.out.println("The revision after the update request is " + rev + ".");
		} catch (IOException e) {
			System.out.println("An error occurred while updating a document.");
			System.out.println(e.getMessage());
			System.exit(-1);			
		} finally {
			// release the connection
			httpPut.releaseConnection();
		}

	  	// return the rev of the updated doc
	  	return rev;
	}

	// Delete a document
	private static void delete(String id, String rev) {
		// initialize the DELETE with the base URL, id, and rev
	  	HttpDelete httpDelete = new HttpDelete(baseUrl + id + "?rev=" + rev);

	  	// add your credentials to the request
	  	addAuth(httpDelete);

	  	try {
		  	// send the request, read the response. and release the connection
		  	HttpResponse deleteResp = httpClient.execute(httpDelete);
		  	System.out.println("The response from the delete request is... ");
		  	System.out.println(deleteResp.toString());
		} catch (IOException e) {
			System.out.println("An error occurred while deleting a document.");
			System.out.println(e.getMessage());
			System.exit(-1);			
		} finally {
			// release the connection
			httpDelete.releaseConnection();
		}
	}
	
	public static void main(String[] args) {
		// read the user's credentials
		System.out.print("Enter your Cloudant username: ");
		user = System.console().readLine();
		System.out.print("Enter your Cloudant password: ");
		pass = new String(System.console().readPassword());
		System.out.print("Enter your database name: ");
		db = System.console().readLine();

		// initialize the base URL using the user's info
		baseUrl = "https://" + user + ":" + pass + "@" + user + ".cloudant.com/" + db + "/";

		// this is our example doc we want to create
		String newDoc = "{ \"name\": \"john\", \"age\": 35 }";

		// create a new document
		String id = create(newDoc);

		// read the document back
		ObjectNode doc = read(id);

		// update the doc and then commit to the database
	  	doc.put("age", 36);
	  	String rev = update(doc);

	  	// delete the document from the database
	  	delete(id, rev);					
	}
}
