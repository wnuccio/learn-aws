variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "parameter_name" {
  description = "Name of the SSM parameter"
  type        = string
}

variable "parameter_value" {
  description = "Value of the SSM parameter"
  type        = string
}