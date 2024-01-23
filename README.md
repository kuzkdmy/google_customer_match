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


1. init oauth attempt
```bash
curl --location 'localhost:8080/api/oauth' \
--header 'Content-Type: application/json' \
--data '{
"customerId" : ${GCM_CLIENT_ID},
"developerToken": "${GCM_CLIENT_DEV_TOKEN}"
}'
```


#### Project api flow
1 OAuth flow 
- Make oauth attempt request
- navigate to redirect url(property of oauth attempt response)
- copy authorization code from google auth page 
- Make complete oauth attempt request with passing oauth attempt id and authorization code
2 Create audience

https://developers.google.com/google-ads/api/docs/remarketing/audience-types/customer-match#customer_match_considerations
https://developers.google.com/google-ads/api/docs/best-practices/quotas#user_data
