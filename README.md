# VES Client Simulator
Simulator that generates VES events related to PNF PNP integration.

## Usage of simulator
### Setting up
Preferred way to start simulator is to use `docker-compose up -d` command.
All required docker images will be downloaded from ONAP Nexus, however there is a possibility to build those 
images locally. It can be achieved by invoking `mvn clean install -P docker` from top directory.
 
### API
Simulator provides REST endpoints which can be used to trigger sending events to VES.

*Periodic event sending*
To trigger sending use following endpoint *http://<simulator_ip>:5000/simulator/start*.  
Supported method: *POST*  
Headers:  
    - Content-Type - application/json  
Parameters: 
    
    simulatorParams:
        repeatCount    -  determines how many events will be sent
        repeatInterval -  time (in seconds) between events
        vesServerUrl   -  valid path to VES Collector
    templateName   -  name of template file (check *Templates* section) 
    patch           - part of event which will be merged into template
    variables    - correct json containing variables to merge with patched template

  
Sample Request:

    {
      "simulatorParams": {
        "repeatCount": 5,
        "repeatInterval": 2,
        "vesServerUrl": "http://VES-HOST:8080/eventListener/v7"
      },
      "templateName": "validExampleMeasurementEvent.json",
      "patch": {
                   "event": {
                       "commonEventHeader": {
                           "eventId": "PATCHED_eventId",
                           "sourceName": "PATCHED_sourceName",
                           "version": 3.0
                       }
                   }
               },
       "variables": {
                    "dn":"Abcd",
                    "anyObject": {
                        "key": "value"
                    }
       }        
    }
    
*One-time event sending*
Enables direct, immediate event sending without need to have template deployed on backend.
Keywords are supported,thus once passed, will also be substituted with proper strings. 
Passed event body must be valid and complete event according to VES Collector interface. 
To trigger sending use following endpoint *http://<simulator_ip>:5000/simulator/event*.
After sending event, response message from VES will be passed as response message from Simulator.
Thanks to that when sending one-time event user will receive information about request.
This is helpful when authentication fail or VES response with "Forbidden" when using http instead of https.
In a situation when given URL address is not pointing to VES, Simulator response with status ```421```
and information about communication problem.   

Supported method: *POST*  
Headers:  
    - Content-Type - application/json  
Parameters: 
    
    vesServerUrl   -  valid URL to VES Collector event listener
    event          -  body of event to be sent directly to VES Collector (it can contain keyword expressions)

  
Sample Request:

    {
      "vesServerUrl": "http://VES-HOST:8080/eventListener/v7",
      "event": {
        "commonEventHeader": {
          "eventId": "#RandomString(20)",
          "sourceName": "PATCHED_sourceName",
          "version": 3.0
        }
      }
    }
    
### Changing simulator configuration
Utility of default configuration has been introduced so as to facilitate sending requests. so far only vesServerUrl states default simulator configuration.
On simulator startup, vesServerUrl is initialized with default value, but must be replaced with correct VES server url by user.
Once vesServerUrl is properly set on simulator, this parameter does not need to be incorporated into every trigger event request.
If user does not provide vesServerUrl in trigger request, default value will be used.
If use does provide vesServerUrl in trigger request, then passed value will be used instead of default one (default value will not be overwritten by provided one).

It is possible to get and update configuration (current target vesServerUrl) using offered REST API - */simulator/config* endpoint is exposed for that.
To get current configuration *GET* method must be used.
To update vesServerUrl *PUT* method is used, example request: 

    {
      "vesServerUrl": "http://10.154.164.117:8080/eventListener/v7"
    }
  
Note: passed vesServerUrl must be wellformed URL.

### Templates
Template is a draft event. Merging event with patch will result in valid VES event. Template itself should be a correct VES event as well as valid json object. 
In order to apply custom template, just copy it to ./templates directory.
*notification.json* and *registration.json* are available by default in *./templates* directory.

#### Template management
The simulator provides means for managing templates. Supported actions: adding, editing (overriding) and deleting are available via HTTP endpoint */template*

```GET /template/list```  
Lists all templates known to the simulator.

```GET /template/get/{name}```  
Gets template content based on *name* path variable.

```POST /template/upload?override=true```  
Saves template content under *name* path variable. The non-mandatory parameter *override* allows overwriting an existing template.

Sample payload:
```
{
  "name": "someTemplate",
  "template": {
    "commonEventHeader": {
      "domain": "notification",
      "eventName": "vFirewallBroadcastPackets"
    },
    "notificationFields": {
      "arrayOfNamedHashMap": [{
        "name": "A20161221.1031-1041.bin.gz",
        "hashMap": {
          "fileformatType": "org.3GPP.32.435#measCollec"
        }
      }]
    }
  }
}
```

### Searching for key-value conditions in stored templates
Simulator allows to search through stored templates and retrieve names of those that satisfy given criteria passed in form of key-value pairs (See examples below).
Following data types are supported in search as values:
-integer
-string
-double
-boolean
Searching for null values as well as incorporating regex expression with intention to find a match is not supported.
Search expression must be a valid JSON, thus no duplicated keys are allowed - user could specify the same parameter multiple times, but only last occurrence will be applied to query.
Example search expression:

{"domain": "notification", "sequence": 1, "startEpochMicrosec": 1531616794, "sampleDouble": 2.5}

will find all templates that contain all of passed key-value entries. There is an AND condition beetwen given criteria - all of them must be satisfied to qualify template as matching item.
 Keys of search expressions are searched in case insensitive way as well as string values.
Where it comes to values of numerical and boolean type exact match is expected.

API usage:

```POST /template/search```
Produces query that returns templates that contain searched criteria

Sample payload:
```
{
  "searchExpr": {
    "domain": "notification",
    "sequence": 1,
    "startEpochMicrosec": 1531616794,
    "sampleDouble": 2.5
    }
}
```
Sample response:
```
[notification.json]
```
 

Note: Manually deployed templates, or actually existing ones, but modified inside the templates catalog '/app/templates', will be automatically synchronized with schemas stored inside the database.  That means that a user can dynamically change the template content using vi editor at simulator container, as well as use any editor at any machine and then push the changes to the template folder. All the changes will be processed 'on the fly' and accessible via the rest API.

### Periodic events
Simulator has ability to send event periodically. Rest API support parameters:
* repeatCount - count of times that event will be sent to VES
* repeatInterval - interval (in second) between two events.
(Checkout example to see how to use them)

### Patching
User is able to provide patch in request, which will be merged into template.  

Warning: Patch should be a valid json object (no json primitives nor json arrays are allowed as a full body of patch).

This mechanism allows to override part of template. 
If in "patch" section there are additional parameters (absent in template), those parameters with values will be added to event.
Patching mechanism supports also keywords that enables automatic value generation of appropriate type

### Keyword support
Simulator supports corresponding keywords:
- \#RandomInteger(start,end) - substitutes keyword with random positive integer within given range (range borders inclusive)
- \#RandomPrimitiveInteger(start,end) - the same as #RandomInteger(start,end), but returns long as result
- \#RandomInteger -  substitutes keyword with random positive integer
- \#RandomString(length) - substitutes keyword with random ASCII string with specified length
- \#RandomString - substitutes keyword with random ASCII string with length of 20 characters
- \#Timestamp - substitutes keyword with current timestamp in epoch (calculated just before sending event)
- \#TimestampPrimitive - the same as \#Timestamp, but returns long as result
- \#Increment - substitutes keyword with positive integer starting from 1 - for each consecutive event, value of increment property is incremented by 1

Additional hints and restrictions:
All keywords without 'Primitive' in name return string as result. To specify keyword with 2 arguments e.g. #RandomInteger(start,end) no whitespaces between arguments are allowed.
Maximal value of arguments for RandomInteger is limited to the java integer range. Minimal is always 0. (Negative values are prohibited and wont be treated as a correct parts of keyword).
RandomInteger with parameters will automatically find minimal and maximal value form the given attributes so no particular order of those is expected.    

How does it work?
When user does't want to fill in parameter values that are not relevant from user perspective but are mandatory by end system, then keyword feature should be used.
In template, keyword strings are substituted in runtime with appropriate values autogenerated by simulator. 
Example can be shown below:

Example template with keywords:
  
    {
      "event": {
        "commonEventHeader": {
          "eventId": "123#RandomInteger(8,8)",
          "eventType": "pnfRegistration",
          "startEpochMicrosec": "#Timestamp",
          "vesEventListenerVersion": "7.0.1",
          "lastEpochMicrosec": 1539239592379
        },
        "pnfRegistrationFields": {
          "pnfRegistrationFieldsVersion":"2.0",
          "serialNumber": "#RandomString(7)",
          "vendorName": "Nokia",
          "oamV4IpAddress": "val3",
          "oamV6IpAddress": "val4"
        }
      }
    }

Corresponding result of keyword substitution (event that will be sent):
  
    {
      "event": {
        "commonEventHeader": {
          "eventId": "1238",
          "eventType": "pnfRegistration",
          "startEpochMicrosec": "154046405117",
          "vesEventListenerVersion": "7.0.1",
          "lastEpochMicrosec": 1539239592379
        },
        "pnfRegistrationFields": {
          "pnfRegistrationFieldsVersion":"2.0",
          "serialNumber": "6061ZW3",
          "vendorName": "Nokia",
          "oamV4IpAddress": "val3",
          "oamV6IpAddress": "val4"
        }
      }
    }
 
### In place variables support
Simulator supports dynamic keywords e.g. #dN to automatically substitute selected phrases in defined json schema.
Keywords have to be specified as separate json values, so no mixing keywords inside textual fields are acceptable. Current implementation 
supports placing variables in json templates as well as in patches latter sent as part of the requests.

####Example:

Request:
```json
{
  "simulatorParams": {
    "repeatCount": 1,
    "repeatInterval": 1,
    "vesServerUrl": "http://ves:5123"
  },
  "templateName": "cmNotification.json",
  "patch": {},
  "variables": {
    "dN": "NRNB=5, NRCEL=1234",
    "attributeList": {
      "threshXHighQ": "50",
      "threshXHighP": "52"
    }
  }
}
``` 

cmNotification.json template is installed automatically after startup of the simulator but can be found also in repository in 'templates' folder:
```json
{
  "event": {
    "otherFields": {
      "otherFieldsVersion": "3.0",
      "jsonObjects": [
        {
          "objectName": "CustomNotification",
          "objectInstances": [
            {
              "objectInstance": {
                "cm3gppNotifyFields": {
                  "dN": "#dN",
                  "notificationType": "notifyMOIAttributeValueChange",
                  "notificationId": "notificationID123121312323",
                  "sourceIndicator": "sONOperation",
                  "eventTime": "#Timestamp",
                  "systemDN": "NRNB=5",
                  "attributeList": "#attributeList",
                  "correlatedNotifications": {
                    "notificationID-notifyMOIAttributeValueChange": "sONOperation"
                  },
                  "additionalText": "sometext",
                  "cm3gppNotifyFieldsVersion": "1.0"
                }
              }
            }
          ]
        }
      ]
    }
  }
}
```

Expected output of such request (body of an event being send to a ves) should be as follows:
```json
{
	"event": {
		"otherFields": {
			"otherFieldsVersion": "3.0",
			"jsonObjects": [{
				"objectName": "CustomNotification",
				"objectInstances": [{
					"objectInstance": {
						"cm3gppNotifyFields": {
							"dN": "NRNB=5, NRCEL=1234",
							"notificationType": "notifyMOIAttributeValueChange",
							"notificationId": "notificationID123121312323",
							"sourceIndicator": "sONOperation",
							"eventTime": "1571306716",
							"systemDN": "NRNB=5",
							"attributeList": {
								"threshXHighQ": "50",
								"threshXHighP": "52"
							},
							"correlatedNotifications": {
								"notificationID-notifyMOIAttributeValueChange": "sONOperation"
							},
							"additionalText": "sometext",
							"cm3gppNotifyFieldsVersion": "1.0"
						}
					}
				}]
			}]
		}
	}
}
```

### Logging
Every start of simulator will generate new logs that can be found in docker pnf-simualtor container under path: 
/var/log/ONAP/pnfsimulator/pnfsimulator_output.log

### Swagger
Detailed view of simulator REST API is available via Swagger UI
Swagger UI is available on *http://<simulator_ip>:5000/swagger-ui.html*

### History
User is able to view events history.  
In order to browse history, go to *http://<simulator_ip>:8081/db/pnf_simulator/eventData*

### TLS Support
Simulator is able to communicate with VES using HTTPS protocol.
CA certificates are incorporated into simulator docker image, thus no additional actions are required from user.

Certificates can be found in docker container under path: */usr/local/share/ca-certificates/*

Simulator works with VES that uses both self-signed certificate (already present in keystore) and VES integrated to AAF.

Certification loading can be disabled by setting environment variable ```USE_CERTIFICATE_FOR_AUTHORIZATION``` to false. 
Once certificate are not used for authorization, user can set up VES url using username and password.

    {
        "vesServerUrl": "http://<user>:<password>@<ves_url>:<port>/eventListener/v7"
    }

## Developers Guide

### Integration tests
Integration tests are located in folder 'integration'. Tests are using docker-compose from root folder. 
This docker-compose has pnfsimulator image set on nexus3.onap.org:10003/onap/pnf-simulator:1.0.1-SNAPSHOT. 
To test your local changes before running integration tests please build project using:

    'mvn clean install -P docerk'
    
then go to 'integration' folder and run: 

    'mvn test'
    
### Client certificate authentication
Simulator can cooperate with VES server in different security types in particular ```auth.method=certBasicAuth``` which means that it needs to authenticate using client private certificate. 

Warning: according to VES implementation which uses certificate with Common Name set to DCAELOCAL we decided not to use strict hostname verification, so at least this parameter is skipped during checking of the client certificate.

#### How to generate client correct keystore for pnf-simulator
 The Root CA cert is available in certs folder in VES repository. The password for rootCA.key is collector.
 
 The procedure of generating client's certificate:
 1. Generate a private key for the SSL client: ```openssl genrsa -out client.key 2048```
 2. Use the client’s private key to generate a cert request: ```openssl req -new -key client.key -out client.csr```
 3. Issue the client certificate using the cert request and the CA cert/key: ```openssl x509 -req -in client.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial -out client.crt -days 500 -sha256```
 4. Convert the client certificate and private key to pkcs#12 format: ```openssl pkcs12 -export -inkey client.key -in client.crt -out client.p12```
 5. Copy pkcs file into pnf simulators folder: ```/app/store/```
 
#### How to generate correct truststore for pnf-simulator
 Create truststore with rootCA.crt: 
 1. ```keytool -import -file rootCA.crt -alias firstCA -keystore trustStore```
 2. Copy truststore to ```/app/store/```

#### Testing keystore with real/mocked ves server
```curl --cacert rootCA.crt --cert client.crt --key client.key https://VES_SECURED_URL -d "{}" -X POST -H "Content-type: application/json" -kv```

#### How to refresh configuration of app
Depending on your needs, you are able to change client certificate, replace trustStore to accept new server certificate change keystore and truststore passwords or completely disable client cert authentication.

For this purpose:
1. Go to the pnf simulator container into the /app folder.
2. If you want to replace keystore or truststore put them into the /app/store folder.
3. Edit /app/application.properties file as follow:
- ssl.clientCertificateEnabled=true (to disable/enable client authentication)
- ssl.strictHostnameVerification=true (to disable/enable hostname verification)
- ssl.clientCertificatePath=/app/store/client.p12 (to replace with keystore file)
- ssl.clientCertificatePasswordPath=/app/store/keystore.pass (to replace with keystore password file)
- ssl.trustStorePath=/app/store/trustStore (to replace with truststore file)
- ssl.trustStorePasswordPath=/app/store/truststore.pass (to replace with truststore password file)
4. Refresh configuration by sending simple POST request to correct actuator endpoint at: ```curl http://localhost:5001/refresh -H 'Content-type: application/json' -X POST --data '{}'```
