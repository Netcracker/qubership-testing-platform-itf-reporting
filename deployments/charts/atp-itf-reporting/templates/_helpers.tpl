{{/* Helper functions, do NOT modify */}}
{{- define "env.default" -}}
{{- $ctx := get . "ctx" -}}
{{- $def := get . "def" | default $ctx.Values.SERVICE_NAME -}}
{{- $pre := get . "pre" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "" $ctx.Release.Namespace) -}}
{{- get . "val" | default ((empty $pre | ternary $def (print $pre "_" (trimPrefix "atp-" $def))) | nospace | replace "-" "_") -}}
{{- end -}}

{{- define "env.factor" -}}
{{- $ctx := get . "ctx" -}}
{{- get . "def" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "1" (default "3" $ctx.Values.KAFKA_REPLICATION_FACTOR)) -}}
{{- end -}}

{{- define "env.compose" }}
{{- range $key, $val := merge (include "env.lines" . | fromYaml) (include "env.secrets" . | fromYaml) }}
{{ printf "- %s=%s" $key $val }}
{{- end }}
{{- end }}

{{- define "env.cloud" }}
{{- range $key, $val := (include "env.lines" . | fromYaml) }}
{{ printf "- name: %s" $key }}
{{ printf "  value: \"%s\"" $val }}
{{- end }}
{{- $keys := (include "env.secrets" . | fromYaml | keys | uniq | sortAlpha) }}
{{- range $keys }}
{{ printf "- name: %s" . }}
{{ printf "  valueFrom:" }}
{{ printf "    secretKeyRef:" }}
{{ printf "      name: %s-secrets" $.Values.SERVICE_NAME }}
{{ printf "      key: %s" . }}
{{- end }}
{{- end }}

{{- define "env.host" -}}
{{- $url := .Values.ATP_ITF_REPORTING_URL -}}
{{- if $url -}}
{{- regexReplaceAll "http(s)?://(.*)" $url "${2}" -}}
{{- else -}}
{{- $hosts := dict "KUBERNETES" "dev-kubernetes-address" "OPENSHIFT" "dev-cloud-address" -}}
{{- print .Values.SERVICE_NAME "-" .Release.Namespace "." (.Values.CLOUD_PUBLIC_HOST | default (index $hosts .Values.PAAS_PLATFORM)) -}}
{{- end -}}
{{- end -}}
{{/* Helper functions end */}}

{{/* Environment variables to be used AS IS */}}
{{- define "env.lines" }}
ACTIVEMQ_PORT: "{{ .Values.ACTIVEMQ_PORT_PARAM }}"
ATP_CATALOGUE_URL: "{{ .Values.CATALOGUE_URL }}"
ATP_INTERNAL_GATEWAY_ENABLED: "{{ .Values.ATP_INTERNAL_GATEWAY_ENABLED }}"
ATP_INTERNAL_GATEWAY_NAME: "{{ .Values.ATP_INTERNAL_GATEWAY_NAME }}"
ATP_INTERNAL_GATEWAY_URL: "{{ .Values.ATP_INTERNAL_GATEWAY_URL }}"
ATP_ITF_BROKER_URL_TCP: "{{ .Values.ATP_ITF_BROKER_URL_TCP }}"
ATP_ITF_CONFIGURATOR_URL: "{{ .Values.ATP_ITF_CONFIGURATOR_URL }}"
ATP_ITF_REPORTING_CRON_EXPRESSION: "{{ .Values.ATP_ITF_REPORTING_CRON_EXPRESSION }}"
ATP_PUBLIC_GATEWAY_URL: "{{ .Values.ATP_PUBLIC_GATEWAY_URL }}"
ATP_RAM_RECEIVER_URL: "{{ .Values.ATP_RAM_RECEIVER_URL }}"
ATP_RAM_URL: "{{ .Values.ATP_RAM_URL }}"
ATP_SERVICE_INTERNAL: "{{ .Values.ATP_SERVICE_INTERNAL }}"
ATP_SERVICE_PUBLIC: "{{ .Values.ATP_SERVICE_PUBLIC }}"
AUDIT_LOGGING_ENABLE: "{{ .Values.AUDIT_LOGGING_ENABLE }}"
AUDIT_LOGGING_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.AUDIT_LOGGING_TOPIC_NAME "def" "audit_logging_topic") }}"
AUDIT_LOGGING_TOPIC_PARTITIONS: "{{ .Values.AUDIT_LOGGING_TOPIC_PARTITIONS }}"
AUDIT_LOGGING_TOPIC_REPLICAS: "{{ include "env.factor" (dict "ctx" . "def" .Values.AUDIT_LOGGING_TOPIC_REPLICAS) }}"
BV_SERVICE_URL: "{{ .Values.ATP_BVT_URL }}"
CLOUD_NAMESPACE: "{{ .Release.Namespace }}"
CONSUL_CONFIG_DATA_KEY: "{{ .Values.CONSUL_CONFIG_DATA_KEY }}"
CONSUL_CONFIG_FORMAT: "{{ .Values.CONSUL_CONFIG_FORMAT }}"
CONSUL_ENABLED: "{{ .Values.CONSUL_ENABLED }}"
CONSUL_HEALTH_CHECK_ENABLED: "{{ .Values.CONSUL_HEALTH_CHECK_ENABLED }}"
CONSUL_PORT: "{{ .Values.CONSUL_PORT }}"
CONSUL_PREFIX: "{{ .Values.CONSUL_PREFIX }}"
CONSUL_TOKEN: "{{ .Values.CONSUL_TOKEN }}"
CONSUL_URL: "{{ .Values.CONSUL_URL }}"
CONTENT_SECURITY_POLICY: "{{ .Values.CONTENT_SECURITY_POLICY }}"
DATASET_SERVICE_URL: "{{ .Values.ATP_DATASET_URL }}"
ENVIRONMENTS_SERVICE_URL: "{{ .Values.ATP_ENVIRONMENTS_URL }}"
EUREKA_CLIENT_ENABLED: "{{ .Values.EUREKA_CLIENT_ENABLED }}"
FEIGN_ATP_BV_NAME: ATP-BV
FEIGN_ATP_BV_ROUTE: api/bvtool/v1
FEIGN_ATP_BV_URL: ""
FEIGN_ATP_CATALOGUE_NAME: "{{ .Values.FEIGN_ATP_CATALOGUE_NAME }}"
FEIGN_ATP_CATALOGUE_ROUTE: "{{ .Values.FEIGN_ATP_CATALOGUE_ROUTE }}"
FEIGN_ATP_CATALOGUE_URL: "{{ .Values.FEIGN_ATP_CATALOGUE_URL }}"
FEIGN_ATP_DATASETS_NAME: ATP-DATASETS
FEIGN_ATP_DATASETS_ROUTE: api/atp-datasets/v1
FEIGN_ATP_DATASETS_URL: ""
FEIGN_ATP_ENVIRONMENTS_NAME: ATP-ENVIRONMENTS
FEIGN_ATP_ENVIRONMENTS_ROUTE: api/atp-environments/v1
FEIGN_ATP_ENVIRONMENTS_URL: ""
FEIGN_ATP_USERS_NAME: "{{ .Values.FEIGN_ATP_USERS_NAME }}"
FEIGN_ATP_USERS_ROUTE: "{{ .Values.FEIGN_ATP_USERS_ROUTE }}"
FEIGN_ATP_USERS_URL: "{{ .Values.FEIGN_ATP_USERS_URL }}"
FEIGN_HTTPCLIENT_DISABLE_SSL: "true"
FEIGN_HTTPCLIENT_ENABLED: "false"
FEIGN_OKHTTP_ENABLED: "true"
GRAYLOG_HOST: "{{ .Values.GRAYLOG_HOST }}"
GRAYLOG_ON: "{{ .Values.GRAYLOG_ON }}"
GRAYLOG_PORT: "{{ .Values.GRAYLOG_PORT }}"
HAZELCAST_ADDRESS: "{{ .Values.HAZELCAST_ADDRESS }}"
JAVA_OPTIONS: "{{ if .Values.HEAPDUMP_ENABLED }}-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/diagnostic{{ end }} -Dcom.sun.management.jmxremote={{ .Values.JMX_ENABLE }} -Dcom.sun.management.jmxremote.port={{ .Values.JMX_PORT }} -Dcom.sun.management.jmxremote.rmi.port={{ .Values.JMX_RMI_PORT }} -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dspring.datasource.hikari.maximum-pool-size={{ .Values.SPRING_DATASOURCE_MAXTOTAL }} -Dspring.datasource.hikari.minimum-idle={{ .Values.SPRING_DATASOURCE_MINIDLE }} -Dserver.undertow.threads.io={{ .Values.SERVER_UNDERTOW_IO_THREADS }} -Dserver.undertow.threads.worker={{ .Values.SERVER_UNDERTOW_WORKER_THREADS }} -Djboss.threads.eqe.statistics={{ .Values.JBOSS_THREADS_EQE_STATISTICS }} "
JDBC_URL: "jdbc:postgresql://{{ .Values.PG_DB_ADDR }}:{{ .Values.PG_DB_PORT }}/{{ include "env.default" (dict "ctx" . "val" .Values.ITF_REPORTING_DB "def" .Values.SERVICE_NAME ) }}"
KAFKA_LOGRECORD_CONTEXT_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_LOGRECORD_CONTEXT_TOPIC "def" "orch_logrecord_context_topic") }}"
KAFKA_LOGRECORD_CONTEXT_TOPIC_PARTITIONS: "{{ .Values.KAFKA_LOGRECORD_CONTEXT_TOPIC_PARTITIONS }}"
KAFKA_LOGRECORD_CONTEXT_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_LOGRECORD_CONTEXT_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC "def" "orch_logrecord_step_context_topic") }}"
KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC_PARTITIONS: "{{ .Values.KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC_PARTITIONS }}"
KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_LOGRECORD_STEP_CONTEXT_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_REPORTING_SERVERS: "{{ .Values.KAFKA_REPORTING_SERVERS }}"
KAFKA_SERVERS: "{{ .Values.KAFKA_SERVERS }}"
KAFKA_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_TOPIC "def" "catalog_notification_topic") }}"
KEYCLOAK_AUTH_URL: "{{ .Values.KEYCLOAK_AUTH_URL }}"
KEYCLOAK_DISABLE_SSL: "true"
KEYCLOAK_ENABLED: "{{ .Values.KEYCLOAK_ENABLED }}"
KEYCLOAK_REALM: "{{ .Values.KEYCLOAK_REALM }}"
MAX_RAM_SIZE: "{{ .Values.MAX_RAM_SIZE }}"
MICROSERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
MULTI_TENANCY_HIBERNATE_ENABLED: "{{ .Values.MULTI_TENANCY_HIBERNATE_ENABLED }}"
PARTITIONING_ENABLED: "{{ .Values.PARTITIONING_ENABLED }}"
PROFILER_ENABLED: "{{ .Values.PROFILER_ENABLED }}"
PROJECT_INFO_ENDPOINT: "{{ .Values.PROJECT_INFO_ENDPOINT }}"
RECEIVER_CONCURRENCY: "{{ .Values.RECEIVER_CONCURRENCY }}"
RECEIVER_MAX_MESSAGES_PER_TASK: "{{ .Values.RECEIVER_MAX_MESSAGES_PER_TASK }}"
REMOTE_DUMP_HOST: "{{ .Values.REMOTE_DUMP_HOST }}"
REMOTE_DUMP_PORT: "{{ .Values.REMOTE_DUMP_PORT }}"
REPORT_QUEUE: "{{ include "env.default" (dict "ctx" . "val" .Values.REPORT_QUEUE "def" "ReportExecution") }}"
RUNNING_URL: "{{ .Values.ATP_ITF_REPORTING_URL | default  (print "http://" .Values.SERVICE_NAME "-" .Release.Namespace "." (.Values.CLOUD_PUBLIC_HOST|default "dev-kubernetes-address")) }}"
SERVICE_REGISTRY_URL: "{{ .Values.SERVICE_REGISTRY_URL }}"
SPRING_PROFILES: "{{ .Values.SPRING_PROFILES }}"
SWAGGER_ENABLED: "{{ .Values.SWAGGER_ENABLED }}"
UNDERTOW_COMPRESSION_ENABLED: "{{ .Values.UNDERTOW_COMPRESSION_ENABLED }}"
UNDERTOW_COMPRESSION_MIMETYPE: "{{ .Values.UNDERTOW_COMPRESSION_MIMETYPE }}"
UNDERTOW_SESSION_TIMEOUT: "{{ .Values.UNDERTOW_SESSION_TIMEOUT }}"
{{- end }}

{{/* Sensitive data to be converted into secrets whenever possible */}}
{{- define "env.secrets" }}
ITF_REPORTING_DB_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.ITF_REPORTING_DB_USER "def" .Values.SERVICE_NAME ) }}"
ITF_REPORTING_DB_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.ITF_REPORTING_DB_PASSWORD "def" .Values.SERVICE_NAME ) }}"
KEYCLOAK_CLIENT_NAME: "{{ default "atp-itf" .Values.KEYCLOAK_CLIENT_NAME }}"
KEYCLOAK_SECRET: "{{ default "71b6a213-e3b0-4bf4-86c8-dfe11ce9e248" .Values.KEYCLOAK_SECRET }}"
{{- if .Values.MULTI_TENANCY_HIBERNATE_ENABLED -}}
{{- $additionalClusters := .Values.additionalClusters -}}
{{- if $additionalClusters -}}
{{- range $cluster, $params := .Values.additionalClusters }}
ADDITIONAL_PG_{{ print $cluster "_URL" | upper }}: "{{ print $params.url }}"
ADDITIONAL_PG_{{ print $cluster "_USERNAME" | upper }}: "{{ print $params.user }}"
ADDITIONAL_PG_{{ print $cluster "_PASSWORD" | upper }}: "{{ print $params.password }}"
ADDITIONAL_PG_{{ print $cluster "_DRIVER" | upper }}: "{{ print $params.driver }}"
ADDITIONAL_PG_{{ print $cluster "_PROJECTS" | upper }}: "{{ print ($params.projects | join ",") }}"
{{- end }}
{{- end }}
{{- end }}
{{- end }}

{{- define "env.deploy" }}
pg_pass: "{{ .Values.pg_pass }}"
pg_user: "{{ .Values.pg_user }}"
ITF_REPORTING_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.ITF_REPORTING_DB "def" .Values.SERVICE_NAME ) }}"
PG_DB_ADDR: "{{ .Values.PG_DB_ADDR }}"
PG_DB_PORT: "{{ .Values.PG_DB_PORT }}"
{{- end }}