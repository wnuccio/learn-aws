# AWS 

## Login
- Go to login page
- Access as root user (username/password)
- Use Google Authenticator for MFA

## Notes
- `~/.aws` folder contains AWS configuration and credentials files.
- The files inside, has one or more profiles (the [default] profile is used now).
- `credentials` file contains Access key and Secret access key.
- `config` file contains configuration settings (ex. the `region`).


- go on AWS console, IAM service, and create an access key + secret access key


- `aws configure` -> fill `credentials` and `config`
- IMPORTANT: switch on personal hot-spot (on mobile): there is a company obstacle for https calls
- `aws sts get-caller-identity` -> verify that configuration is correct


- ensure the `pom.xml` has the correct AWS SDK dependencies and plugin configurations
- `mvn package` -> creates the fat JAR in the `target/` directory
- upload the JAR through the AWS console 
- specify what is the `handler method` 
- specify the Json input: it matches the first parameter of the handler
- test the function


# Terraform
- `resource "aws_xlambda_function" "helloworld"`: the `type` is `aws_lambda_function` and the name is `helloworld`
- a resource has `input attributes` specified directly, and `computed attributes` generated after the creation (ex. the ARN)
- a `variable` has a type and optionally a `default` value, which can be overriden (in `terraform.tfvars` or the CLI)
- different `terraform.tfvars` can be used for different environments
- `terraform init`: the plugin for the provider declared in `main.tf` is downloaded 
- `terraform apply`: the plugin translates the HCL declarations to AWS API calls


