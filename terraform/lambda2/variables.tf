variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-west-3"
}

variable "function_name" {
  description = "Lambda function name"
  type        = string
  default     = "HelloWorld2"
}

variable "db_name" {
  description = "Name of the initial database"
  type        = string
  default     = "helloworld"
}

variable "db_username" {
  description = "RDS master username"
  type        = string
  default     = "helloworld"
}
