variable "product" {
}

variable "component" {
}

variable "location" {
  default = "UK South"
}

variable "raw_product" {
  default = "ccd" // jenkins-library overrides product for PRs and adds e.g. pr-118-ccd
}

variable "env" {
type = "string"
}
