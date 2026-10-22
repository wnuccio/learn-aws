resource "aws_ssm_parameter" "demo" {
  name           = var.parameter_name
  type           = "String"
  insecure_value = var.parameter_value
}