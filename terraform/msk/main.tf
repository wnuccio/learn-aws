terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

data "aws_availability_zones" "available" {
  state = "available"
}

# --- Network ---------------------------------------------------------------
# Private-only VPC: no internet gateway and no NAT, so the network itself is free.

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/24"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.name}-vpc"
  }
}

# MSK requires subnets in at least two availability zones
resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = ["10.0.0.0/25", "10.0.0.128/25"][count.index]
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "${var.name}-private-${count.index}"
  }
}

resource "aws_security_group" "lambda" {
  name   = "${var.name}-lambda-sg"
  vpc_id = aws_vpc.main.id
}

resource "aws_security_group" "msk" {
  name   = "${var.name}-msk-sg"
  vpc_id = aws_vpc.main.id
}

# 9098 is the Kafka port for IAM authentication
resource "aws_vpc_security_group_egress_rule" "lambda_to_msk" {
  security_group_id            = aws_security_group.lambda.id
  referenced_security_group_id = aws_security_group.msk.id
  ip_protocol                  = "tcp"
  from_port                    = 9098
  to_port                      = 9098
}

resource "aws_vpc_security_group_ingress_rule" "msk_from_lambda" {
  security_group_id            = aws_security_group.msk.id
  referenced_security_group_id = aws_security_group.lambda.id
  ip_protocol                  = "tcp"
  from_port                    = 9098
  to_port                      = 9098
}

# --- MSK -------------------------------------------------------------------
# Serverless: no brokers to size. Billed hourly plus per partition and per GB,
# so run `terraform destroy` right after testing.

resource "aws_msk_serverless_cluster" "main" {
  cluster_name = "${var.name}-cluster"

  vpc_config {
    subnet_ids         = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.msk.id]
  }

  client_authentication {
    sasl {
      iam {
        enabled = true
      }
    }
  }
}