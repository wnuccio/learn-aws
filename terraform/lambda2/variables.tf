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
