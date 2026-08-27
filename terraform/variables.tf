variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-west-3"
}

variable "function_name" {
  description = "Lambda function name"
  type        = string
  default     = "HelloWorld"
}

variable "jar_path" {
  description = "Path to the JAR file"
  type        = string
  default     = "../target/learn-aws.jar"
}

variable "handler" {
  description = "Lambda handler"
  type        = string
  default     = "org.example.lambda.HelloWorldHandler"
}

variable "runtime" {
  description = "Lambda runtime"
  type        = string
  default     = "java17"
}