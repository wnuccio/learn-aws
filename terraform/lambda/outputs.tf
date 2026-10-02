output "api_invoke_url" {
  description = "Invoke URL for the HTTP API"
  value       = aws_apigatewayv2_stage.default.invoke_url
}

output "api_gateway_log_group" {
  description = "CloudWatch log group for API Gateway"
  value       = aws_cloudwatch_log_group.api_gateway_log_group.name
}