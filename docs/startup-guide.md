# Qubership ATP-ITF-REPORTING Startup Guide

## How to start backend

1. Clone repository
`git clone <atp-itf-reporting repository url>`

2. Build the project
`mvn clean install`
   
3. Changed default configuration [`.run/backend.run.xml`](../.run/backend.run.xml)

    * Go to Run menu and click Edit Configuration
    * Set parameters
    * Add the following parameters in VM options - click Modify Options and select "Add VM Options":

**NOTE:** Configuration files [`application.properties`](../common/application.properties) and [`bootstrap.properties`](../common/bootstrap.properties)

**NOTE:** Configuration logging file [`logback-spring.xml`](../common/logback-spring.xml)
   
```properties
##==============================Undertow===============================
server.port=${HTTP_PORT}
# Undertow session timeout (in minutes) before authorization is expired
server.servlet.session.timeout=${UNDERTOW_SESSION_TIMEOUT}
server.compression.enabled=${UNDERTOW_COMPRESSION_ENABLED}
server.compression.mime-types=${UNDERTOW_COMPRESSION_MIMETYPE}
server.undertow.threads.io=${SERVER_UNDERTOW_IO_THREADS}
server.undertow.threads.worker=${SERVER_UNDERTOW_WORKER_THREADS}
jboss.threads.eqe.statistics=${JBOSS_THREADS_EQE_STATISTICS}
##=============================DateBase Setting===============================
spring.datasource.url=${JDBC_URL}
spring.datasource.username=${ITF_REPORTING_DB_USER}
spring.datasource.password=${ITF_REPORTING_DB_PASSWORD}
spring.datasource.hikari.minimum-idle=${SPRING_DATASOURCE_MINIDLE}
spring.datasource.hikari.maximum-pool-size=${SPRING_DATASOURCE_MAXTOTAL}
spring.datasource.hikari.idle-timeout=${SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT}
spring.datasource.hikari.max-lifetime=${SPRING_DATASOURCE_HIKARI_MAX_LIFE_TIME}
##===============================Logging===============================
logging.level.root=${LOG_LEVEL}
log.graylog.on=${GRAYLOG_ON}
log.graylog.host=${GRAYLOG_HOST}
log.graylog.port=${GRAYLOG_PORT}
##==================Integration with Spring Cloud======================
spring.application.name=${SERVICE_NAME}
eureka.client.enabled=${EUREKA_CLIENT_ENABLED}
eureka.client.serviceUrl.defaultZone=${SERVICE_REGISTRY_URL}
atp.service.public=${ATP_SERVICE_PUBLIC}
atp.service.path=${ATP_SERVICE_PATH}
##==================atp-auth-spring-boot-starter=======================
spring.profiles.active=${SPRING_PROFILES}
keycloak.enabled=${KEYCLOAK_ENABLED}
keycloak.resource=${KEYCLOAK_CLIENT_NAME}
keycloak.credentials.secret=${KEYCLOAK_SECRET}
keycloak.realm=${KEYCLOAK_REALM}
keycloak.auth-server-url=${KEYCLOAK_AUTH_URL}
atp-auth.project_info_endpoint=${PROJECT_INFO_ENDPOINT}
atp-auth.headers.content-security-policy=${CONTENT_SECURITY_POLICY}
##=============================Feign===================================
atp.public.gateway.url=${ATP_PUBLIC_GATEWAY_URL}
atp.internal.gateway.url=${ATP_INTERNAL_GATEWAY_URL}
atp.internal.gateway.enabled=${ATP_INTERNAL_GATEWAY_ENABLED}
atp.internal.gateway.name=${ATP_INTERNAL_GATEWAY_NAME}
feign.httpclient.disableSslValidation=${FEIGN_HTTPCLIENT_DISABLE_SSL}
feign.httpclient.enabled=${FEIGN_HTTPCLIENT_ENABLED}
feign.okhttp.enabled=${FEIGN_OKHTTP_ENABLED}
atp.service.internal=${ATP_SERVICE_INTERNAL}
## datasets
feign.atp.datasets.name=${FEIGN_ATP_DATASETS_NAME}
feign.atp.datasets.url=${FEIGN_ATP_DATASETS_URL}
feign.atp.datasets.route=${FEIGN_ATP_DATASETS_ROUTE}
## bulk validator
feign.atp.bv.name=${FEIGN_ATP_BV_NAME}
feign.atp.bv.url=${FEIGN_ATP_BV_URL}
feign.atp.bv.route=${FEIGN_ATP_BV_ROUTE}
## environments
feign.atp.environments.name=${FEIGN_ATP_ENVIRONMENTS_NAME}
feign.atp.environments.url=${FEIGN_ATP_ENVIRONMENTS_URL}
feign.atp.environments.route=${FEIGN_ATP_ENVIRONMENTS_ROUTE}
## catalogue
feign.atp.catalogue.name=${FEIGN_ATP_CATALOGUE_NAME}
feign.atp.catalogue.route=${FEIGN_ATP_CATALOGUE_ROUTE}
feign.atp.catalogue.url=${FEIGN_ATP_CATALOGUE_URL}
## users
feign.atp.users.url=${FEIGN_ATP_USERS_URL}
feign.atp.users.name=${FEIGN_ATP_USERS_NAME}
feign.atp.users.route=${FEIGN_ATP_USERS_ROUTE}
## itf executor
feign.atp.executor.name=${FEIGN_ATP_ITF_EXECUTOR_NAME}
feign.atp.executor.url=${FEIGN_ATP_ITF_EXECUTOR_URL}
feign.atp.executor.route=${FEIGN_ATP_ITF_EXECUTOR_ROUTE}
##=============================ActiveMQ================================
message-broker.url=${ATP_ITF_BROKER_URL_TCP}
message-broker.reports.queue=${REPORT_QUEUE}
receiver.listenerContainerFactory.concurrency=${RECEIVER_CONCURRENCY}
receiver.listenerContainerFactory.maxMessagesPerTask=${RECEIVER_MAX_MESSAGES_PER_TASK}
##=============JobRunner=========================
atp-reporting.cron.expression=${ATP_ITF_REPORTING_CRON_EXPRESSION}
##======================ATP Services integration=======================
atp.catalogue.url=${ATP_CATALOGUE_URL}
configurator.url=${ATP_ITF_CONFIGURATOR_URL}
##=================Monitoring==========================================
management.server.port=${MONITOR_PORT}
management.endpoints.web.exposure.include=${MONITOR_WEB_EXPOSE}
management.endpoints.web.base-path=${MONITOR_WEB_BASE}
management.endpoints.web.path-mapping.prometheus=${MONITOR_WEB_MAP_PROM}
management.health.consul.enabled=${CONSUL_HEALTH_CHECK_ENABLED}
##===============Hibernate-multi-tenancy configurations=================================
atp.multi-tenancy.enabled=${MULTI_TENANCY_HIBERNATE_ENABLED}
springdoc.api-docs.enabled=${SWAGGER_ENABLED}
##=============Audit Logging=================
atp.audit.logging.enable=${AUDIT_LOGGING_ENABLE}
atp.audit.logging.topic.name=${AUDIT_LOGGING_TOPIC_NAME}
atp.audit.logging.topic.partitions=${AUDIT_LOGGING_TOPIC_PARTITIONS}
atp.audit.logging.topic.replicas=${AUDIT_LOGGING_TOPIC_REPLICAS}
atp.reporting.kafka.producer.bootstrap-server=${KAFKA_REPORTING_SERVERS}
##=============Required for Kafka beans init===============
spring.kafka.producer.bootstrap-servers=${KAFKA_SERVERS}
##=============================Consul==================================
spring.cloud.consul.host=${CONSUL_URL}
spring.cloud.consul.port=${CONSUL_PORT}
spring.cloud.consul.enabled=${CONSUL_ENABLED}
spring.cloud.consul.config.enabled=${CONSUL_ENABLE}
spring.cloud.consul.config.prefix=${CONSUL_PREFIX}
spring.cloud.consul.config.acl_token=${CONSUL_TOKEN}
spring.cloud.consul.config.data-key=${CONSUL_CONFIG_DATA_KEY}
spring.cloud.consul.config.format=${CONSUL_CONFIG_FORMAT}
##=====================================================================
```

5. Click `Apply` and `OK`

6. Run the project
