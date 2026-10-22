locals {
  root = read_terragrunt_config(find_in_parent_folders("root.hcl"))
}

remote_state {
  backend = "s3"

  generate = {
    path      = "backend.tf"
    if_exists = "overwrite_terragrunt"
  }

  config = {
    bucket       = "learn-aws-tfstate-${get_aws_account_id()}"
    key          = "${path_relative_to_include("backend")}/terraform.tfstate"
    region       = local.root.inputs.aws_region
    encrypt      = true
    use_lockfile = true
  }
}
