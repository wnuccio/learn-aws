include "root" {
  path = find_in_parent_folders("root.hcl")
}

include "backend" {
  path = find_in_parent_folders("backend.hcl")
}

locals {
  env_vars = read_terragrunt_config(find_in_parent_folders("env.hcl")).locals
}

terraform {
  source = "../../../modules/remote-state"
}

inputs = {
  parameter_name  = "/learn-aws/${local.env_vars.env}/demo"
  parameter_value = local.env_vars.parameter_value
}