# Qubership ATP-ITF-REPORTING Installation Guide

## 3rd party dependencies

| Name       | Version | Mandatory/Optional | Comment                |
|------------|---------|--------------------|------------------------|
| PostgreSQL | 14+     | Mandatory          | JDBC connection string |

## HWE

|                  | CPU request | CPU limit | RAM request | RAM limit |
|------------------|-------------|-----------|-------------|-----------|
| Dev level        | 50m         | 500m      | 300Mi       | 1500Mi    |
| Production level | 50m         | 1500m     | 3Gi         | 3Gi       |

## Minimal parameters set

```properties
-DSPRING_PROFILES=default
-DKEYCLOAK_AUTH_URL=
-DKEYCLOAK_ENABLED=
-DKEYCLOAK_REALM=
-DKEYCLOAK_CLIENT_NAME=
-DKEYCLOAK_SECRET=
-DEUREKA_CLIENT_ENABLED=true
-DSERVICE_REGISTRY_URL=
-DJDBC_URL=
-DITF_REPORTING_DB_USER=
-DITF_REPORTING_DB_PASSWORD=
```

**NOTE:** schema database will be pre-created by Liquidbase prescripts

### Full ENV VARs list per container

| Deploy Parameter Name                    | Mandatory | Example                                                                                                        | Description                                     |
|------------------------------------------|-----------|----------------------------------------------------------------------------------------------------------------|-------------------------------------------------|
| `HTTP_PORT`                              | Yes       | 8080                                                                                                           | Server port number                              |
| `UNDERTOW_SESSION_TIMEOUT`               | No        | 58m                                                                                                            | Server servlet session timeout value            |
| `UNDERTOW_COMPRESSION_ENABLED`           | No        | false                                                                                                          | Enable or disable undertow server compression   |
| `UNDERTOW_COMPRESSION_MIMETYPE`          | No        | text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml | Undertow server compression mime-types value    |
| `SERVER_UNDERTOW_IO_THREADS`             | No        | 4                                                                                                              | Undertow server threads io number               |
| `SERVER_UNDERTOW_WORKER_THREADS`         | No        | 32                                                                                                             | Undertow server threads worker number           |
| `JBOSS_THREADS_EQE_STATISTICS`           | No        | true                                                                                                           | Jboss threads statistics                        |
| `JDBC_URL`                               | Yes       | jdbc:postgresql://localhost:5432/reporting                                                                     | Datasource url                                  |
| `ITF_REPORTING_DB_USER`                  | Yes       | postgres                                                                                                       | Datasource username                             |
| `ITF_REPORTING_DB_PASSWORD`              | Yes       | postgres                                                                                                       | Datasource password                             |
| `SPRING_DATASOURCE_MINIDLE`              | No        | 10                                                                                                             | Spring hikari minimum-idle value                |
| `SPRING_DATASOURCE_MAXTOTAL`             | No        | 2000                                                                                                           | Spring hikari maximum-pool-size value           |
| `SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT`  | No        | 55000                                                                                                          | Spring hikari idle-timeout value                |
| `SPRING_DATASOURCE_HIKARI_MAX_LIFE_TIME` | No        | 7200000                                                                                                        | Spring hikari max-lifetime value                |
| `LOG_LEVEL`                              | No        | INFO                                                                                                           | Logging level value                             |
| `GRAYLOG_ON`                             | No        | false                                                                                                          | Enable or disable Graylog integration           |
| `GRAYLOG_HOST`                           | No        | graylog-service-address                                                                                        | Graylog log host address                        |
| `GRAYLOG_PORT`                           | No        | 12201                                                                                                          | Graylog port value                              |
| `SERVICE_NAME`                           | No        | atp-itf-reporting                                                                                              | Service system name                             |
| `EUREKA_CLIENT_ENABLED`                  | No        | false                                                                                                          | Enable or disable eureka integration            |
| `SERVICE_REGISTRY_URL`                   | No        | http://atp-registry-service:8761/eureka                                                                        | Eureka serviceUrl defaultZone value             |
| `ATP_SERVICE_PUBLIC`                     | No        | true                                                                                                           | Enable or disable service public                |
| `ATP_SERVICE_PATH`                       | No        | /api/atp-itf-reports/v1/**                                                                                     | Atp service path                                |
| `SPRING_PROFILES`                        | Yes       | default                                                                                                        | Spring active profiles                          |
| `KEYCLOAK_ENABLED`                       | Yes       | false                                                                                                          | Enable or disable Keycloak integration          |
| `KEYCLOAK_CLIENT_NAME`                   | Yes       | atp2                                                                                                           | Keycloak resource name                          |
| `KEYCLOAK_SECRET`                        | Yes       | f3e17149-94d0-47ed-a5b7-744c332fdf66                                                                           | Keycloak secret value                           |
| `KEYCLOAK_REALM`                         | Yes       | atp2                                                                                                           | Keycloak realm name                             |
| `KEYCLOAK_AUTH_URL`                      | Yes       | localhost                                                                                                      | Keycloak auth URL                               |
| `PROJECT_INFO_ENDPOINT`                  | Yes       | /api/v1/users/projects                                                                                         | Project metadata API endpoint                   |
| `CONTENT_SECURITY_POLICY`                | Yes       | default-src 'self' 'unsafe-inline' *                                                                           | Security policy settings for frontend           |
| `ATP_PUBLIC_GATEWAY_URL`                 | No        | http://atp-public-gateway-service-address                                                                      | Public gateway url                              |
| `ATP_INTERNAL_GATEWAY_URL`               | No        | http://atp-internal-gateway:8080                                                                               | Internal gateway url                            |
| `ATP_INTERNAL_GATEWAY_ENABLED`           | No        | false                                                                                                          | Enable or disable Internal gateway              |
| `ATP_INTERNAL_GATEWAY_NAME`              | No        | atp-internal-gateway                                                                                           | Internal gateway name                           |
| `FEIGN_HTTPCLIENT_DISABLE_SSL`           | No        | true                                                                                                           | Feign enable or disable ssl validation          |
| `FEIGN_HTTPCLIENT_ENABLED`               | No        | true                                                                                                           | Enable or disable feign                         |
| `FEIGN_OKHTTP_ENABLED`                   | No        | true                                                                                                           | Enable or disable feign okhttp                  |
| `FEIGN_ATP_DATASETS_NAME`                | No        | ATP-DATASETS                                                                                                   | Feign atp-dataset client name                   |
| `FEIGN_ATP_DATASETS_URL`                 | No        | -                                                                                                              | Feign atp-dataset client url                    |
| `FEIGN_ATP_DATASETS_ROUTE`               | No        | api/atp-datasets/v1                                                                                            | Feign atp-dataset client route                  |
| `FEIGN_ATP_BV_NAME`                      | No        | ATP-BV                                                                                                         | Feign atp-bv client name                        |
| `FEIGN_ATP_BV_URL`                       | No        | -                                                                                                              | Feign atp-bv client url                         |
| `FEIGN_ATP_BV_ROUTE`                     | No        | api/bvtool/v1                                                                                                  | Feign atp-bv client route                       |
| `FEIGN_ATP_ENVIRONMENTS_NAME`            | No        | ATP-ENVIRONMENTS                                                                                               | Feign atp-environments client name              |
| `FEIGN_ATP_ENVIRONMENTS_URL`             | No        | -                                                                                                              | Feign atp-environments client url               |
| `FEIGN_ATP_ENVIRONMENTS_ROUTE`           | No        | api/atp-environments/v1                                                                                        | Feign atp-environments client route             |
| `FEIGN_ATP_CATALOGUE_NAME`               | No        | ATP-CATALOGUE                                                                                                  | Feign atp-catalogue client name                 |
| `FEIGN_ATP_CATALOGUE_ROUTE`              | No        | -                                                                                                              | Feign atp-catalogue client url                  |
| `FEIGN_ATP_CATALOGUE_URL`                | No        | api/atp-catalogue/v1                                                                                           | Feign atp-catalogue client route                |
| `FEIGN_ATP_USERS_URL`                    | No        | ATP-USERS-BACKEND                                                                                              | Feign atp-users-backend client name             |
| `FEIGN_ATP_USERS_NAME`                   | No        | -                                                                                                              | Feign atp-users-backend client url              |
| `FEIGN_ATP_USERS_ROUTE`                  | No        | api/atp-users-backend/v1                                                                                       | Feign atp-users-backend client route            |
| `FEIGN_ATP_ITF_EXECUTOR_NAME`            | No        | ATP-ITF-EXECUTOR                                                                                               | Feign atp-itf-executor client name              |
| `FEIGN_ATP_ITF_EXECUTOR_URL`             | No        | -                                                                                                              | Feign atp-itf-executor client url               |
| `FEIGN_ATP_ITF_EXECUTOR_ROUTE`           | No        | api/atp-itf-executor/v1                                                                                        | Feign atp-itf-executor client route             |
| `ATP_ITF_BROKER_URL_TCP`                 | Yes       | tcp://atp-activemq:61616?wireFormat.maxInactivityDuration=0&wireFormat.maxFrameSize=104857600&keepAlive=true   | Broker url                                      |
| `REPORT_QUEUE`                           | Yes       | ReportExecution                                                                                                | Broker reports queue name                       |
| `ATP_CATALOGUE_URL`                      | No        | http://atp-catalogue-service-address                                                         | Catalogue service url                           |
| `ATP_ITF_CONFIGURATOR_URL`               | No        | http://atp-itf-configurator-service-address                                                  | ITF Configurator service url                    |
| `MONITOR_PORT`                           | No        | 8090                                                                                                           | Metric server port number                       |
| `MONITOR_WEB_EXPOSE`                     | No        | prometheus,health,info,env                                                                                     | Metric endpoints exposure include               |
| `MONITOR_WEB_BASE`                       | No        | /                                                                                                              | Metric endpoints base-path                      |
| `MONITOR_WEB_MAP_PROM`                   | No        | metrics                                                                                                        | Metric endpoints path-mapping prometheus        |
| `MULTI_TENANCY_HIBERNATE_ENABLED`        | No        | false                                                                                                          | Enable or disable atp multi-tenancy integration |
| `SWAGGER_ENABLED`                        | No        | true                                                                                                           | Enable or disable Swagger integration           |
| `AUDIT_LOGGING_ENABLE`                   | No        | false                                                                                                          | Enable or Disable audit logging                 |
| `AUDIT_LOGGING_TOPIC_NAME`               | No        | audit_logging_topic                                                                                            | Audit logging Kafka topic name                  |
| `AUDIT_LOGGING_TOPIC_PARTITIONS`         | No        | 1                                                                                                              | Audit logging Kafka topic partitions number     |
| `AUDIT_LOGGING_TOPIC_REPLICAS`           | No        | 3                                                                                                              | Audit logging Kafka replicas number             |
| `KAFKA_REPORTING_SERVERS`                | No        | kafka.reporting.svc:9092                                                                                       | Atp reporting kafka producer bootstrap-server   |
| `KAFKA_SERVERS`                          | No        | kafka:9092                                                                                                     | Spring kafka producer bootstrap-servers         |
| `CONSUL_URL`                             | No        | localhost                                                                                                      | Consul host number                              |
| `CONSUL_PORT`                            | No        | 8500                                                                                                           | Consul port number                              |
| `CONSUL_ENABLED`                         | No        | false                                                                                                          | Enable or disable Consul                        |
| `CONSUL_PREFIX`                          | No        | devci                                                                                                          | Consul prefix value                             |
| `CONSUL_TOKEN`                           | No        | -                                                                                                              | Consul acl_token value                          |
| `CONSUL_CONFIG_DATA_KEY`                 | No        | data                                                                                                           | Consul config data-key value                    |
| `CONSUL_CONFIG_FORMAT`                   | No        | properties                                                                                                     | Consul config format value                      |

# Helm

## Prerequisites

1. Install k8s locally
2. Install Helm

## How to deploy tool

1. Build snapshot (artifacts and docker image) of https://github.com/Netcracker/qubership-testing-platform-itf-reporting in GitHub
2. Clone repository to a place, available from your openshift/kubernetes where you need to deploy the tool to
3. Navigate to <repository-root>/deployments/charts/atp-itf-stubs folder
4. Check/change configuration parameters in the ./values.yaml file according to your services installed
5. Execute the command: `helm install atp-itf-reporting`
6. After installation is completed, check deployment health
