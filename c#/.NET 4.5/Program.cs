using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace haengematte
{
    class Program
    {
        static void Main(string[] args)
        {
            //read config data from a command line parameters
            var user = args[0];
            var password = args[1];
            var database = args[2];

            //base request builder for all requests containing user name and database name
            var handler = new HttpClientHandler { Credentials = new NetworkCredential(user, password) };

            using (var client = CreateHttpClient(handler, user, database))
            {
                var creationResponse = Create(client, new {name = "john", age = 15});
                PrintResponse(creationResponse);

                var id = GetString("id", creationResponse);
                var readResponse = Read(client, id);
                PrintResponse(readResponse);

                var rev1 = GetString("_rev", readResponse);
                var updateResponse = Update(client, id, new {name = "john", age = 36, _rev = rev1});
                PrintResponse(updateResponse);
                
                var rev2 = GetString("rev", updateResponse); // note that an update produces a "rev" in the response rather than "_rev"
                var deleteResponse = Delete(client, id, rev2);
                PrintResponse(deleteResponse);
            }
        }

        private static HttpResponseMessage Create(HttpClient client, object doc)
        {
            var json = JsonConvert.SerializeObject(doc);
            return client.PostAsync("", new StringContent(json, Encoding.UTF8, "application/json")).Result;
        }

        private static HttpResponseMessage Read(HttpClient client, string id)
        {
            return client.GetAsync(id).Result;
        }

        private static HttpResponseMessage Update(HttpClient client, string id, object doc)
        {
            var json = JsonConvert.SerializeObject(doc);
            return client.PutAsync(id, new StringContent(json, Encoding.UTF8, "application/json")).Result;
        }

        private static HttpResponseMessage Delete(HttpClient client, string id, string rev)
        {
            return client.DeleteAsync(id + "?rev=" + rev).Result;
        }

        private static HttpClient CreateHttpClient(HttpClientHandler handler, string user, string database)
        {
            return new HttpClient(handler)
            {
                BaseAddress = new Uri(string.Format("https://{0}.cloudant.com/{1}/", user, database))
            };
        }

        private static void PrintResponse(HttpResponseMessage response)
        {
            Console.WriteLine("Status code: {0}", response.StatusCode);
            Console.WriteLine(Convert.ToString(response));
        }
        
        private static string GetString(string propertyName, HttpResponseMessage creationResponse)
        {
            using (var streamReader = new StreamReader(creationResponse.Content.ReadAsStreamAsync().Result))
            {
                var responseContent = (JObject) JToken.ReadFrom(new JsonTextReader(streamReader));
                return responseContent[propertyName].Value<string>();
            }
        }
    }
}
