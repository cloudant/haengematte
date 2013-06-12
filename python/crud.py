import json
import os

import requests

username = os.environ['user']
password = os.environ['pass']
db_name = os.environ['db']

baseUri = "https://{0}.cloudant.com/{1}".format(username, db_name)

creds = (username, password)

response = requests.put(
    baseUri,
    auth=creds
)
print "Created database at {0}".format(baseUri)

response = requests.post(
    baseUri,
    data=json.dumps({
        "name": "John", "age": 35
    }),
    auth=creds,
    headers={"Content-Type": "application/json"}
)
docId = response.json()["id"]
print "The new document's ID is {0}".format(docId)

response = requests.get(
    "{0}/{1}".format(baseUri, docId),
    auth=creds
)
doc = response.json()
print "The document's rev is {0}".format(doc["_rev"])

doc['age'] = 36
response = requests.put(
    "{0}/{1}".format(baseUri, docId),
    data=json.dumps(doc),
    auth=creds
)
rev2 = response.json()['rev']
print "The document's new rev is {0}".format(rev2)

print "Deleting document and database"
response = requests.delete(
    "{0}/{1}".format(baseUri, docId),
    params={"rev": rev2},
    auth=creds
)
print " > doc: ", response.json()

response = requests.delete(
    baseUri,
    auth=creds
)
print " > db: ", response.json()
