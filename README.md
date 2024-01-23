### Minimal prototype to create audience in Google Customer Match

![img.png](docs/img.png)

#### Docs
[Google OAuth2](https://developers.google.com/identity/protocols/oauth2)

#### How to run
1. Create `.env` file in root directory
2. Add `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` to `.env` file
https://console.cloud.google.com/apis/credentials "Client ID" and "Client secret" under console google cloud
- "APIs & Services" > "Credentials" > "OAuth 2.0 Client IDs" > "Web client (auto created by Google Service)"
- "APIs & Services" > "Credentials" > "OAuth 2.0 Client IDs" > "Desktop client for tests without WEB"
3. Prepare GCM_CLIENT_ID and GCM_CLIENT_DEV_TOKEN
https://ads.google.com 
- client id can be seen at the top right corner of the page
- developer token located under "Tools & Settings" > "API Center" > "Developer Token"

#### Project api flow
1. Create Google Customer Match integration connection 
- Make oauth attempt request
```bash
curl --location 'localhost:8080/api/oauth' \
--header 'Content-Type: application/json' \
--data '{
"customerId" : ${GCM_CLIENT_ID},
"developerToken": "${GCM_CLIENT_DEV_TOKEN}"
}'
```
- navigate to redirect url(property of oauth attempt response)
- copy id from oauth attempt request and authorization code from google auth page 
- Make complete oauth attempt request with passing oauth attempt id and authorization code
```bash
curl --location --request PUT 'localhost:8080/api/oauth' \
--header 'Content-Type: application/json' \
--data '{
    "id": "${GCM_CONNECTION_ID_FROM_OAUTH_ATTEMPT_RESPONSE}",
    "code": "${AUTHORIZATION_CODE_FROM_GOOGLE_AUTH_PAGE}"

}' 
```
- at this point we have valid google customer match connection(refresh of it is a part of backend responsibility)
2. Create users list(audience list)
```bash
curl --location 'localhost:8080/api/user-list' \
--header 'Content-Type: application/json' \
--data-raw '{
    "connectionId" : "${GCM_CONNECTION_ID_FROM_OAUTH_ATTEMPT_RESPONSE}",
    "listName": "dmitriy.kuzkin@gmail.com [23 Jan 2024] created via api resource name"
}' 
```
- from response take id
```bash
{
    "id": 8564853330,
    "name": "dmitriy.kuzkin@gmail.com [23 Jan 2024] created via api resource name",
    "resourceName": "customers/2319161505/userLists/8564853330",
    "description": "GCM integration via API created users list",
    "matchRatePercentage": 0
} 
```
3. Add users to list
```bash
curl --location 'localhost:8080/api/user-list/members' \
--header 'Content-Type: application/json' \
--data '{
    "connectionId" : "dea04b25-7fb5-4ec5-9e2e-d05d91a25308",
    "listId": "{{GCM_USER_LIST_ID_FROM_CREATE_USER_LIST_RESPONSE}}", 
    "membersToAdd": [{SEE NOTE BELOW}, {SEE NOTE BELOW}],
    "membersToRemove": [{SEE NOTE BELOW}, {SEE NOTE BELOW}]
}' 
```
- membersToAdd and membersToRemove are arrays of objects with following structure, see UserIdentity
```json
{
  "email"               : "string optional field that will be used by google to identify person",
  "hashedEmail"         : "string optional field that will be used by google to identify person",
  "mobileId"            : "string optional field that will be used by google to identify person",
  "thirdPartyUserId"    : "string optional field that will be used by google to identify person",
  "phoneNumber"         : "string optional field that will be used by google to identify person",
  "hashedPhoneNumber"   : "string optional field that will be used by google to identify person",
  "firstName"           : "string optional field that will be used by google to identify person",
  "hashedFirstName"     : "string optional field that will be used by google to identify person",
  "lastName"            : "string optional field that will be used by google to identify person",
  "hashedLastName"      : "string optional field that will be used by google to identify person",
  "state"               : "string optional field that will be used by google to identify person",
  "postalCode"          : "string optional field that will be used by google to identify person",
  "countryCode"         : "string optional field that will be used by google to identify person",
  "city"                : "string optional field that will be used by google to identify person",
  "streetAddress"       : "string optional field that will be used by google to identify person",
  "hashedStreetAddress" : "string optional field that will be used by google to identify person"
}
```


https://developers.google.com/google-ads/api/docs/remarketing/audience-types/customer-match#customer_match_considerations
https://developers.google.com/google-ads/api/docs/best-practices/quotas#user_data
