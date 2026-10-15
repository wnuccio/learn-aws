variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "name" {
  description = "Name prefix for the resources"
  type        = string
  default     = "fargate-demo"
}

variable "my_ip_cidr" {
  description = "My public IP as a /32 CIDR, the only source allowed to reach the app port"
  type        = string
}