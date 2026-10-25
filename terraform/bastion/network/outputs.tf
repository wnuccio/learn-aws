output "vpc_id" {
  description = "ID of the VPC, for the security groups"
  value       = aws_vpc.main.id
}

output "public_subnet_id" {
  description = "ID of the public subnet, for the bastion"
  value       = aws_subnet.public.id
}

output "private_subnet_id" {
  description = "ID of the private subnet, for the target"
  value       = aws_subnet.private.id
}