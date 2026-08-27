# AWS 

## AWS console (browser)
- Go to login page
- Access as root user (username/password)
- Use Google Authenticator for MFA

## CLI
IMPORTANT: switching on personal hot-spot (on mobile) to avoid issue due to Netskope will work only temporarily,
normally the aws call on the CLI will fail;
NOTE: Terraform uses the same credentials as the CLI
- `~/.aws` folder contains AWS configuration and credentials files.
- The files inside, has one or more profiles (the [default] profile is used now).
  - `credentials` file contains Access key and Secret access key.
  - `config` file contains configuration settings (ex. the `region`).
- go on AWS console, IAM service, and create an access key + secret access key
- `aws configure` -> fill `credentials` and `config`
- `aws sts get-caller-identity` -> verify that configuration is correct

## Lambda package
- ensure the `pom.xml` has the correct AWS SDK dependencies and plugin configurations
- `mvn package` -> creates the fat JAR in the `target/` directory
- upload the JAR through the AWS console 
- specify what is the `handler method` 
- specify the Json input: it matches the first parameter of the handler
- test the function

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
- `terraform init`: the plugin for the provider declared in `main.tf` is downloaded
- `terraform apply`: the plugin translates the HCL declarations to AWS API calls; generates the tfstate
- `terraform plan`: compare the current state with the new configuration and detects `drift`
- `terraform destroy`: removes the resources tracked in the state, in the correct order, then emptying the state
IMPORTANT: any resource created manually before 'apply' will not be managed by Terraform, as not tracked in the state
    

