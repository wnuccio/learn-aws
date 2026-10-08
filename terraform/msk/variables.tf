variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-west-3"
}

variable "name" {
  description = "Name prefix for the resources"
  type        = string
  default     = "msk-demo"
}