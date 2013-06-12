// you'll need `request`. 
// Run `npm install request` to get it.
var request = require('request');

// establish an authenticated connection
var base_url = "https://"
              +process.env.user
              +":"
              +process.env.pass
              +"@"
              +process.env.user
              +".cloudant.com";

request.get(base_url, function(err, res, body){
  if(err){
    console.log("An error happened: ", err);
  }else{
    console.log("These are some details about our account: ", body);

    // create a database
    var db_url = [base_url, process.env.db].join('/');
    request.put(db_url, function(err, res, body){
      if(err){
        console.log("An error happened: ", err);
      }else{
        console.log("You just made a database: ", body);

        // create a document
        var doc_id = "test_doc"
        request.post({
          url: db_url,
          json: {
            _id: doc_id,
            good_life_advice: "Buy pizza. Pay with snakes."
          }
        }, function(err, res, body){
          if(err){
            console.log("An error happened: ", err);
          }else{
            console.log("\nYou just made a document: ", body);

            // get a document
            var doc_url = [db_url, doc_id].join('/')
              , doc = {};
            request.get(doc_url, function(err, res, body){
              if(err){
                console.log("An error happened: ", err);
              }else{
                console.log("\nHere's that document: ", body);
                // `request` will only consider the response body a JSON 
                // if you send a JSON in the request.
                doc = JSON.parse(body);

                // update a document
                doc.good_life_advice = "It's simple. Kill the Batman."
                request.put({
                  url: doc_url,
                  json: doc
                }, function(err, res, body){
                  if(err){
                    console.log("An error happened: ", err);
                  }else{
                    console.log("\nYou just changed a document: ", body);
                    doc._rev = body.rev; // keep our local doc up to date

                    // delete a document
                    request.del({
                      url: doc_url,
                      qs: {rev: doc._rev}
                    }, function(err, res, body){
                      if(err){
                        console.log("An error happened: ", err);
                      }else{
                        console.log("\nYou just deleted a document: ", body);
                        
                        // delete a database
                        request.del(db_url, function(err, res, body){
                          if(err){
                            console.log("An error happened: ", err);
                          }else{
                            console.log("\nYou just deleted a database: ", body);
                          }
                        });
                      }
                    });
                  }
                });
              }
            });
          }
        });
      }
    });
  }
});