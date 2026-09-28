output "lambda_function_arn" {
  description = "ARN of the Lambda function"
  value       = aws_lambda_function.helloworld.arn
}

output "lambda_function_name" {
  description = "Name of the Lambda function"
  value       = aws_lambda_function.helloworld.function_name
}

output "iam_role_arn" {
  description = "ARN of the IAM role for Lambda"
  value       = aws_iam_role.lambda_role.arn
}

output "api_invoke_url" {
  description = "Invoke URL for the HTTP API"
  value       = aws_apigatewayv2_stage.default.invoke_url
}