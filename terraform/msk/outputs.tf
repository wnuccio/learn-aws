output "api_invoke_url" {
  description = "Invoke URL for the HTTP API"
  value       = aws_apigatewayv2_stage.default.invoke_url
}

output "producer_log_group" {
  description = "CloudWatch log group for the producer Lambda"
  value       = aws_cloudwatch_log_group.producer.name
}