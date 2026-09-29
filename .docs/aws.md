# AWS 

## AWS console (browser)
- Go to login page
- Access as root user (username/password)
- Use Google Authenticator for MFA

## CLI
- Install the CLI: [link](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
- go on AWS console, IAM service, and create an access key + secret access key
- `aws configure` -> fill `credentials` and `config`
- `aws sts get-caller-identity` -> verify that configuration is correct
- `~/.aws` folder contains AWS configuration and credentials files.
- The files inside, has one or more profiles (the [default] profile is used now).
  - `credentials` file contains Access key and Secret access key.
  - `config` file contains configuration settings (ex. the `region`).

## Lambda
- ensure the `pom.xml` has the correct AWS SDK dependencies and plugin configurations
- `mvn package` -> creates the fat JAR in the `target/` directory
- upload the JAR through the AWS console 
- specify the `handler method` 
- specify the Json input: it matches the first parameter of the handler
- test the function

## API Gateway
- in terraform, to specify an api gateway there are up to 5 resources involved
- the `api` is the main resource, having its own id
- the `integration` is attached to the `api`, and specifies the exposed resource: the lambda function
- the `route` specifies the path to reach the `integration` (eg. GET /hello)
- IMPORTANT: the same `api` can have multiple `integration`s, each with its own `route`
- the `stage`, publishes the api and makes it reachable in an environment (dev, prod)
- the `permission` gives the api gateway the authorization to invoke the lambda function

## S3
- a `bucket` is a container that holds objects
- a `key` is the path/name of the object within the bucket (e.g., `filename.txt` or `folder/filename.txt`)
- an `object` is the actual data/file stored in the bucket
- when writing to S3, you specify: the bucket name, the key, and the object (the actual data)
- example: bucket `learn-aws-helloworld`, key `John-2026-09-29T09:30:45.123Z`, object `"Hello, John!"` (the message string)
- Lambda must have IAM permissions (`s3:PutObject`) to write objects to a bucket

# Terraform
IMPORTANT: Terraform uses the same credentials as the CLI

## Resource model
- `resource "aws_lambda_function" "helloworld"`: the `type` is `aws_lambda_function` and the name is `helloworld`
- a resource has `input attributes` specified directly, and `computed attributes` generated after the creation (ex. the ARN)
- the terraform's resource model doesn't always map 1:1: to the AWS model
- e.g.: `resource "aws_iam_role_policy_attachment" "lambda_basic_execution"` is not an AWS resource, it's just the role-policy relationship
- a `variable` has a type and optionally a `default` value, which can be overriden (in `terraform.tfvars` or the CLI)
- different `terraform.tfvars` can be used for different environments

## Operations
- move to /terraform directory
- `terraform init`: the plugin for the provider declared in `main.tf` is downloaded
- `terraform apply`: the plugin translates the HCL declarations to AWS API calls; generates the tfstate
- `terraform plan`: compare the current state with the new configuration and detects `drift`
- `terraform destroy`: removes the resources tracked in the state, in the correct order, then emptying the state

IMPORTANT: any resource created manually before 'apply' will not be managed by Terraform, as not tracked in the state
    

