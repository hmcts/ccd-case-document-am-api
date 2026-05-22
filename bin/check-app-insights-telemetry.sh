#!/usr/bin/env sh

set -eu

APP_INSIGHTS_ENV="${APP_INSIGHTS_ENV:-aat}"
APP_INSIGHTS_APP_NAME="${APP_INSIGHTS_APP_NAME:-ccd-${APP_INSIGHTS_ENV}}"
APP_INSIGHTS_RESOURCE_GROUP="${APP_INSIGHTS_RESOURCE_GROUP:-ccd-shared-${APP_INSIGHTS_ENV}}"
APP_INSIGHTS_ROLE_NAME="${APP_INSIGHTS_ROLE_NAME:-ccd-case-document-am-api}"
APP_INSIGHTS_LOOKBACK="${APP_INSIGHTS_LOOKBACK:-60m}"
APP_INSIGHTS_TIMEOUT_SECONDS="${APP_INSIGHTS_TIMEOUT_SECONDS:-600}"
APP_INSIGHTS_POLL_SECONDS="${APP_INSIGHTS_POLL_SECONDS:-30}"
APP_INSIGHTS_TRACE_MARKER="${APP_INSIGHTS_TRACE_MARKER:-LA-CDAM}"
REQUIRE_DEPENDENCY_TELEMETRY="${REQUIRE_DEPENDENCY_TELEMETRY:-true}"
REQUIRE_TRACE_TELEMETRY="${REQUIRE_TRACE_TELEMETRY:-true}"

if ! command -v az >/dev/null 2>&1; then
  echo "Azure CLI 'az' is required to query Application Insights."
  exit 2
fi

if ! az account show >/dev/null 2>&1; then
  if [ -n "${AZURE_CLIENT_ID:-}" ] && [ -n "${AZURE_CLIENT_SECRET:-}" ] && [ -n "${AZURE_TENANT_ID:-}" ]; then
    echo "Azure CLI is not logged in. Logging in with supplied service principal credentials."
    az login \
      --service-principal \
      --username "$AZURE_CLIENT_ID" \
      --password "$AZURE_CLIENT_SECRET" \
      --tenant "$AZURE_TENANT_ID" \
      --output none
  else
    echo "Azure CLI is not logged in or has no active subscription."
    echo "Set AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, and AZURE_TENANT_ID, or run this from an authenticated az session."
    exit 2
  fi
fi

if [ -n "${AZURE_SUBSCRIPTION_ID:-}" ]; then
  az account set --subscription "$AZURE_SUBSCRIPTION_ID"
fi

is_true() {
  case "$1" in
    true|TRUE|True|1|yes|YES|Yes) return 0 ;;
    *) return 1 ;;
  esac
}

query_count() {
  query="$1"
  if ! count=$(az monitor app-insights query \
      --app "$APP_INSIGHTS_APP_NAME" \
      --resource-group "$APP_INSIGHTS_RESOURCE_GROUP" \
      --analytics-query "$query" \
      --query "tables[0].rows[0][0]" \
      --output tsv 2>&1); then
    echo "Failed to query Application Insights:"
    echo "$count"
    exit 2
  fi

  if [ -z "$count" ]; then
    count=0
  fi

  echo "$count"
}

requests_query="
requests
| where timestamp > ago(${APP_INSIGHTS_LOOKBACK})
| where cloud_RoleName == '${APP_INSIGHTS_ROLE_NAME}'
| summarize Count=count()
"

dependencies_query="
dependencies
| where timestamp > ago(${APP_INSIGHTS_LOOKBACK})
| where cloud_RoleName == '${APP_INSIGHTS_ROLE_NAME}'
| summarize Count=count()
"

traces_query="
traces
| where timestamp > ago(${APP_INSIGHTS_LOOKBACK})
| where cloud_RoleName == '${APP_INSIGHTS_ROLE_NAME}'
| where message has '${APP_INSIGHTS_TRACE_MARKER}'
| summarize Count=count()
"

deadline=$(( $(date +%s) + APP_INSIGHTS_TIMEOUT_SECONDS ))

echo "Checking Application Insights telemetry"
echo "  app: ${APP_INSIGHTS_APP_NAME}"
echo "  resource group: ${APP_INSIGHTS_RESOURCE_GROUP}"
echo "  cloud role: ${APP_INSIGHTS_ROLE_NAME}"
echo "  lookback: ${APP_INSIGHTS_LOOKBACK}"

while true; do
  request_count=$(query_count "$requests_query")
  dependency_count=$(query_count "$dependencies_query")
  trace_count=$(query_count "$traces_query")

  echo "Telemetry counts: requests=${request_count}, dependencies=${dependency_count}, traces=${trace_count}"

  passed=true

  if [ "$request_count" -lt 1 ]; then
    passed=false
  fi

  if is_true "$REQUIRE_DEPENDENCY_TELEMETRY" && [ "$dependency_count" -lt 1 ]; then
    passed=false
  fi

  if is_true "$REQUIRE_TRACE_TELEMETRY" && [ "$trace_count" -lt 1 ]; then
    passed=false
  fi

  if [ "$passed" = "true" ]; then
    echo "Application Insights telemetry check passed."
    exit 0
  fi

  if [ "$(date +%s)" -ge "$deadline" ]; then
    echo "Application Insights telemetry check failed before timeout."
    echo "Expected at least one request telemetry item."
    if is_true "$REQUIRE_DEPENDENCY_TELEMETRY"; then
      echo "Expected at least one dependency telemetry item."
    fi
    if is_true "$REQUIRE_TRACE_TELEMETRY"; then
      echo "Expected at least one trace containing '${APP_INSIGHTS_TRACE_MARKER}'."
    fi
    exit 1
  fi

  sleep "$APP_INSIGHTS_POLL_SECONDS"
done
