var baseUrl = "https://" + user + ":" + pass + "@" + user + ".cloudant.com/" + db;

function errorHandler(jqXHR, textStatus, errorThrown) {
	console.log("something went wrong: " + textStatus + " " + errorThrown + " " + JSON.stringify(jqXHR));
}

$.ajax({
	url: baseUrl,
	type: "POST",
	contentType: "application/json",
	data: JSON.stringify({name: "john", age: 35}),
	error: errorHandler
}).done(function(data) {
	var json = JSON.parse(data);
	var id = json.id
	console.log("The new document's ID is " + id + ".");
	var docUrl = baseUrl + "/" + id;
	$.ajax({
		url: docUrl,
		type: "GET",
		error: errorHandler
	}).done(function(data) {
		var doc = JSON.parse(data);
		var rev1 = doc['_rev'];
		doc.age = 36;
		console.log("The first revision is " + rev1 + ".");
		$.ajax({
		  url: docUrl,
		  type: "PUT",
		  data: JSON.stringify(doc),
		  contentType: "application/json",
		  error:errorHandler
		}).done(function(data){
		  var putResp = JSON.parse(data);
		  var rev2 = putResp.rev;
		  console.log('The second revision is ' + rev2 + '.');
		  $.ajax({
				url: docUrl + '?rev=' + rev2,
				type: "DELETE",
				error:errorHandler
			}).done(function(data){
				console.log('Let\'s delete the document. The response is: ' + data);				
			});
		})
	})
});



