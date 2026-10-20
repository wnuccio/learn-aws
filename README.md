# learn-aws

A learning kata for AWS Lambda with Java 17 and Terraform. A simple "Hello World" Lambda function, deployed and managed via Terraform.

## Prequisites
- Java 17 installed (use Homebrew)
- Maven installed (use Homebrew)
- AWS account with root user access
- AWS CLI installed (use aws script)
- Terraform installed (use Homebrew)
- Terraform plugin for IntelliJ
- Terragrunt installed (use Homebrew)
- Docker via Colima (use Homebrew: `brew install colima docker`); run `colima start` after each reboot
- Set the PATH in: source ~/.zprofile

## Test Lambda
- Go on AWS console and verify nothing exists: lambda function, API gateway, S3 bucket, log groups
- Run `mvn package` to create the fat jar in local
- Run terraform `init`, `plan` and `apply` to deploy the configuration
- Verify in AWS console that all the above resources are created
- Retrieve the `api_invoke_url` from terraform output or from AWS console (API Gateway -> Stages -> ... -> Invoke URL)
- Add the url to Postman, and test the function: `GET {url}/hello?name=John`
  - Alternative: `curl "$(terraform output -raw api_invoke_url)hello?name=Uno"` (to avoid updating url after destroy) 
- Verify the object is added in the S3 bucket and the logs are created
- Run terraform `destroy` to clean up the configuration

## Test Secrets Manager
- `mvn package`: creates the fat jar
- `terragrunt init`, `plan`, `apply` (in `terraform/lambda/`): deploys the lambda, the API and the secret container
- Verify in the console (Secrets Manager): the secret exists, with no value and no version
- `curl "$(terragrunt output -raw api_invoke_url)secret"`: returns 404 "secret has no value" (`ResourceNotFoundException`: no `AWSCURRENT` version)
- `aws secretsmanager put-secret-value --secret-id "$(terragrunt output -raw secret_name)" --secret-string "my-api-key-123"`: creates the first version
- `curl "$(terragrunt output -raw api_invoke_url)secret"`: returns the masked value
- `terragrunt destroy`: cleans up (the secret is deleted immediately, thanks to `recovery_window_in_days = 0`)

## Test MSK
- `curl -X POST -H "Content-Type: text/plain" -d "hello" "$(terraform output -raw api_invoke_url)/messages?key=john"`
- The response should be Sent. If the first call fails or times out, retry it before suspecting a bug, because the cold start is slow. To check how keys spread over the 2 partitions, send a few with different keys:
- `curl -X POST -H "Content-Type: text/plain" -d "hi" "$(terraform output -raw api_invoke_url)/messages?key=mary"`
- `curl -X POST -H "Content-Type: text/plain" -d "hey" "$(terraform output -raw api_invoke_url)/messages?key=anna"`

## Test Bastion
- `ssh-keygen -t ed25519 -f ~/.ssh/bastion-demo -C bastion-demo`: creates the key pair (just once)
  - `~/.ssh/bastion-demo`:  private key
  - `~/.ssh/bastion-demo.pub` public key, uploaded by Terraform
- `export TF_VAR_my_ip_cidr="$(curl -s https://checkip.amazonaws.com)/32"`: sets my public IP
- `terragrunt apply`: deploys the bastion and target instances, and outputs the `ssh_command`
- `ssh-add ~/.ssh/bastion-demo`: loads the private key into the SSH agent, so the jump process of `-J` can use it
- `ssh -J ec2-user@<bastion_public_ip> ec2-user@<target_private_ip>`: connects to target  (copy from the output)
- `terragrunt destroy`: cleans up

## Test Fargate
- `mvn package`: creates the fat jar
- run the server locally, without Docker:
  - `java -cp target/learn-aws.jar org.example.fargate.GreeterServer`: starts the server on port 8080
  - `curl "http://localhost:8080/hello?name=World"`: in another terminal; the response should be `Hello, World!`
  - `Ctrl+C`: stops the server
- run the server locally, in Docker (`colima start` first):
  - `docker build -t fargate-demo .`: builds the image from the `Dockerfile`, tagged `fargate-demo`
  - `docker run --rm -p 8080:8080 fargate-demo`: starts a container, mapping the Mac's port 8080 to the container's
  - `curl "http://localhost:8080/hello?name=World"`: in another terminal; the response should be `Hello, World!`
  - `Ctrl+C`: stops the container (`--rm` removes it)
- run the server on Fargate (in `terraform/fargate/`, after `docker build` above):
  - `export TF_VAR_my_ip_cidr="$(curl -s https://checkip.amazonaws.com)/32"`: sets my public IP
  - `terragrunt apply`: deploys the repository, network, cluster and service; the task fails until the image is pushed
  - `REPO=$(terragrunt output -raw repository_url)`: saves the ECR repository URL
  - `aws ecr get-login-password | docker login --username AWS --password-stdin "${REPO%%/*}"`: logs Docker into the registry
  - `docker tag fargate-demo "${REPO}:latest"`: the name is where `docker push` sends it: not Docker Hub (the default) but the ECR repository
  - `docker image inspect fargate-demo --format '{{.RepoTags}}'`: lists all the image's names, the local one and the ECR one
  - `docker push "${REPO}:latest"`: uploads the image; the service's next launch attempt picks it up
  - `aws ecs update-service --cluster fargate-demo --service fargate-demo --force-new-deployment`: optional, launches a task now instead of waiting for the next attempt
  - `TASK=$(aws ecs list-tasks --cluster fargate-demo --service-name fargate-demo --query 'taskArns[0]' --output text)`: finds the running task
  - `ENI=$(aws ecs describe-tasks --cluster fargate-demo --tasks "$TASK" --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text)`: finds its network interface
  - `IP=$(aws ec2 describe-network-interfaces --network-interface-ids "$ENI" --query 'NetworkInterfaces[0].Association.PublicIp' --output text)`: finds its public IP
  - `curl "http://$IP:8080/hello?name=World"`: the response should be `Hello, World!`
  - `aws logs tail /ecs/fargate-demo --follow`: shows the container's logs
  - `terragrunt destroy`: cleans up

## Documentation
- [Learning Plan](.docs/plan.md)
- [AWS Notes](.docs/aws.md)