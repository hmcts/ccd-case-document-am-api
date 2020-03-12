output "idam_url" {
  value = "${var.idam_api_url}"
}

output "s2s_url" {
  value = "${local.s2s_url}"
}

output "BEFTA_S2S_CLIENT_ID" {
  value = "ccd_case_document_am_api"
}

output "OAUTH2_CLIENT_ID" {
  value = "ccd_gateway"
}

output "OAUTH2_REDIRECT_URI" {
  value = "${local.oauth2_redirect_uri}"
}

output "DEFINITION_STORE_HOST" {
  value = "${local.definition_store_host}"
}

output "DOCUMENT_STORE_URL" {
  value = "${local.document_store_url}"
}
