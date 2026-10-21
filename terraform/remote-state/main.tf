resource "aws_ssm_parameter" "demo" {
  name           = "/learn-aws/remote-state/demo"
  type           = "String"
  insecure_value = "value1"
}