provider "azurerm" {
  features {}
}

locals {
  vaultName = "${var.raw_product}-${var.env}"
  sharedResourceGroup = "${var.raw_product}-shared-${var.env}"
}

data "azurerm_key_vault" "ccd_shared_key_vault" {
  name                = "${local.vaultName}"
  resource_group_name = "${local.sharedResourceGroup}"
}

data "azurerm_key_vault" "s2s_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

data "azurerm_key_vault_secret" "ccd-case-document-am-api-s2s-vault-secret" {
  name         = "microservicekey-ccd-case-document-am-api"
  key_vault_id = "${data.azurerm_key_vault.s2s_vault.id}"
}

resource "azurerm_key_vault_secret" "ccd-case-document-am-api-s2s-secret" {
  name         = "ccd-case-document-am-api-s2s-secret"
  value        = "${data.azurerm_key_vault_secret.ccd-case-document-am-api-s2s-vault-secret.value}"
  key_vault_id = "${data.azurerm_key_vault.ccd_shared_key_vault.id}"
}

