variable "product" {
  default = "ccd"
}

variable "component" {
  default = "case-document-am-api"
}

variable "location" {
  default = "UK South"
}

variable "raw_product" {
  default = "ccd" // jenkins-library overrides product for PRs and adds e.g. pr-118-ccd
}

variable "env" {}

variable "subscription" {}

variable "common_tags" {
  type = "map"
}

variable "ilbIp"{}

variable "additional_host_name" {
  description = "A custom domain name for this webapp."
  default = "null"
}

variable "asp_name" {
  type = "string"
  description = "App Service Plan (ASP) to use for the webapp, 'use_shared' to make use of the shared ASP"
  default = "use_shared"
}

variable "asp_rg" {
  type = "string"
  description = "App Service Plan (ASP) resource group for 'asp_name', 'use_shared' to make use of the shared resource group"
  default = "use_shared"
}

variable "capacity" {
  default = "1"
}

variable "appinsights_instrumentation_key" {
  description = "Instrumentation key of the App Insights instance this webapp should use. Module will create own App Insights resource if this is not provided"
  default = ""
}

variable "enable_ase" {
  default = false
}

variable "idam_api_url" {
  default = "http://betaDevBccidamAppLB.reform.hmcts.net"
}

variable "http_client_connection_timeout" {
  type = "string"
  default = "30000"
}

variable "http_client_read_timeout" {
  default = "60000"
}

variable "http_client_max_total" {
  type = "string"
  default = "100"
}

variable "http_client_seconds_idle_connection" {
  type = "string"
  default = "120"
}

variable "http_client_max_client_per_route" {
  type = "string"
  default = "20"
}

variable "http_client_validate_after_inactivity" {
  type = "string"
  default = "0"
}

variable "frontend_url" {
  description = "Optional front end URL to use for building redirect URI"
  type = "string"
  default = ""
}

variable "authorised-services" {
  type    = "string"
  default = "ccd_case_document_am_api,ccd_gw,xui_webapp"
}

variable "deployment_namespace" {}
