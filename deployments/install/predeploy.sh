#!/usr/bin/env sh

#get value of dynamic variable
getValue() {
  eval "value=\"\${$1}\""
  echo "${value}"
}

if [ ! -f ./atp-common-scripts/openshift/common.sh ]; then
  echo "ERROR: Cannot locate ./atp-common-scripts/openshift/common.sh"
  exit 1
fi

# shellcheck source=../atp-common-scripts/openshift/common.sh
. ./atp-common-scripts/openshift/common.sh

_ns="${NAMESPACE}"
echo "***** Preparing Postgres connection *****"
PG_DB_ADDR=${PG_DB_ADDR:?PG_DB_ADDR is empty}
PG_DB_PORT=${PG_DB_PORT:?PG_DB_PORT is empty}
ITF_REPORTING_DB="$(env_default "${ITF_REPORTING_DB}" "${SERVICE_NAME}" "${_ns}")"
ITF_REPORTING_DB_USER="$(env_default "${ITF_REPORTING_DB_USER}" "${SERVICE_NAME}" "${_ns}")"
ITF_REPORTING_DB_PASSWORD="$(env_default "${ITF_REPORTING_DB_PASSWORD}" "${SERVICE_NAME}" "${_ns}")"

init_pg "${PG_DB_ADDR}" "${ITF_REPORTING_DB}" "${ITF_REPORTING_DB_USER}" "${ITF_REPORTING_DB_PASSWORD}" "${PG_DB_PORT}" "${pg_user}" "${pg_pass}"

if [ "${MULTI_TENANCY_HIBERNATE_ENABLED:-false}" = "true" ]; then
  echo "Multi-tenancy-hibernate is enabled"
  _clusters="$(env | grep -e 'ADDITIONAL_.*CLUSTER.*_URL'|cut -d _ -f3|sort)"
  if [ -n "${_clusters}" ]; then
  echo "Additional clusters:"
  for cluster in ${_clusters}
    do
      _url="ADDITIONAL_PG_${cluster}_URL"
      _user="ADDITIONAL_PG_${cluster}_USERNAME"
      _password="ADDITIONAL_PG_${cluster}_PASSWORD"
      echo "${cluster} :=>  $(getValue "$_url")"
      ADDITIONAL_DB_HOST=$(getValue "$_url"|awk -F ':' '{print $3}'|sed 's/\/\///g')
      ADDITIONAL_DB_NAME=$(getValue "$_url"|awk -F ":" '{print $4}'|cut -d / -f2)
      ADDITIONAL_DB_PORT=$(getValue "$_url"|awk -F ":" '{print $4}'|cut -d / -f1)
      ADDITIONAL_DB_USER=$(getValue "$_user")
      ADDITIONAL_DB_PASSWORD=$(getValue "$_password")
      init_pg "${ADDITIONAL_DB_HOST:?}" "${ADDITIONAL_DB_NAME:?}" \
              "${ADDITIONAL_DB_USER:?}" "${ADDITIONAL_DB_PASSWORD:?}" \
              "${ADDITIONAL_DB_PORT:?}" "${pg_user}" "${pg_pass}" < /dev/null
    done
  else
    echo "List of additional clusters is empty."
  fi
else
  echo "Multi-tenancy-hibernate is disabled"
fi

