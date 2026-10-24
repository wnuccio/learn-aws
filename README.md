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
- `ssh-add -l`: lists the keys loaded in the agent; `bastion-demo` should be there
- `ssh -J ec2-user@<bastion_public_ip> ec2-user@<target_private_ip>`: connects to target  (copy from the output)
- `hostname -I`: on the target, shows its private IP (`10.0.2.x`)
- `ssh ec2-user@<target_private_ip>`: from the laptop, fails; the target is reachable only through the bastion
- `exit`: on the target, closes the session (and the jump through the bastion); back to the laptop shell
- `terragrunt destroy`: cleans up (copy the two IPs from the output first: they're gone after)
- `ssh-keygen -R <bastion_public_ip>` and `ssh-keygen -R <target_private_ip>`: 
  - remove the host keys from `~/.ssh/known_hosts` 
  - so a later apply with the same IPs doesn't fail with `REMOTE HOST IDENTIFICATION HAS CHANGED`

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
  - check the console, ECS > Clusters > `fargate-demo` > Services > `fargate-demo` > Tasks: 
    - a failed attempt about every 2 minutes, `CannotPullContainerError ... not found`
  - `REPO=$(terragrunt output -raw repository_url)`: saves the ECR repository URL
  - `aws ecr get-login-password | docker login --username AWS --password-stdin "${REPO%%/*}"`: logs Docker into the registry
  - `docker tag fargate-demo "${REPO}:latest"`: the name is where `docker push` sends it: not Docker Hub (the default) but the ECR repository
  - `docker image inspect fargate-demo --format '{{.RepoTags}}'`: lists all the image's names, the local one and the ECR one
  - `docker push "${REPO}:latest"`: uploads the image; the service's next launch attempt picks it up
  - `aws ecs update-service --cluster fargate-demo --service fargate-demo --force-new-deployment`: optional, launches a task now instead of waiting for the next attempt
    - check the console, same Tasks page: a task in `Running` (it may already be there, launched by the service's automatic retry after the push)
  - `TASK=$(aws ecs list-tasks --cluster fargate-demo --service-name fargate-demo --query 'taskArns[0]' --output text)`: finds the running task
  - `ENI=$(aws ecs describe-tasks --cluster fargate-demo --tasks "$TASK" --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text)`: finds its network interface
  - `IP=$(aws ec2 describe-network-interfaces --network-interface-ids "$ENI" --query 'NetworkInterfaces[0].Association.PublicIp' --output text)`: finds its public IP
  - `curl "http://$IP:8080/hello?name=World"`: the response should be `Hello, World!`
  - `aws logs tail /ecs/fargate-demo --follow`: shows the container's logs
  - `terragrunt destroy`: cleans up
  - `docker rmi fargate-demo "${REPO}:latest"`: removes the local image (both names); the base image `eclipse-temurin:17-jre` stays, for the next build
    - or `docker system prune -a --volumes`: removes all unused images, containers, networks and volumes

## Test Remote State
- in `terraform/live/dev/remote-state/`:
- `BUCKET="learn-aws-tfstate-$(aws sts get-caller-identity --query Account --output text)"`: saves the state bucket name
- `terragrunt init --backend-bootstrap`: creates the bucket (first time only), generates `backend.tf`, configures the backend
- `terragrunt apply`: creates `/learn-aws/dev/demo`; the state is written to the bucket
- `aws s3 ls "s3://${BUCKET}/dev/remote-state/"`: shows `terraform.tfstate`
- lock:
  - change `parameter_value` to `"dev-value-2"` in `live/dev/env.hcl`, then `terragrunt apply`: leave it waiting at the "yes" prompt
  - `aws s3 ls "s3://${BUCKET}/dev/remote-state/"`: in another terminal; shows `terraform.tfstate.tflock` too
  - `terragrunt plan`: in the other terminal; fails with `Error acquiring the state lock`
  - answer `yes` in the first terminal: the lock file disappears
- drift:
  - `aws ssm put-parameter --name /learn-aws/dev/demo --value dev-value-3 --overwrite`: changes the value by hand (or in the console)
  - `terragrunt plan`: the refresh reads `dev-value-3`; the plan proposes `insecure_value: "dev-value-3" -> "dev-value-2"` (visible, not `sensitive`)
  - `terragrunt apply`: aligns the cloud to the code
- `terragrunt destroy`: deletes the parameter; the state file stays in the bucket, empty
- `aws s3api list-object-versions --bucket "$BUCKET" --prefix dev/remote-state/ --query 'Versions[].[LastModified,Size]'`: lists the state's previous versions
- set `parameter_value` back to `"dev-value-1"` in `live/dev/env.hcl`
- delete the bucket by hand in the console (S3 → bucket → `Empty`, then `Delete`): it's not in any state

## Test Environments
- `BUCKET="learn-aws-tfstate-$(aws sts get-caller-identity --query Account --output text)"`: saves the state bucket name
- `cd terraform/live/dev/remote-state && terragrunt init --backend-bootstrap && terragrunt apply`: creates the bucket (first time only) and `/learn-aws/dev/demo` = `dev-value-1`
- `cd ../../prod/remote-state && terragrunt init && terragrunt apply`: creates `/learn-aws/prod/demo` = `prod-value-1`; same bucket, already bootstrapped
- `aws ssm get-parameters-by-path --path /learn-aws --recursive --query 'Parameters[].[Name,Value]' --output text`: shows both parameters, with their values
- `aws s3 ls "s3://${BUCKET}/" --recursive`: shows two state files, `dev/remote-state/` and `prod/remote-state/`
- `terragrunt destroy` in both folders
- delete the bucket by hand in the console (S3 → bucket → `Empty`, then `Delete`): it's not in any state

## Documentation
- [Learning Plan](.docs/plan.md)
- [AWS Notes](.docs/aws.md)