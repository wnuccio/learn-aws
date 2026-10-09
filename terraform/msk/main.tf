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

# The event source mapping reaches the brokers through an ENI that uses the cluster's security group, not the Lambda's,
# so the cluster group needs self-referencing rules on the broker port
resource "aws_vpc_security_group_ingress_rule" "msk_from_msk" {
  security_group_id            = aws_security_group.msk.id
  referenced_security_group_id = aws_security_group.msk.id
  ip_protocol                  = "tcp"
  from_port                    = 9098
  to_port                      = 9098
}

resource "aws_vpc_security_group_egress_rule" "msk_to_msk" {
  security_group_id            = aws_security_group.msk.id
  referenced_security_group_id = aws_security_group.msk.id
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

# --- API Gateway -----------------------------------------------------------

resource "aws_apigatewayv2_api" "producer" {
  name          = "${var.name}-producer-api"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_integration" "producer" {
  api_id                 = aws_apigatewayv2_api.producer.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.producer.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "post_messages" {
  api_id    = aws_apigatewayv2_api.producer.id
  target    = "integrations/${aws_apigatewayv2_integration.producer.id}"
  route_key = "POST /messages"
}

resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.producer.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.producer.execution_arn}/*/*"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.producer.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway.arn
    format = jsonencode({
      requestId               = "$context.requestId"
      routeKey                = "$context.routeKey"
      status                  = "$context.status"
      integrationErrorMessage = "$context.integration.error"
    })
  }
}

resource "aws_cloudwatch_log_group" "api_gateway" {
  name              = "/aws/apigateway/${var.name}-producer"
  retention_in_days = 7
}

# Managed here so that `terraform destroy` removes the logs too
resource "aws_cloudwatch_log_group" "producer" {
  name              = "/aws/lambda/${var.name}-producer"
  retention_in_days = 7
}

# The first call on a cold container creates the topic and opens the Kafka connection, hence the generous timeout
resource "aws_lambda_function" "producer" {
  filename      = "../../target/learn-aws.jar"
  function_name = "${var.name}-producer"
  role          = aws_iam_role.producer_role.arn
  handler       = "org.example.msk.ProducerHandler"
  runtime       = "java17"
  timeout       = 60
  memory_size   = 1024

  source_code_hash = filebase64sha256("../../target/learn-aws.jar")

  vpc_config {
    subnet_ids         = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.lambda.id]
  }

  environment {
    variables = {
      BOOTSTRAP_SERVERS = aws_msk_serverless_cluster.main.bootstrap_brokers_sasl_iam
      TOPIC             = var.topic
    }
  }

  depends_on = [
    aws_cloudwatch_log_group.producer,
    aws_iam_role_policy_attachment.producer_vpc_execution,
    aws_iam_role_policy.producer_kafka
  ]
}

# --- Consumer Lambda -------------------------------------------------------

resource "aws_iam_role" "consumer_role" {
  name = "${var.name}-consumer-role"

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

# Logs, the cluster lookups the event source mapping needs (kafka:Describe*, kafka:GetBootstrapBrokers) and the ENI permissions
resource "aws_iam_role_policy_attachment" "consumer_msk_execution" {
  role       = aws_iam_role.consumer_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaMSKExecutionRole"
}

# The event source mapping polls Kafka with this role, so it needs the consumer-side IAM auth actions.
# Topic and group ARNs are built from the cluster ARN like the producer's topic ARN.
resource "aws_iam_role_policy" "consumer_kafka" {
  name = "${var.name}-consumer-kafka"
  role = aws_iam_role.consumer_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "kafka-cluster:Connect"
        Resource = aws_msk_serverless_cluster.main.arn
      },
      {
        Effect = "Allow"
        Action = [
          "kafka-cluster:DescribeTopic",
          "kafka-cluster:ReadData"
        ]
        Resource = "${replace(aws_msk_serverless_cluster.main.arn, ":cluster/", ":topic/")}/*"
      },
      {
        Effect = "Allow"
        Action = [
          "kafka-cluster:DescribeGroup",
          "kafka-cluster:AlterGroup"
        ]
        Resource = "${replace(aws_msk_serverless_cluster.main.arn, ":cluster/", ":group/")}/*"
      }
    ]
  })
}

resource "aws_cloudwatch_log_group" "consumer" {
  name              = "/aws/lambda/${var.name}-consumer"
  retention_in_days = 7
}

# Not VPC-attached: the event source mapping polls Kafka and invokes the function through the Lambda service,
# and the function only logs what it receives
resource "aws_lambda_function" "consumer" {
  filename      = "../../target/learn-aws.jar"
  function_name = "${var.name}-consumer"
  role          = aws_iam_role.consumer_role.arn
  handler       = "org.example.msk.ConsumerHandler"
  runtime       = "java17"
  timeout       = 30
  memory_size   = 1024

  source_code_hash = filebase64sha256("../../target/learn-aws.jar")

  depends_on = [
    aws_cloudwatch_log_group.consumer,
    aws_iam_role_policy_attachment.consumer_msk_execution,
    aws_iam_role_policy.consumer_kafka
  ]
}

# The mapping polls the topic with the consumer role and invokes the function with batches of records.
# The provisioned_poller_config block switches it to provisioned mode: no NAT or VPC endpoints needed.
# TRIM_HORIZON because the topic is created by the first producer call, possibly after the mapping exists.
resource "aws_lambda_event_source_mapping" "consumer" {
  event_source_arn  = aws_msk_serverless_cluster.main.arn
  function_name     = aws_lambda_function.consumer.arn
  topics            = [var.topic]
  starting_position = "TRIM_HORIZON"

  provisioned_poller_config {
    minimum_pollers = 1
    maximum_pollers = 2
  }

  depends_on = [
    aws_iam_role_policy_attachment.consumer_msk_execution,
    aws_iam_role_policy.consumer_kafka
  ]
}