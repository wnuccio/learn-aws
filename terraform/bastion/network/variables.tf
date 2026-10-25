variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "name" {
  description = "Name prefix for the resources"
  type        = string
  default     = "bastion-demo"
}