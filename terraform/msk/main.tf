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

# --- Producer Lambda -------------------------------------------------------

resource "aws_iam_role" "producer_role" {
  name = "${var.name}-producer-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

# Includes the ENI permissions Lambda needs to attach to the VPC
resource "aws_iam_role_policy_attachment" "producer_vpc_execution" {
  role       = aws_iam_role.producer_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

# The topic ARN is built from the cluster ARN by swapping ":cluster/" for ":topic/", then appending "/*" to cover every topic
resource "aws_iam_role_policy" "producer_kafka" {
  name = "${var.name}-producer-kafka"
  role = aws_iam_role.producer_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "kafka-cluster:Connect",
          "kafka-cluster:WriteDataIdempotently"
        ]
        Resource = aws_msk_serverless_cluster.main.arn
      },
      {
        Effect = "Allow"
        Action = [
          "kafka-cluster:CreateTopic",
          "kafka-cluster:DescribeTopic",
          "kafka-cluster:WriteData"
        ]
        Resource = "${replace(aws_msk_serverless_cluster.main.arn, ":cluster/", ":topic/")}/*"
      }
    ]
  })
}