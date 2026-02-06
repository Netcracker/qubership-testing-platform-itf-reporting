#!/bin/sh

if [ "${ATP_INTERNAL_GATEWAY_ENABLED}" = "true" ]; then
  echo "Internal gateway integration is enabled."
  export FEIGN_ATP_CATALOGUE_NAME="$ATP_INTERNAL_GATEWAY_NAME"
  export FEIGN_ATP_USERS_NAME="$ATP_INTERNAL_GATEWAY_NAME"
  export FEIGN_ATP_DATASETS_NAME="$ATP_INTERNAL_GATEWAY_NAME"
  export FEIGN_ATP_BV_NAME="$ATP_INTERNAL_GATEWAY_NAME"
  export FEIGN_ATP_ENVIRONMENTS_NAME="$ATP_INTERNAL_GATEWAY_NAME"
  export FEIGN_ATP_ITF_EXECUTOR_NAME="$ATP_INTERNAL_GATEWAY_NAME"
else
  echo "Internal gateway integration is disabled."
  export FEIGN_ATP_CATALOGUE_ROUTE=
  export FEIGN_ATP_USERS_ROUTE=
  export FEIGN_ATP_DATASETS_ROUTE=
  export FEIGN_ATP_BV_ROUTE=
  export FEIGN_ATP_ENVIRONMENTS_ROUTE=
  export FEIGN_ATP_ITF_EXECUTOR_ROUTE=
fi

# *** Set JVM options
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.config.location=application.properties"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.cloud.bootstrap.location=bootstrap.properties"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dbootstrap.servers=${KAFKA_SERVERS:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlogging.config=logback-spring.xml"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.on=${GRAYLOG_ON}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.host=${GRAYLOG_HOST}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.port=${GRAYLOG_PORT}"

if [ "${MULTI_TENANCY_HIBERNATE_ENABLED}" = "true" ]; then
    i=0
    _javaoptions="javaoptions.$$"
    env | grep -E '^ADDITIONAL_PG_.*_URL=.*' | sort -u | while IFS='=' read -r _key _value; do
    echo "-Datp.multi-tenancy.additional.postgres.clusters[${i}].url=${_value}" >> "${_javaoptions}"
    # shellcheck disable=SC2030
    i=$((i+1))
    done

    env | grep -E '^ADDITIONAL_PG_.*_USERNAME=.*' | sort -u | while IFS='=' read -r _key _value; do
    # shellcheck disable=SC2031
    echo "-Datp.multi-tenancy.additional.postgres.clusters[${i}].username=${_value}" >> "${_javaoptions}"
    # shellcheck disable=SC2030
    # shellcheck disable=SC2031
    i=$((i+1))
    done

    env | grep -E '^ADDITIONAL_PG_.*_PASSWORD=.*' | sort -u | while IFS='=' read -r _key _value; do
    # shellcheck disable=SC2031
    echo "-Datp.multi-tenancy.additional.postgres.clusters[${i}].password=${_value}" >> "${_javaoptions}"
    # shellcheck disable=SC2030
    # shellcheck disable=SC2031
    i=$((i+1))
    done

    env | grep -E '^ADDITIONAL_PG_.*_DRIVER=.*' | sort -u | while IFS='=' read -r _key _value; do
    # shellcheck disable=SC2031
    echo "-Datp.multi-tenancy.additional.postgres.clusters[${i}].driver-class=${_value}" >> "${_javaoptions}"
    # shellcheck disable=SC2030
    # shellcheck disable=SC2031
    i=$((i+1))
    done

    env | grep -E '^ADDITIONAL_PG_.*_PROJECTS=.*' | sort -u | while IFS='=' read -r _key _value; do
    # shellcheck disable=SC2031
    echo "-Datp.multi-tenancy.additional.postgres.clusters[${i}].projects=${_value}" >> "${_javaoptions}"
    # shellcheck disable=SC2030
    # shellcheck disable=SC2031
    i=$((i+1))
    done

    sort -u -o "${_javaoptions}" "${_javaoptions}"
    while read -r _line; do
      JAVA_OPTIONS="${JAVA_OPTIONS} ${_line}"
    done <"${_javaoptions}"
fi

/usr/bin/java -Xverify:none -Xms128m -XX:MaxRAM="${MAX_RAM_SIZE:-3000m}" -Dhibernate.jdbc.lob.non_contextual_creation=true -XX:MaxRAMPercentage=75.0 "${JAVA_OPTIONS}" -cp "./:./lib/*" org.qubership.automation.itf.Main
