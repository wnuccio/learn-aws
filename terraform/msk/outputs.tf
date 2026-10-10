output "api_invoke_url" {
  description = "Invoke URL for the HTTP API"
  value       = aws_apigatewayv2_stage.default.invoke_url
}

output "lambda_log_group" {
  description = "CloudWatch log group shared by the Lambdas"
  value       = aws_cloudwatch_log_group.lambda.name
}