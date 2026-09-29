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

data "aws_caller_identity" "current" {}

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

resource "aws_iam_role_policy_attachment" "lambda_basic_execution" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "lambda_s3_policy" {
  name   = "${var.function_name}-s3-policy"
  role   = aws_iam_role.lambda_role.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "s3:PutObject"
        ]
        Effect   = "Allow"
        Resource = "${aws_s3_bucket.helloworld.arn}/*"
      }
    ]
  })
}

resource "aws_lambda_function" "helloworld" {
  filename      = var.jar_path
  function_name = var.function_name
  role          = aws_iam_role.lambda_role.arn
  handler       = var.handler
  runtime       = var.runtime
  timeout       = 60

  source_code_hash = filebase64sha256(var.jar_path)

  environment {
    variables = {
      S3_BUCKET_NAME = aws_s3_bucket.helloworld.id
      APP_REGION     = var.aws_region
    }
  }
}

resource "aws_apigatewayv2_api" "helloworld" {
  name          = "${var.function_name}-api"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_integration" "helloworld" {
  api_id                 = aws_apigatewayv2_api.helloworld.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.helloworld.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "helloworld" {
  api_id    = aws_apigatewayv2_api.helloworld.id
  route_key = "GET /hello"
  target    = "integrations/${aws_apigatewayv2_integration.helloworld.id}"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.helloworld.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.helloworld.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.helloworld.execution_arn}/*/*"
}

resource "aws_s3_bucket" "helloworld" {
  bucket = "learn-aws-helloworld-${data.aws_caller_identity.current.account_id}"
}