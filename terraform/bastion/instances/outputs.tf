output "bastion_public_ip" {
  description = "Public IP of the bastion"
  value       = aws_instance.bastion.public_ip
}

output "target_private_ip" {
  description = "Private IP of the target instance"
  value       = aws_instance.target.private_ip
}

output "ssh_command" {
  description = "SSH into the target, jumping through the bastion (run ssh-add ~/.ssh/bastion-demo first: -J doesn't pass -i to the jump)"
  value       = "ssh -J ec2-user@${aws_instance.bastion.public_ip} ec2-user@${aws_instance.target.private_ip}"
}

output "known_hosts_cleanup_command" {
  description = "Removes both host keys from ~/.ssh/known_hosts after destroy (copy it before: the outputs are gone after)"
  value       = "ssh-keygen -R ${aws_instance.bastion.public_ip} && ssh-keygen -R ${aws_instance.target.private_ip}"
}