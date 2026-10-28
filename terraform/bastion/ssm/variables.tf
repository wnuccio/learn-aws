variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "name" {
  description = "Name prefix for the resources"
  type        = string
  default     = "ssm-demo"
}

variable "vpc_id" {
  description = "ID of the VPC, from the network module"
  type        = string
}

variable "private_subnet_id" {
  description = "ID of the private subnet, from the network module"
  type        = string
}