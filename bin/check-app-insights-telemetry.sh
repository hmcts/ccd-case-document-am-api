#!/usr/bin/env sh

set -eu

APP_INSIGHTS_ENV="${APP_INSIGHTS_ENV:-aat}"
APP_INSIGHTS_APP_NAME="${APP_INSIGHTS_APP_NAME:-${APP_INSIGHTS_APP:-ccd-${APP_INSIGHTS_ENV}}}"
APP_INSIGHTS_RESOURCE_GROUP="${APP_INSIGHTS_RESOURCE_GROUP:-ccd-shared-${APP_INSIGHTS_ENV}}"
APP_INSIGHTS_ROLE_NAME="${APP_INSIGHTS_ROLE_NAME:-ccd-case-document-am-api}"
APP_INSIGHTS_LOOKBACK="${APP_INSIGHTS_LOOKBACK:-${APP_INSIGHTS_TELEMETRY_LOOKBACK:-2h}}"
APP_INSIGHTS_TIMEOUT_SECONDS="${APP_INSIGHTS_TIMEOUT_SECONDS:-600}"
APP_INSIGHTS_POLL_SECONDS="${APP_INSIGHTS_POLL_SECONDS:-30}"
APP_INSIGHTS_SOURCE_ENV="${APP_INSIGHTS_SOURCE_ENV:-${APP_INSIGHTS_ENV}}"
APP_INSIGHTS_REQUEST_URL_CONTAINS="${APP_INSIGHTS_REQUEST_URL_CONTAINS:-}"
REQUIRE_DEPENDENCY_TELEMETRY="${REQUIRE_DEPENDENCY_TELEMETRY:-true}"
REQUIRE_TRACE_TELEMETRY="${REQUIRE_TRACE_TELEMETRY:-true}"
attempt=1

resolve_trace_marker() {
  if [ -n "${APP_INSIGHTS_TRACE_MARKER:-}" ]; then
    echo "$APP_INSIGHTS_TRACE_MARKER"
    return
  fi

  if [ ! -x "./gradlew" ]; then
    echo "APP_INSIGHTS_TRACE_MARKER is not set and ./gradlew is not available to resolve the audit log tag." >&2
    exit 2
  fi

  if ! marker="$(./gradlew -q printAuditLogTag 2>&1)"; then
    echo "Failed to resolve audit log tag from AuditLogFormatter.TAG:" >&2
    echo "$marker" >&2
    exit 2
  fi

  marker="$(printf '%s\n' "$marker" | sed '/^[[:space:]]*$/d' | tail -n 1)"

  if [ -z "$marker" ]; then
    echo "AuditLogFormatter.TAG resolved to an empty value." >&2
    exit 2
  fi

  echo "$marker"
}

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
else
  AZURE_SUBSCRIPTION_ID="$(az account show --query id --output tsv)"
fi

APP_INSIGHTS_TRACE_MARKER="$(resolve_trace_marker)"

if [ -z "$APP_INSIGHTS_REQUEST_URL_CONTAINS" ] && [ "$APP_INSIGHTS_SOURCE_ENV" = "preview" ]; then
  case "${BRANCH_NAME:-}" in
    PR-*|pr-*)
      APP_INSIGHTS_REQUEST_URL_CONTAINS="$(printf '%s' "${APP_INSIGHTS_ROLE_NAME}-${BRANCH_NAME}.preview.platform.hmcts.net" | tr '[:upper:]' '[:lower:]')"
      ;;
  esac
fi

if [ -z "$APP_INSIGHTS_REQUEST_URL_CONTAINS" ] && [ "$APP_INSIGHTS_SOURCE_ENV" = "preview" ]; then
  echo "Preview telemetry check requires APP_INSIGHTS_REQUEST_URL_CONTAINS or a PR-* BRANCH_NAME." >&2
  exit 2
fi

is_true() {
  case "$1" in
    true|TRUE|True|1|yes|YES|Yes) return 0 ;;
    *) return 1 ;;
  esac
}

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

kql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

query_telemetry_counts() {
  query="$1"
  escaped_query="$(json_escape "$query")"
  body="{\"query\":\"${escaped_query}\"}"
  error_file="$(mktemp)"

  echo "Querying telemetry..." >&2

  if [ -n "${APP_INSIGHTS_RESOURCE_ID:-}" ]; then
    uri="https://management.azure.com${APP_INSIGHTS_RESOURCE_ID}/query?api-version=2018-04-20"
  else
    uri="https://management.azure.com/subscriptions/${AZURE_SUBSCRIPTION_ID}/resourceGroups/${APP_INSIGHTS_RESOURCE_GROUP}/providers/Microsoft.Insights/components/${APP_INSIGHTS_APP_NAME}/query?api-version=2018-04-20"
  fi

  if ! counts=$(az rest \
      --method post \
      --uri "$uri" \
      --headers "Content-Type=application/json" \
      --body "$body" \
      --query "tables[0].rows[0]" \
      --output tsv 2>"$error_file"); then
    echo "Failed to query telemetry from Application Insights:" >&2
    cat "$error_file" >&2
    rm -f "$error_file"
    exit 2
  fi
  rm -f "$error_file"

  set -- $counts
  if [ "$#" -ne 3 ]; then
    echo "Unexpected telemetry query result:" >&2
    echo "$counts" >&2
    exit 2
  fi

  for count in "$1" "$2" "$3"; do
    case "$count" in
      ''|*[!0-9]*)
        echo "Unexpected telemetry count:" >&2
        echo "$count" >&2
        exit 2
        ;;
    esac
  done

  printf '%s %s %s\n' "$1" "$2" "$3"
}

role_name="$(kql_escape "$APP_INSIGHTS_ROLE_NAME")"
trace_marker="$(kql_escape "$APP_INSIGHTS_TRACE_MARKER")"
base_filter="timestamp > ago(${APP_INSIGHTS_LOOKBACK}) | where cloud_RoleName == '${role_name}'"
request_filter="$base_filter"

if [ -n "$APP_INSIGHTS_REQUEST_URL_CONTAINS" ]; then
  request_url_contains="$(kql_escape "$APP_INSIGHTS_REQUEST_URL_CONTAINS")"
  request_filter="${request_filter} | where url contains '${request_url_contains}'"
fi

dependency_count_expression="0"
trace_count_expression="0"

if is_true "$REQUIRE_DEPENDENCY_TELEMETRY"; then
  dependency_count_expression="toscalar(dependencies | where ${base_filter} | where operation_Id in (request_operations) | summarize Count=count())"
fi

if is_true "$REQUIRE_TRACE_TELEMETRY"; then
  trace_count_expression="toscalar(traces | where ${base_filter} | where operation_Id in (request_operations) | where message contains '${trace_marker}' | summarize Count=count())"
fi

telemetry_query="let matching_requests = requests | where ${request_filter} | project operation_Id;"
telemetry_query="${telemetry_query} let request_operations = matching_requests | distinct operation_Id;"
telemetry_query="${telemetry_query} print RequestCount=toscalar(matching_requests | summarize Count=count()), DependencyCount=${dependency_count_expression}, TraceCount=${trace_count_expression}"

deadline=$(( $(date +%s) + APP_INSIGHTS_TIMEOUT_SECONDS ))

echo "Checking Application Insights telemetry"
echo "  app: ${APP_INSIGHTS_APP_NAME}"
echo "  resource group: ${APP_INSIGHTS_RESOURCE_GROUP}"
echo "  resource id: ${APP_INSIGHTS_RESOURCE_ID:-}"
echo "  cloud role: ${APP_INSIGHTS_ROLE_NAME}"
echo "  source env: ${APP_INSIGHTS_SOURCE_ENV}"
echo "  request URL contains: ${APP_INSIGHTS_REQUEST_URL_CONTAINS:-<not set>}"
echo "  marker: ${APP_INSIGHTS_TRACE_MARKER}"
echo "  lookback: ${APP_INSIGHTS_LOOKBACK}"
echo "  subscription: ${AZURE_SUBSCRIPTION_ID}"
echo "  required: requests=true, dependencies=${REQUIRE_DEPENDENCY_TELEMETRY}, traces=${REQUIRE_TRACE_TELEMETRY}"
echo "  dependency/trace scope: correlated by operation_Id to matching request telemetry"

while true; do
  echo "Application Insights telemetry check attempt ${attempt}"

  counts=$(query_telemetry_counts "$telemetry_query")
  set -- $counts
  request_count="$1"
  dependency_count="$2"
  trace_count="$3"

  request_status="PASS"
  dependency_status="SKIP"
  trace_status="SKIP"

  if [ "$request_count" -lt 1 ]; then
    request_status="FAIL"
  fi

  if is_true "$REQUIRE_DEPENDENCY_TELEMETRY"; then
    dependency_status="PASS"
    if [ "$dependency_count" -lt 1 ]; then
      dependency_status="FAIL"
    fi
  fi

  if is_true "$REQUIRE_TRACE_TELEMETRY"; then
    trace_status="PASS"
    if [ "$trace_count" -lt 1 ]; then
      trace_status="FAIL"
    fi
  fi

  echo "Telemetry result: requests=${request_status} (${request_count}), dependencies=${dependency_status} (${dependency_count}), traces=${trace_status} (${trace_count})"

  passed=true

  if [ "$request_status" = "FAIL" ]; then
    passed=false
  fi

  if [ "$dependency_status" = "FAIL" ]; then
    passed=false
  fi

  if [ "$trace_status" = "FAIL" ]; then
    passed=false
  fi

  if [ "$passed" = "true" ]; then
    echo "Application Insights telemetry check PASSED."
    exit 0
  fi

  if [ "$(date +%s)" -ge "$deadline" ]; then
    echo "Application Insights telemetry check FAILED before timeout."
    echo "Missing required telemetry:"
    [ "$request_status" = "FAIL" ] && echo "  - request telemetry for cloud role '${APP_INSIGHTS_ROLE_NAME}'"
    [ "$dependency_status" = "FAIL" ] && echo "  - dependency telemetry correlated with matching request telemetry"
    [ "$trace_status" = "FAIL" ] && echo "  - trace telemetry containing '${APP_INSIGHTS_TRACE_MARKER}' and correlated with matching request telemetry"
    exit 1
  fi

  echo "Telemetry not complete yet. Waiting ${APP_INSIGHTS_POLL_SECONDS}s for App Insights ingestion..."
  attempt=$((attempt + 1))
  sleep "$APP_INSIGHTS_POLL_SECONDS"
done
