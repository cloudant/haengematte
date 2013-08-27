<?php
require 'vendor/autoload.php';

use Guzzle\Http\Client;

$user = getenv('user');
$pass = getenv('pass');
$db = getenv('db');
echo "$user $pass $db\n";

$baseUrl = "https://$user:$pass@$user.cloudant.com/$db";

$client = new Client($baseUrl);
$client->setDefaultOption('exceptions', false);
$putDbRequest = $client->put('');
$putDbResponse = $putDbRequest->send();
$doc = array('name' => 'john', 'age' => 35);
$postDocRequest = $client->post('', array("Content-Type" => "application/json"), json_encode($doc));
$postDocResponse = $postDocRequest->send();
$rev1 = json_decode($postDocResponse->getBody())->{'rev'};
$id = json_decode($postDocResponse->getBody())->{'id'};
echo "The new document's id is $id and the first revision is $rev1.\n";
$doc['age'] = 36;
$doc['_rev'] = $rev1;
$putDocRequest = $client->put("$id", array(), json_encode($doc));
$putDocResponse = $putDocRequest->send();
echo $putDocRequest;
echo $putDocResponse;
$rev2 = json_decode($putDocResponse->getBody())->{'rev'};
echo "The second revision is $rev2.\n";
echo "Now we will delete the document. This is the response we got:\n";
$deleteRequest = $client->delete("$id?rev=$rev2");
$deleteResponse = $deleteRequest->send();
echo $deleteResponse->getBody();

