#!/bin/bash
echo "Testing Schema Registry Mock using cURL"
echo "---------------------------------------"

URL="http://127.0.0.1:8081"

echo "1. Health Check:"
curl -s $URL/ | jq .
echo ""

echo "2. Register AVRO schema (subject: user-value):"
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"schema": "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}"}' \
  $URL/subjects/user-value/versions | jq .
echo ""

echo "3. Register JSON schema (subject: person-value):"
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"schema": "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}", "schemaType": "JSON"}' \
  $URL/subjects/person-value/versions | jq .
echo ""

echo "4. Retrieve Schema ID 1:"
curl -s $URL/schemas/ids/1 | jq .
echo ""

echo "5. Retrieve Schema ID 2:"
curl -s $URL/schemas/ids/2 | jq .
echo ""
