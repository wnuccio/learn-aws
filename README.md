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

## First test of lambda function
- Go on AWS console and verify no lambda function exists
- Create the fat jar in local, (`mvn package`)
- Launch terraform commands to init, plan and apply the configuration
- Verify the lambda function is created in AWS console
- Verify the api gateway is created in AWS console
- Retrieve the `url` from terraform output (`terraform output -raw api_invoke_url`)
- Test the function: `GET {url}/hello?name=John`
- Destroy the configuration using terraform

## Documentation
- [Learning Plan](.docs/plan.md)
- [AWS Notes](.docs/aws.md)