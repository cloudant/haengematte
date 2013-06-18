#include <stdio.h>
#include <curl/curl.h>
#include <json.h>
#include <string.h>

char buffer[100000];
int offset;
int putBodyOffset = 0;
const char *putBody;

size_t writeFunction(char *ptr, size_t size, size_t nmemb, void *userdata) {
  memcpy(&(buffer[offset]), ptr, size * nmemb);
  offset += size * nmemb;
  return size * nmemb;
}

size_t readFunction(void *ptr, size_t size, size_t nmemb, void *stream) {
  int len = strlen(putBody);
  memcpy(ptr, putBody, len);
  return len;
}
 
int main(void) {
  //global init
  char *user = getenv("user");
  char *pass = getenv("pass");
  char *db = getenv("db");
  CURL *curl;
  CURLcode res;
  curl_global_init(CURL_GLOBAL_ALL);
  char dbUrl[4096];
  snprintf(dbUrl, sizeof(dbUrl),"http://%s:%s@%s.cloudant.com/%s/", user, pass, user, db);
  //post
  offset = 0;
  curl = curl_easy_init();
  if (!curl) exit(1);
  struct curl_slist *headers = NULL;
  headers = curl_slist_append(headers, "Content-Type: application/json");
  char *doc = "{ \"name\": \"john\", \"age\": 35 }";
  long docLen = strlen(doc);
  curl_easy_setopt(curl, CURLOPT_POSTFIELDS, doc);
  curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, docLen);
  curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
  curl_easy_setopt(curl, CURLOPT_URL, dbUrl);
  curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, &writeFunction);
  res = curl_easy_perform(curl);
  if (res != CURLE_OK) {
    fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
  }
  curl_easy_cleanup(curl);
  json_object *jo = json_tokener_parse(buffer);
  const char *id = json_object_get_string(json_object_object_get(jo, "id"));
  printf("The new document's ID is %s.\n", id);

  //get
  curl = curl_easy_init();
  if (!curl) exit(1);
  char docUrl[4096];
  snprintf(docUrl, sizeof(docUrl),"%s%s", dbUrl, id);
  json_object_put(jo);
  curl_easy_setopt(curl, CURLOPT_URL, docUrl);
  curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, &writeFunction);
  offset = 0;
  res = curl_easy_perform(curl);
  if (res != CURLE_OK) {
    fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
  }
  curl_easy_cleanup(curl);
  jo = json_tokener_parse(buffer);
  const char *rev1 = json_object_get_string(json_object_object_get(jo, "_rev"));
  printf("The first revision is %s.\n", rev1);

  //put
  curl = curl_easy_init();
  if (!curl) exit(1);
  curl_easy_setopt(curl, CURLOPT_URL, docUrl);
  curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, &writeFunction);
  offset = 0;
  json_object_object_del(jo, "age");
  json_object *age = json_object_new_int(36);
  json_object_object_add(jo, "age", age);
  putBody = json_object_get_string(jo);
  long putBodyLen = strlen(putBody);
  curl_easy_setopt(curl, CURLOPT_READFUNCTION, &readFunction);
  curl_easy_setopt(curl, CURLOPT_INFILESIZE, putBodyLen);
  curl_easy_setopt(curl, CURLOPT_PUT, 1);
  curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
  res = curl_easy_perform(curl);
  buffer[offset] = '\0';
  if (res != CURLE_OK) {
    fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
  }
  json_object_put(jo);
  jo = json_tokener_parse(buffer);
  const char *rev2 = json_object_get_string(json_object_object_get(jo, "rev"));
  printf("The second revision is %s.\n", rev2);
  curl_easy_cleanup(curl);
  //delete
  char deleteUrl[4096];
  snprintf(deleteUrl, sizeof(deleteUrl),"%s?rev=%s", docUrl, rev2);
  json_object_put(jo);
  curl = curl_easy_init();
  if (!curl) exit(1);
  curl_easy_setopt(curl, CURLOPT_URL, deleteUrl);
  curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, &writeFunction);
  curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, "DELETE");
  offset = 0;
  puts("Now we'll try to delete the document. This is the response: ");
  res = curl_easy_perform(curl);
  buffer[offset] = '\0';
  if (res != CURLE_OK) {
    fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
  }
  puts(buffer);
  curl_easy_cleanup(curl);
  curl_global_cleanup();
  return 0;
}


