variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "jar_path" {
  description = "Path of the Lambda jar"
  type        = string
}

variable "function_name" {
  description = "Lambda function name"
  type        = string
  default     = "HelloWorld"
}