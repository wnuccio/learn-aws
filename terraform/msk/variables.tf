variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "jar_path" {
  description = "Path of the Lambda jar"
  type        = string
}

variable "topic" {
  description = "Kafka topic the producer writes to (created by the producer Lambda on start)"
  type        = string
  default     = "messages"
}

variable "name" {
  description = "Name prefix for the resources"
  type        = string
  default     = "msk-demo"
}