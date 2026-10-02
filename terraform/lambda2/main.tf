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

data "aws_availability_zones" "available" {
  state = "available"
}

# --- Network ---------------------------------------------------------------
# Private-only VPC: no internet gateway and no NAT, so nothing here costs money
# beyond the RDS instance. The Lambda only needs to reach RDS inside the VPC.

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/24"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.function_name}-vpc"
  }
}

# RDS requires a subnet group spanning at least two availability zones
resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = ["10.0.0.0/25", "10.0.0.128/25"][count.index]
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "${var.function_name}-private-${count.index}"
  }
}

resource "aws_security_group" "lambda" {
  name   = "${var.function_name}-lambda-sg"
  vpc_id = aws_vpc.main.id
}

resource "aws_security_group" "rds" {
  name   = "${var.function_name}-rds-sg"
  vpc_id = aws_vpc.main.id
}

resource "aws_vpc_security_group_egress_rule" "lambda_to_rds" {
  security_group_id            = aws_security_group.lambda.id
  referenced_security_group_id = aws_security_group.rds.id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_lambda" {
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = aws_security_group.lambda.id
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
}

# --- Database --------------------------------------------------------------

resource "aws_db_subnet_group" "main" {
  name       = "${lower(var.function_name)}-db-subnets"
  subnet_ids = aws_subnet.private[*].id
}

# No special characters: RDS forbids some of them (/ @ " space) and they'd need escaping in JDBC anyway
resource "random_password" "db" {
  length  = 24
  special = false
}

resource "aws_db_instance" "main" {
  identifier             = "${lower(var.function_name)}-db"
  engine                 = "postgres"I''
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  storage_type           = "gp3"
  db_name                = var.db_name
  username               = var.db_username
  password               = random_password.db.result
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  skip_final_snapshot    = true
}

# --- Lambda ----------------------------------------------------------------

resource "aws_iam_role" "lambda_role" {
  name = "${var.function_name}-role"

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
resource "aws_iam_role_policy_attachment" "lambda_vpc_execution" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

resource "aws_lambda_function" "helloworld2" {
  filename      = "../../target/learn-aws.jar"
  function_name = var.function_name
  role          = aws_iam_role.lambda_role.arn
  handler       = "org.example.lambda2.HelloWorld2Handler"
  runtime       = "java17"
  timeout       = 30
  memory_size   = 1024

  source_code_hash = filebase64sha256("../../target/learn-aws.jar")

  vpc_config {
    subnet_ids         = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.lambda.id]
  }

  environment {
    variables = {
      DB_URL      = "jdbc:postgresql://${aws_db_instance.main.address}:${aws_db_instance.main.port}/${var.db_name}"
      DB_USER     = var.db_username
      DB_PASSWORD = random_password.db.result
    }
  }

  depends_on = [aws_cloudwatch_log_group.lambda_log_group]
}

# --- API Gateway -----------------------------------------------------------

resource "aws_apigatewayv2_api" "helloworld2" {
  name          = "${var.function_name}-api"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_integration" "helloworld2" {
  api_id                 = aws_apigatewayv2_api.helloworld2.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.helloworld2.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "helloworld2" {
  api_id    = aws_apigatewayv2_api.helloworld2.id
  target    = "integrations/${aws_apigatewayv2_integration.helloworld2.id}"
  route_key = "GET /hello"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.helloworld2.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway_log_group.arn
    format = jsonencode({
      requestId               = "$context.requestId"
      routeKey                = "$context.routeKey"
      status                  = "$context.status"
      integrationErrorMessage = "$context.integration.error"
    })
  }
}

resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.helloworld2.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.helloworld2.execution_arn}/*/*"
}

resource "aws_cloudwatch_log_group" "lambda_log_group" {
  name              = "/aws/lambda/${var.function_name}"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "api_gateway_log_group" {
  name              = "/aws/apigateway/${var.function_name}"
  retention_in_days = 7
}
