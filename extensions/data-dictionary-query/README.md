# Data Dictionary Query Method

This is the library for the Data Dictionary Query system.

## What is "Data Dictionary Query"?

Data Dictionary Query is a methodology and a toolkit to create a data API layer that allows you to define
data boundary.

This technology is not designed as a "Data Access" system. Instead, it is designed to align data boundary
between domains. You can move one data dictionary item from one domain to another as data boundary alignment changes.

While the Data Dictionary Query method is inspired by GraphQL, it uses a different approach for data source discovery,
orchestration and input/output data mapping.

### Data Dictionary concept

This project is not a framework for data governance and administrative measures in the traditional definition of
"data dictionary".

We apply a narrower definition of "data".

"Data" refers to "data attribute", a key-value that represents some meaning, purpose, intention or outcome.
A data entity would contain a number of data attributes that describe the behavior of the specific entity.

Data dictionary concept is more aligned to "Domain Driven Design" where events with some data attributes are passed
from one entity to another to achieve some business purpose such as a query or transaction.

### Features

1. Zero-code Data API design - the data dictionary query toolkit allows us to create data API solely by configuration.
2. Eliminates rigid schema coupling - the methodology does not require repeated definition of data schemas.
3. Aligns with Domain Driven Design - configuration starts with data attributes
4. Simplified orchestration of data providers - system automatically invokes retrieval from one or more data providers.
5. Flexible input/output data mapping - it inherits the same syntax in Event Script to map parameters and result set.
6. Pluggable data provider implementations - developers can write small pieces of code for data communication skills.

For features 1 to 5, it is driven solely by configuration.

For feature 6, the developers only need to implement a small number of organization's specific protocols and skills
for communication with target services.

### Data Dictionary

A data dictionary item can be configured as a YAML file. Each file should have a unique filename that is also its
identifier.

An example configuration file is shown below.

```yaml
dictionary:
  id: 'account-details'
  target: 'account://details'
  input:
    - 'person_id'
    - 'account_id'
  output:
    - 'response.account.details -> result.account_details'
```

In this example, a dictionary has an ID, a target, a list of input parameters and a list of output mappings.

1. The "id" should be an enterprise wide unique identifier
2. A "target" is presented in the format of `protocol://service`
3. The "input" section is a list of one or more input parameter names that are required by the target (aka "provider")
4. The "output" section is a list of one or more data mappings where each data mapping can map a key-value of the
   response from the "provider" to the "result set".

For the left hand side for each output data mapping entry, you can use simple dot-bracket format or JSON-Path format.
Since it is dealing with a single data item, you don't need to map the entire response schema. You just need to pick
the relevant key-value(s).

### Data Provider

Once a data dictionary item is defined, we can define the corresponding "data provider".

The unique filename for a data provider YAML file is created by combining the protocol and service with a hyphen
character.

In the above example, the target is "account://details" and the corresponding data provider ID is "account-details".

```yaml
provider:
  protocol: 'account'
  service: 'details'
  url: '${ACCOUNT_HOST}/api/account/details'
  method: 'POST'
  skills:
    - 'oauth2-bearer'
  headers:
    - 'accept: application/json'
    - 'content-type: application/json'
  input:
    - 'person_id -> person_id'
    - 'account_id -> account_id'
```

A provider configuration contains protocol, service, url, method, skills, headers and input.

1. "protocol" represents a target service. In this context, "protocol" is a set of capabilities of a domain.
2. "service" refers to a feature or API of a domain.
3. "url" is the target's network address.
4. "method" is the communication method. In HTTP, it may be GET, POST, PUT, DELETE, PATCH, etc.
5. "skills" refer to a list of organization's specific requirements such as OAuth 2.0, API key, JWT, etc.
6. "headers" is list of optional HTTP request headers
7. "input" is a list of input data mappings.

### Naming convention

All data dictionary IDs, data provider IDs and question IDs and their corresponding configuration filenames
should use hyphen instead of underscore between words.

This distinguishes them from the snake_case or camelCase data labels in input and output.

### Pluggable provider implementation

You can tell the system to associate a provider implementation with a provider's protocol name.

For example, the following parameters in application.properties tells the system to associate the "mdm" and
"account" protocols with a composable function route name "v1.simple.http.service".

```properties
provider.mdm=v1.simple.http.service
provider.account=v1.simple.http.service
```

A built-in HTTP service named "simple.http.service" is available in the data dictionary query framework.
It is designed to handle simple HTTP requests without authentication.

A copy of it is saved as the "MySimpleHttpService" so you can use it as a template to write your own implementations.

### Data API creation

You can create a "question" to represent a new data API that uses the available data dictionary and data provider
configuration.

An worked example is shown below.

```yaml
purpose: "Retrieve a person's name and a list of accounts"

#
# The question section contains one or more questions to be executed orderly
#
question:
  - id: 'get-accounts'
    input:
      - 'input.body.person_id -> person_id'
    output:
      - 'person_name'
      - 'person_accounts'
  - id: 'fetch-account-details'
    for_each: 'result.person_accounts -> model.account_id'
    input:
      - 'input.body.person_id -> person_id' # redundant statement but it does not matter
      - 'model.account_id -> account_id'
    output:
      - 'account_details'

#
# The concurrency value tells the system to control the number of parallel operation
# when spinning up requests based on the "for_each" directive.
# (default is 5 if not given. Min is 1 and max is 30 to avoid overwhelming a target service)
#
concurrency: 3
#
# Mapping result set to output
#
answer:
  - 'input.body.person_id -> output.body.person_id'
  - 'result.person_name -> output.body.name'
  - 'result.person_accounts -> output.body.account_id'
  - 'result.account_details -> output.body.account_details'
```

1. "purpose" - this section describes the use case
2. "question" - this section contains a list of one or more outgoing data API requests. In most cases, you only need
   a single outgoing API request to fetch data. The above example illustrates stepwise operation of APIs.
3. "concurrency" - an outgoing data API request may use the "for_each" keyword to tell the system to iterate
   an array of key-values as an input parameter for each iteration. Concurrency controls the number of
   parallel requests.
4. "answer" - this contains a list of output data mappings to construct a result set suitable for user consumption.

For each data API request in the "question" section, there are two types of APIs:

*Regular API* - contains id, input and output. "id" is the name of the request.
"input" is a list of input data mappings. "output" is a list of data dictionary items.

*Iterative API* - contains id, for_each, input and output. The "for_each" statement is a simple data mapping to prepare
for an input parameter from an array key-values. The resolved key-value is then applied to the input of the data API.
The result set will be returned as an array.

### Input/output data mapping syntax

Please refer to the data mapping section of Developer Guide's Chapter-4 (Event Script) for details.
For example, "input.body." is the namespace to obtain a set of key-values from the payload of the incoming request.
Similarly, "input.header." is the namespace to obtain a header key-value from the incoming request.

The "model." namespace is used as a temporary buffer to hold key-values for stepwise data mapping and simple
data structure creation.

## Configuration file locations

You must configure the locations of data dictionary, data provider and question specifications in the
applicaton.properties base configuration file. An example is shown below.

```properties
location.data.dictionary=file:/opt/dictionary
location.data.provider=file:/opt/providers
location.questions=file:/opt/questions
```

This example application use "classpath:/" to locate the configuration files. In production, we recommend
to externalize the data dictionary, data provider and question specs to a transient folder in your application
container. e.g. "/tmp", "/opt", etc.

## On-demand configuration parsing

The YAML configuration files are parsed on-demand to avoid loading thousands of configuration files at startup.
Once a configuration file is parsed as objects, it is cached in memory for faster execution.

## Library subproject

The "data-dictionary-query" library is available in the "extensions" project folder. It is imported as a dependency
for this example application.

## Deployment

This is an example application that can be used as a template for you to deploy your own data dictionary query system.

It may be deployed as a "sidecar" to your main POD in the same container. Alternatively, it may be deployed as
a standalone "service gateway" to downstream domains.

## Test drive

To test this sample application, you may run it in an IDE and execute the "MainApp".

To build this application, you can do `mvn clean package` and
then run `java -jar target/data-dictionary-query-4.3.57.jar`

Once the application is started, do a HTTP-POST to "http://127.0.0.1:8100/api/data/get-accounts" with payload
`"person_id": 100` to retrieve a result set for 3 data dictionary items.

A sample python script is shown below:

```python
import json, requests
d = {'person_id': 100}
h = {'content-type': 'application/json'}
r = requests.post('http://127.0.0.1:8100/api/data/get-accounts', data=json.dumps(d), headers=h)
print(r.text)
{
  "account_id": [
    "a101",
    "b202",
    "c303",
    "d400",
    "e500"
  ],
  "name": "Peter",
  "account_details": [
    {
      "balance": 25032.13,
      "id": "a101",
      "type": "Saving"
    },
    {
      "balance": 120000.0,
      "id": "c303",
      "type": "C/D"
    },
    {
      "balance": 6020.68,
      "id": "b202",
      "type": "Current"
    },
    {
      "balance": 8200.0,
      "id": "e500",
      "type": "google"
    },
    {
      "balance": 6000.0,
      "id": "d400",
      "type": "apple"
    }
  ],
  "person_id": 100
}
```

To show the specification of a question, visit "http://127.0.0.1:8100/api/specs/question/get-accounts"

```python
r = requests.get('http://127.0.0.1:8100/api/specs/question/get-accounts')
print(r.text)
{
  "purpose": "Retrieve a person's name and a list of accounts",
  "questions": [
    {
      "output": [
        "person-name",
        "person-accounts"
      ],
      "input": [
        "input.body.person_id -> person_id"
      ],
      "for_each": [],
      "id": "get-accounts"
    },
    {
      "output": [
        "account-details"
      ],
      "input": [
        "input.body.person_id -> person_id",
        "model.account_id -> account_id"
      ],
      "for_each": [
        "result.person_accounts -> model.account_id"
      ],
      "id": "fetch-account-details"
    }
  ],
  "answers": [
    "input.body.person_id -> output.body.person_id",
    "result.person_name -> output.body.name",
    "result.person_accounts -> output.body.account_id",
    "result.account_details -> output.body.account_details"
  ],
  "id": "get-accounts",
  "concurrency": 3
}
```

Similarly, the specs for data dictionary and data providers are available:
"http://127.0.0.1:8100/api/specs/data/person_name"
"http://127.0.0.1:8100/api/specs/provider/mdm/profile"

For a complete list of the configured REST endpoints, please refer to the rest.yaml configuration file
in the "resources" folder in the source code.
