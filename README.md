# ccd-case-document-am-api

[![API v1](https://img.shields.io/badge/API%20Docs-v1-e140ad.svg)](https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/document-management-store-app.json)
[![Build Status](https://travis-ci.org/hmcts/ccd-case-document-am-api.svg?branch=master)](https://travis-ci.org/github/hmcts/ccd-case-document-am-api)
[![Docker Build Status](https://img.shields.io/docker/build/hmcts/ccd-case-document-am-api.svg)](https://hub.docker.com/r/hmcts/ccd-case-document-am-api)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Purpose

The purpose of this application is to act as a proxy document management service to facilitate following access controls on case documents:

1) Applying CCD's configured access control policies to the upload of CCD case documents.

2) Applying CCD's configured access control policies to the download of CCD case documents.

3) Protecting against unauthorised access to CCD case documents while being uploaded to, stored in, and downloaded from the case document repository. In both cases, access control will be applied as if the document were a 'standard' item of case data contained within the CCD case data store.

Users & services with sufficient permissions only will be able to upload, modify, delete and download documents.

### Prerequisites

- [Open JDK 8](https://openjdk.java.net/)
- [Docker](https://www.docker.com)

This service works with the DocStore Api and CaseData Api alongside their databases CCD Data Store and Document Management Store.

#### Environment variables
The following environment variables are required:

| Name | Default | Description |
|------|---------|-------------|
      |CASE_DOCUMENT_S2S_AUTHORISED_SERVICES| ccd_case_document_am_api, ccd_gw, xui_webapp, ccd_data, bulk_scan_processor|
      |REFORM_SERVICE_NAME| ccd-case-document-am-api|
      |REFORM_TEAM| ccd
      |REFORM_ENVIRONMENT| local
      |S2S_SECRET|
      |S2S_KEY| S2S_KEY
      |CCD_DOCUMENT_API_IDAM_KEY|
      |DEFINITION_STORE_HOST|
      |USER_PROFILE_HOST|
      |DM_STORE_BASE_URL| http://dm-store:8080|
      |CCD_DATA_STORE_API_BASE_URL| http://ccd-data-store-api:4452|
      |AZURE_APPLICATIONINSIGHTS_INSTRUMENTATIONKEY|
      |IDAM_USER_URL| http://idam-api:5000 |
      |IDAM_S2S_URL| http://service-auth-provider-api:8080|
      |JAVA_TOOL_OPTIONS| -XX:InitialRAMPercentage=30.0 -XX:MaxRAMPercentage=65.0 -XX:MinRAMPercentage=30.0 -XX:+UseConcMarkSweepGC -agentlib:jdwp=transport=dt_socket, server=y,suspend=n,address=5005

## Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```
To clean up your environment use the following, it will delete any temporarily generated files such as reports.

```bash
  ./gradlew clean
```
### Running

If you want your code to become available to other Docker projects (e.g. for local environment testing), you need to build the image:

```bash
docker-compose build
```

When the project has been packaged in `target/` directory, 
you can run it by executing following command:

```bash
docker-compose up
```

As a result the following containers will get created and started:

 - API exposing port `4455`

## Endpoints

Authorization and ServiceAuthorization (S2S) tokens are required in the headers for all endpoints. All APIs are authorised with some service level permissions captured in the the configurables rules under service_config.json file (https://github.com/hmcts/ccd-case-document-am-api/blob/readme_update/src/main/resources/service_config.json). 

```
GET /cases/documents/{documentId}
```
- Retrieves json representation of the document metadata from doc-store. 
```
GET /cases/documents/{documentId}/binary
```
- Streams contents of the most recent Document Content Version associated with the Stored Document. 
```
GET /cases/documents/{documentId}/token
```
- Returns the hashed token required for document upload functionality. Initially this API is reserved only for the bulk_scan_processor service.
```
POST /cases/documents
```
- Used for uploading any case related documents to doc-store.

        Also requires a request body containing
        - classification {string}
        - files {multipart/form-data}
        - caseTypeId {string}
        - jurisdictionId {string}
```
PATCH /cases/documents/{documentId}
```
- Used to update the TTL(time to live) value for any case related document in doc-store. 

        Also requires a request body containing
        - ttl {string}
```
PATCH /cases/documents/attachToCase
```
- Will be exposed only for ccd-data-store application and utilised in a service to service call for attaching documents to their corresponding case while submitting case create/update with document.

        Also requires a request body containing
        - CaseDocumentMetadata {objects}
```
DELETE /cases/documents/{documentId}
```
- Will delete any case related documents from doc-store

        Also requires a request param for
        - permanent {boolean}

### Functional Tests
The functional tests are located in `functionalTest` folder. These are the tests run against an environment. For example if you would 
like to test your local environment you'll need to export the following variables on your `.bash_profile` script.


```bash
#Smoke/Functional Tests
export BEFTA_S2S_CLIENT_ID=ccd_gw
export BEFTA_S2S_CLIENT_SECRET=AAAAAAAAAAAAAAAC
export BEFTA_RESPONSE_HEADER_CHECK_POLICY=JUST_WARN
export CASE_DOC_AM_URL=http://localhost:4455
export DM_STORE_URL=http://localhost:4506
export CCD_BEFTA_CITIZEN_2_PWD=Pa55word11
export CCD_BEFTA_CASEWORKER_2_SOLICITOR_1_PWD=Pa55word11
export DM_STORE_BASE_URL=http://localhost:4506
export BEFTA_S2S_CLIENT_ID_OF_CCD_DATA=ccd_data
export BEFTA_S2S_CLIENT_SECRET_OF_CCD_DATA=AAAAAAAAAAAAAAAB
export CASE_DOCUMENT_AM_URL=http://localhost:4455
export BEFTA_S2S_CLIENT_ID_OF_BULK_SCAN_PROCESSOR=bulk_scan_processor
export BEFTA_S2S_CLIENT_SECRET_OF_BULK_SCAN_PROCESSOR=AAAAAAAAAAAAAAAA
export CCD_DATA_STORE_API_BASE_URL=http://localhost:4452
export CCD_DM_DOMAIN=http://localhost:4455
export BEFTA_S2S_CLIENT_ID_OF_XUI_WEBAPP=xui_webapp
export BEFTA_S2S_CLIENT_SECRET_OF_XUI_WEBAPP=AAAAAAAAAAAAAAAA
export DM_STORE_BASE_URL=http://localhost:4506
```

These tests also rely on the `CCD_BEFTA_JURISDICTION2.xlsx` file to be already imported. This file should be available in your local environment already.

####Running the tests

In order to run the tests you will need to pull down ```ccd-docker``` repo and checkout the ```AM-CCD_Docker_Custom_Setup_For_Case_Document_API``` branch.

Then pull down ```ccd-case-document-utilities``` repo and update the following lines in ```env-main.sh``` to match your local file structure.
```
###### Please provide Docker project location please
DOCKER_REPO='/Users/{username}/HMCTS-Projects/AM/ccd-docker'
###### Please provide "ccd-case-document-utilities/auto-environment/idam-ui-automation" location
WEB_REPO='/Users/{username}/HMCTS-Projects/AM/ccd-case-document-utilities/auto-environment/idam-ui-automation'
###### Please provide "ccd-case-document-utilities/auto-environment/bin" location
CURRENT_LOCATION='/Users/736062/HMCTS-Projects/AM/ccd-case-document-utilities/auto-environment/bin'
```

Run the scripts that follow.
Once this is done, try to run your functional tests.

## LICENSE

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.
