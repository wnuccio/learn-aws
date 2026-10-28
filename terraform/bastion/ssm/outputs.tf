output "instance_id" {
  description = "ID of the private instance, the target of a Session Manager session"
  value       = aws_instance.main.id
}

output "start_session_command" {
  description = "Opens a shell on the private instance through Session Manager (needs the session-manager-plugin)"
  value       = "aws ssm start-session --target ${aws_instance.main.id}"
}