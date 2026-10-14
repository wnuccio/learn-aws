# learn-aws

A learning kata for AWS Lambda with Java 17 and Terraform. A simple "Hello World" Lambda function, deployed and managed via Terraform.

## Prequisites
- Java 17 installed (use Homebrew)
- Maven installed (use Homebrew)
- AWS account with root user access
- AWS CLI installed (use aws script)
- Terraform installed (use Homebrew)
- Terraform plugin for IntelliJ
- Set the PATH in: source ~/.zprofile

## First test (lambda function)
- Go on AWS console and verify nothing exists: lambda function, API gateway, S3 bucket, log groups
- Run `mvn package` to create the fat jar in local
- Run terraform `init`, `plan` and `apply` to deploy the configuration
- Verify in AWS console that all the above resources are created
- Retrieve the `api_invoke_url` from terraform output or from AWS console (API Gateway -> Stages -> ... -> Invoke URL)
- Add the url to Postman, and test the function: `GET {url}/hello?name=John`
  - Alternative: `curl "$(terraform output -raw api_invoke_url)hello?name=Uno"` (to avoid updating url after destroy) 
- Verify the object is added in the S3 bucket and the logs are created
- Run terraform `destroy` to clean up the configuration

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

## Documentation
- [Learning Plan](.docs/plan.md)
- [AWS Notes](.docs/aws.md)