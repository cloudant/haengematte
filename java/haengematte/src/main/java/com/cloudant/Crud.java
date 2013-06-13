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
	
	private static String user, pass, db;
	
	private static void addAuth(HttpRequest req) {
		final String encodedUserPass = new String(Base64.encodeBase64((user + ":" + pass).getBytes()));
		req.setHeader("Authorization", "Basic " + encodedUserPass);
		
	}
	
	public static void main(String[] args) throws Exception {
		user = System.getenv().get("user");
		pass = System.getenv().get("pass");
		db = System.getenv().get("db");
		final String baseUrl = "https://" + user + ":" + pass + "@" + user + ".cloudant.com/" + db + "/";
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpPost httpPost = new HttpPost(baseUrl);
		final HttpEntity entity = new StringEntity("{ \"name\": \"john\", \"age\": 35 }", ContentType.APPLICATION_JSON);
		httpPost.setEntity(entity);
		addAuth(httpPost);
		final HttpResponse postResp = httpClient.execute(httpPost);
		final InputStream is = postResp.getEntity().getContent();
		final ObjectMapper mapper = new ObjectMapper();
	  final ObjectNode postRespDoc = mapper.readValue(is, ObjectNode.class);
	  final String id = ((TextNode)postRespDoc.get("id")).getTextValue();
	  System.out.println("The new document's ID is " + id + ".");
	  httpPost.releaseConnection();
	  final HttpGet httpGet = new HttpGet(baseUrl + id);
	  addAuth(httpGet);
	  final HttpResponse getResp = httpClient.execute(httpGet);
	  final ObjectNode doc1 = mapper.readValue(getResp.getEntity().getContent(), ObjectNode.class);
	  final String rev1 = ((TextNode)doc1.get("_rev")).getTextValue();
	  httpGet.releaseConnection();
	  System.out.println("The first revision is " + rev1 + ".");
	  final HttpPut httpPut = new HttpPut(baseUrl + id);
	  addAuth(httpPut);
	  doc1.put("_rev", rev1);
	  doc1.put("age", 36);
	  final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	  mapper.writeValue(baos, doc1);
	  final String doc2Str = baos.toString();
	  final HttpEntity doc2Entity = new StringEntity(doc2Str, ContentType.APPLICATION_JSON);
	  httpPut.setEntity(doc2Entity);
	  final HttpResponse putResp = httpClient.execute(httpPut);
	  final ObjectNode putRespDoc = mapper.readValue(putResp.getEntity().getContent(), ObjectNode.class);
	  final String rev2 = ((TextNode)putRespDoc.get("rev")).getTextValue();
	  httpPut.releaseConnection();
	  System.out.println("The second revision is " + rev2 + ".");
	  final HttpDelete httpDelete = new HttpDelete(baseUrl + id + "?rev=" + rev2);
	  addAuth(httpDelete);
	  final HttpResponse deleteResp = httpClient.execute(httpDelete);
	  httpGet.releaseConnection();
	  System.out.println("Now we will delete the document. The response is... ");
	  System.out.println(deleteResp.toString());		
	}
}
