variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "name" {
  description = "Name prefix for the resources"
  type        = string
  default     = "bastion-demo"
}

variable "public_key_path" {
  description = "Local SSH public key installed on both instances"
  type        = string
  default     = "~/.ssh/bastion-demo.pub"
}

variable "my_ip_cidr" {
  description = "My public IP as a /32 CIDR, the only source allowed to SSH into the bastion"
  type        = string
}