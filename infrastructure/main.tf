provider "azurerm" {
  version = "1.27"
}

locals {
  app_full_name = "${var.product}-${var.component}"

  aseName = "core-compute-${var.env}"
  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"
  local_ase = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "core-compute-aat" : "core-compute-saat" : local.aseName}"
  env_ase_url = "${local.local_env}.service.${local.local_ase}.internal"

  // Vault name
  previewVaultName = "${var.raw_product}-aat"
  nonPreviewVaultName = "${var.raw_product}-${var.env}"
  vaultName = "${(var.env == "preview" || var.env == "spreview") ? local.previewVaultName : local.nonPreviewVaultName}"

  // Shared Resource Group
  previewResourceGroup = "${var.raw_product}-shared-aat"
  nonPreviewResourceGroup = "${var.raw_product}-shared-${var.env}"
  sharedResourceGroup = "${(var.env == "preview" || var.env == "spreview") ? local.previewResourceGroup : local.nonPreviewResourceGroup}"

  sharedAppServicePlan = "${var.raw_product}-${var.env}"
  sharedASPResourceGroup = "${var.raw_product}-shared-${var.env}"

  // S2S
  s2s_url = "http://rpe-service-auth-provider-${local.env_ase_url}"

 }

data "azurerm_key_vault" "ccd_shared_key_vault" {
  name = "${local.vaultName}"
  resource_group_name = "${local.sharedResourceGroup}"
}

data "azurerm_key_vault" "s2s_vault" {
  name = "s2s-${local.local_env}"
  resource_group_name = "rpe-service-auth-provider-${local.local_env}"
}

data "azurerm_key_vault_secret" "ccd-case-document-am-api_s2s_key" {
  name = "microservicekey-ccd-case-document-am-api"
  key_vault_id = "${data.azurerm_key_vault.s2s_vault.id}"
}

resource "azurerm_key_vault_secret" "ccd-case-document-am-api-s2s-secret" {
  name = "ccd-case-document-am-api-s2s-secret"
  value = "${data.azurerm_key_vault_secret.ccd-case-document-am-api_s2s_key.value}"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}


resource "random_string" "draft_encryption_key" {
  length  = 16
  special = true
  upper   = true
  lower   = true
  number  = true
  lifecycle {
    ignore_changes = ["*"]
  }
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-shared-${var.env}"
  location = "${var.location}"
}

module "ccd-case-document-am-api" {
  source   = "git@github.com:hmcts/cnp-module-webapp?ref=master"
  product  = "${local.app_full_name}"
  location = "${var.location}"
  appinsights_location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"
  subscription = "${var.subscription}"
  is_frontend = false
  common_tags  = "${var.common_tags}"
  additional_host_name = "${var.additional_host_name}"
  asp_name = "${(var.asp_name == "use_shared") ? local.sharedAppServicePlan : var.asp_name}"
  asp_rg = "${(var.asp_rg == "use_shared") ? local.sharedASPResourceGroup : var.asp_rg}"
  website_local_cache_sizeinmb = 2000
  capacity = "${var.capacity}"
  java_container_version = "9.0"
  appinsights_instrumentation_key = "${var.appinsights_instrumentation_key}"
  enable_ase                      = "${var.enable_ase}"

  app_settings = {
    ENABLE_DB_MIGRATE = "false"

    IDAM_USER_URL                       = "${var.idam_api_url}"
    IDAM_S2S_URL                        = "${local.s2s_url}"
    DATA_STORE_IDAM_KEY                 = "${data.azurerm_key_vault_secret.ccd-case-document-am-api_s2s_key.value}"

    CCD_DRAFT_ENCRYPTION_KEY            = "${random_string.draft_encryption_key.result}"

    HTTP_CLIENT_CONNECTION_TIMEOUT        = "${var.http_client_connection_timeout}"
    HTTP_CLIENT_READ_TIMEOUT              = "${var.http_client_read_timeout}"
    HTTP_CLIENT_MAX_TOTAL                 = "${var.http_client_max_total}"
    HTTP_CLIENT_SECONDS_IDLE_CONNECTION   = "${var.http_client_seconds_idle_connection}"
    HTTP_CLIENT_MAX_CLIENT_PER_ROUTE      = "${var.http_client_max_client_per_route}"
    HTTP_CLIENT_VALIDATE_AFTER_INACTIVITY = "${var.http_client_validate_after_inactivity}"

    JPA_CRITERIA_IN_SEARCH_ENABLED        = false
  }

}
