envValue=$1
APP_NAME=$2
OPENSHIFT_NAMESPACE=$3
COMMON_NAMESPACE=$4
DB_JDBC_CONNECT_STRING=$5
DB_PWD=$6
DB_USER=$7
SPLUNK_TOKEN=$8
CHES_CLIENT_ID=$9
CHES_CLIENT_SECRET=${10}
CHES_TOKEN_URL=${11}
CHES_ENDPOINT_URL=${12}

TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"

SOAM_KC=soam-$envValue.apps.silver.devops.gov.bc.ca
SOAM_KC_LOAD_USER_ADMIN=$(oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)

NATS_CLUSTER=educ_nats_cluster
NATS_URL="nats://nats.${OPENSHIFT_NAMESPACE}-${envValue}.svc.cluster.local:4222"

echo Fetching SOAM token
TKN=$(curl -s \
  -d "client_id=admin-cli" \
  -d "username=$SOAM_KC_LOAD_USER_ADMIN" \
  -d "password=$SOAM_KC_LOAD_USER_PASS" \
  -d "grant_type=password" \
  "https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" | jq -r '.access_token')


echo
echo Retrieving client ID for trax-notification-api-service
TN_APIServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq '.[] | select(.clientId=="trax-notification-api-service")' | jq -r '.id')

echo
echo Removing trax-notification-api-service client if exists
curl -sX DELETE "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$TN_APIServiceClientID" \
  -H "Authorization: Bearer $TKN" \

echo
echo Creating client trax-notification-api-service
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"clientId\" : \"trax-notification-api-service\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\",\"READ_PEN_TRAX\", \"role_list\", \"profile\", \"roles\", \"email\"],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

echo
echo Retrieving client ID for trax-notification-api-service
TN_APIServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq '.[] | select(.clientId=="trax-notification-api-service")' | jq -r '.id')

echo
echo Retrieving client secret for trax-notification-api-service
TN_APIServiceClientSecret=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$TN_APIServiceClientID/client-secret" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq -r '.value')

###########################################################
#Setup for config-map
###########################################################
SPLUNK_URL="gww.splunk.educ.gov.bc.ca"
FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   Parsers_File parsers.conf
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Exclude_Path *.gz,*.zip
   Parser docker
   Mem_Buf_Limit 20MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  *
[OUTPUT]
   Name  splunk
   Match *
   Host  $SPLUNK_URL
   Port  443
   TLS         On
   TLS.Verify  Off
   Message_Key $APP_NAME
   Splunk_Token $SPLUNK_TOKEN
"
PARSER_CONFIG="
[PARSER]
    Name        docker
    Format      json
"
if [ "$envValue" = "dev" ]; then
  PEN_COORDINATOR_EMAIL=dev.pens.coordinator@no-reply.gov.bc.ca
  TO_EMAIL=EDUCDO@Victoria1.gov.bc.ca
fi

if [ "$envValue" = "test" ]; then
  PEN_COORDINATOR_EMAIL=test.pens.coordinator@no-reply.gov.bc.ca
  TO_EMAIL=EDUCDO@Victoria1.gov.bc.ca
fi

if [ "$envValue" = "prod" ]; then
  PEN_COORDINATOR_EMAIL=pens.coordinator@gov.bc.ca
  TO_EMAIL=student.certification@gov.bc.ca
fi

echo
echo Creating config map "$APP_NAME"-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-config-map --from-literal=TZ=$TZVALUE --from-literal=STUDENT_API_URL="http://student-api-master.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/student" --from-literal=TOKEN_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" --from-literal=JDBC_URL="$DB_JDBC_CONNECT_STRING" --from-literal=ORACLE_USERNAME="$DB_USER" --from-literal=ORACLE_PASSWORD="$DB_PWD" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=NATS_URL="$NATS_URL" --from-literal=NATS_CLUSTER="$NATS_CLUSTER" --from-literal=CRON_SCHEDULED_PROCESS_EVENTS_STAN="0 0/1 * * * *" --from-literal=CRON_SCHEDULED_PROCESS_EVENTS_STAN_LOCK_AT_LEAST_FOR="PT50S" --from-literal=CRON_SCHEDULED_PROCESS_EVENTS_STAN_LOCK_AT_MOST_FOR="PT55S" --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --from-literal=CLIENT_ID="trax-notification-api-service" --from-literal=CLIENT_SECRET="$TN_APIServiceClientSecret" --from-literal=NATS_MAX_RECONNECT=60 --from-literal=HIBERNATE_SQL_LOG_LEVEL=INFO --from-literal=HIBERNATE_PARAM_LOG_LEVEL=INFO --from-literal=DB_CONNECTION_MAX_POOL_SIZE=4 --from-literal=DB_CONNECTION_MIN_IDLE=4 --from-literal=CHES_CLIENT_ID="$CHES_CLIENT_ID" --from-literal=CHES_CLIENT_SECRET="$CHES_CLIENT_SECRET" --from-literal=CHES_TOKEN_URL="$CHES_TOKEN_URL" --from-literal=CHES_ENDPOINT_URL="$CHES_ENDPOINT_URL" --from-literal=NOTIFICATION_EMAIL_MERGE_DEMERGE_FROM_EMAIL="$PEN_COORDINATOR_EMAIL" --from-literal=NOTIFICATION_EMAIL_MERGE_DEMERGE_TO_EMAIL="$TO_EMAIL" --from-literal=TRAX_API_URL="http://pen-trax-api-master.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1" --from-literal=PURGE_RECORDS_EVENT_AFTER_DAYS=365 --from-literal=SCHEDULED_JOBS_PURGE_OLD_EVENT_RECORDS_CRON="@midnight" --dry-run -o yaml | oc apply -f -

echo
echo Setting environment variables for "$APP_NAME-$SOAM_KC_REALM_ID" application
oc -n "$OPENSHIFT_NAMESPACE-$envValue" set env --from=configmap/"$APP_NAME"-config-map deployment/"$APP_NAME"-main

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG" --from-literal=parsers.conf="$PARSER_CONFIG" --dry-run -o yaml | oc apply -f -
