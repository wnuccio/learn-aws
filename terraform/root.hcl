locals {
  aws_region = "eu-west-3"
}

generate "provider" {
  path      = "provider.tf"
  if_exists = "overwrite_terragrunt"
  contents  = <<EOF
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}
EOF
}

inputs = {
  aws_region = local.aws_region
  jar_path   = "${get_parent_terragrunt_dir()}/../target/learn-aws.jar"
}
