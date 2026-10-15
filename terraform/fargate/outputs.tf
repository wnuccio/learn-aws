output "repository_url" {
  description = "ECR repository URL, the target of docker tag and docker push"
  value       = aws_ecr_repository.app.repository_url
}

# The task's public IP isn't known here: ECS assigns it when it launches the task, after the apply
output "cluster_name" {
  description = "ECS cluster name, used to look up the running task"
  value       = aws_ecs_cluster.main.name
}

output "service_name" {
  description = "ECS service name, used to look up the running task"
  value       = aws_ecs_service.app.name
}