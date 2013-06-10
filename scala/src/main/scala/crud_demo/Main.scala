package crud_demo

import scala.io.Source.fromFile
import dispatch._
import dispatch.Defaults._
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._
import com.ning.http.client.Response

object Main extends App {
  //read config data from a file with 3 lines: user name, password, and database name
  val List(user,pass,db) = fromFile("config").getLines.toList
  //base request builder for all requests containing user name and database name 
  def baseRb = host(s"${user}.cloudant.com").secure.as_!(user, pass) / db
  //function to parse the response
  val parseResponse = (resp:Response) => resp.getStatusCode -> parse(resp.getResponseBody).asInstanceOf[JObject]
  
  def create(doc:JObject) = {
    val rb = baseRb
      .POST //use POST to create documents
      .setBody(compact(render(doc))) //set the json document as the request body
      .setHeader("Content-Type", "application/json") > //set the content type http header
      parseResponse //add a function that processes the response
    Http(rb).apply  //execute the request
  }
  
  def read(id:String) = {
    val rb = (baseRb / id) > parseResponse
    Http(rb).apply
  }
  
  def update(id:String, doc:JObject) = {
    val rb = (baseRb / id)
      .PUT
      .setBody(compact(render(doc)))
      .setHeader("Content-Type", "application/json") >
      parseResponse
	  Http(rb).apply
  }
  
  def delete(id:String, rev:String) = {
    val rb = (baseRb / id)
      .DELETE
      .addQueryParameter("rev", rev) > 
      parseResponse
	  Http(rb).apply
  }
  
  def printResponse(resp:(Int, JObject)) {
    val (status, body) = resp
    println(s"Status code: ${status}")
    println(compact(render(body)))
  }
  
  def getStr(field:String, o:JObject) = {
    val JString(rev) = o \\ field
    rev
  }
  
  val creationResponse = create(("name" -> "john") ~ ("age" -> 35))
  printResponse(creationResponse)
  val id = getStr("id", creationResponse._2)
  val readResponse = read(id)
  printResponse(readResponse)
  val rev1 = getStr("_rev", readResponse._2)
  val updateResponse = update(id, ("name" -> "john") ~ ("age" -> 36) ~ ("_rev" -> rev1))
  printResponse(updateResponse)
  val rev2 = getStr("rev", updateResponse._2)
  printResponse(delete(id, rev2))
}