variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-west-3"
}

variable "function_name" {
  description = "Lambda function name"
  type        = string
  default     = "HelloWorld"
}