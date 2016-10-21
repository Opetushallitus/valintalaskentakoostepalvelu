/*
* Mongo script to retrieve the results to migrate directly from hakemuslomake mongo.
* This is way easier than adding a new REST API to haku-app just for that...
* The results then need to be stored in ammatillisenKielikoeSuorituksetHakuAppista.json ,
* from where the migration code reads them.
*
* The script can be run e.g. as follows:
*
* # Assuming you have a connection to hakemus mongo ssh-piped to localhost:47017
*
*  export HAKEMUSMONGO_PASSWORD=<password here>
*  time mongo -p $HAKEMUSMONGO_PASSWORD --quiet --authenticationDatabase admin -u oph  localhost:47017/hakulomake haeAmmatillisenKielikoeSuorituksetHakemusMongosta.js > ammatillisenKielikoeSuorituksetHakuAppista.json
*
*  Ensure that the resulting file is valid JSON with reasonable content -- for instance,
*  if you get "Cannot use 'commands' readMode, degrading to 'legacy' mode", it must be
*  cleaned up from the start of the file by hand.
* */

db.getMongo().setSlaveOk();

const projection = { _id: 0,
                     "oid": 1,
                     "additionalInfo.kielikoe_fi":1,
                     "additionalInfo.kielikoe_sv":1,
                     "additionalInfo.kielikoe_fi-OSALLISTUMINEN":1,
                     "additionalInfo.kielikoe_sv-OSALLISTUMINEN":1 };

const result = db.application.find(
  {$or: [ 
           { $and: [
               { "additionalInfo.kielikoe_sv": { $exists: true } },
               { "additionalInfo.kielikoe_sv": { $ne: "" } }
             ]
           },
           { $and: [
               { "additionalInfo.kielikoe_fi": { $exists: true } },
               { "additionalInfo.kielikoe_fi": { $ne: "" } }
             ]
           }
        ]
  }, projection).toArray();

printjson(result);
