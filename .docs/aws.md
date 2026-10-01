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

### Lambda Configuration via Terraform
- each Lambda has its own configuration, specified in the Terraform `aws_lambda_function` resource
- environment variables can be set via the `environment` block in Terraform
- example: `S3_BUCKET_NAME` and `AWS_REGION` are set when the Lambda is deployed (`terraform apply`)
- when the Lambda is invoked, AWS injects these environment variables into the Lambda's runtime
- the Lambda code retrieves them at runtime via `System.getenv("VARIABLE_NAME")`
- multiple Lambdas can have different environment variables (e.g., different buckets)

### Logging
- SLF4J (`slf4j-simple` in the pom) is used everywhere: it also works in classes with no access to the lambda `Context`
- `log.error("msg", e)` logs message and stack trace in one CloudWatch event; `e.printStackTrace()` writes a separate one
- `log.info("key={}", key)`: the placeholders skip the string concatenation when the level is off
- `slf4j-simple` defaults to level INFO: `debug` lines need a `simplelogger.properties` with `org.slf4j.simpleLogger.defaultLogLevel=debug`
- `context.getLogger()` is the platform-native alternative, but needs a `Context` and couples the code to lambda
- avoid catch-log-rethrow: let the exception propagate and log it once at the handler boundary
- logs from Lambda automatically appear in CloudWatch Logs under `/aws/lambda/function-name`

## API Gateway
- in terraform, to specify an api gateway there are up to 5 resources involved
- the `api` is the main resource, having its own id
- the `integration` is attached to the `api`, and specifies the exposed resource: the lambda function
- the `route` specifies the path to reach the `integration` (eg. GET /hello)
- IMPORTANT: the same `api` can have multiple `integration`s, each with its own `route`
- the `stage`, publishes the api and makes it reachable in an environment (dev, prod)
- the `permission` gives the api gateway the authorization to invoke the lambda function

## IAM Roles
- `assume_role_policy` is the trust policy
  - it is embedded in the `aws_iam_role` resource
  - it says who can assume the role (e.g. the `lambda.amazonaws.com` service)
  - it grants no permissions itself
- `aws_iam_role_policy_attachment` links the role to an existing policy
  - it attaches an AWS-managed policy to the role by ARN (e.g. `AWSLambdaBasicExecutionRole`)
- `aws_iam_role_policy` links the role to an inline policy
  - defines a new policy scoped to just that role, used for app-specific permissions (e.g. S3 write access)

- IMPORTANT: never delete **AWSServiceRoleForxxxS** roles, which are AWS-managed and automatically created

## Identity-based vs Resource-based Policies
- example: api gateway invokes the lambda, which writes to S3
  - gateway → lambda: `aws_lambda_permission`, a resource-based policy on the lambda
  - lambda → S3: `aws_iam_role_policy` on `lambda_role`, an identity-based policy
- `resource-based`: attached directly to the resource (lambda); says who is allowed to act on it, no role involved
- `identity-based`: attached to a `role` (or user/group); says what the role is allowed to do
- either one alone is enough to grant access (same account, no explicit deny elsewhere) - they're not both required
- when to use which:
  - `identity-based`: 
    - keeps all permissions ("what can this thing do") in one place (ex. write to S3, call another service, etc.); 
    - good when one caller touches many resources
  - `resource-based`: the caller has no identity of its own or is cross-account; 
    - keeps "who can touch this" in one place, 
    - good when one resource is touched by many callers
- AWS's own default/preference is identity-based policies for same-account access; resource-based is reserved for cases that need it (cross-account, callers without an identity)

## S3
- a `bucket` is a container that holds objects
- a `key` is the path/name of the object within the bucket (e.g., `filename.txt` or `folder/filename.txt`)
- an `object` is the actual data stored in the bucket; it can be built from a string, a file or raw bytes
- when writing to S3, you specify: the bucket name, the key, and the object (the actual data)
- example: bucket `learn-aws-helloworld`, key `John-2026-09-29T09:30:45.123Z`, object `"Hello, John!"` (the message string)
- Lambda must have IAM permissions (`s3:PutObject`) to write objects to a bucket

## Troubleshooting
**Strategy for debugging Lambda + API Gateway integration issues:**
- **Add logs at boundaries**: log before and after each major operation (S3 write, response building, etc.)
- **Use try-catch-finally**: wrap operations to catch and log any exceptions; use finally to log completion
- **Isolate components systematically**: test Lambda in isolation (console), then through API Gateway, to identify where the issue is
  - If Lambda console test works but API Gateway fails → issue is in integration or response format
  - If Lambda console test fails → issue is in Lambda code or S3 operations
  - If hardcoded response works but S3 code doesn't → issue is with S3 code, not response format
- **Check CloudWatch logs sources**: look for status even in absence of error messages (e.g. timeout)
- **Increase timeouts**: S3 operations and SDK initialization can be slow; increase Lambda timeout
- **Test incrementally**: remove S3 calls, then add back piece by piece to isolate the problematic code path

# Terraform
IMPORTANT: Terraform uses the same credentials as the CLI

## Resource model
- a resource is specified as `resource "resource_type" "resource_name"`; e.g.: resource aws_lambda_function helloworld
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
- `terraform state list`: lists the addresses of all the resources currently tracked in the state
- `terraform import <address> <aws_id>`: adopts an already existing AWS resource into the state
  - `<address>` is the terraform resource address (ex. `aws_cloudwatch_log_group.lambda_log_group`)
  - `<aws_id>` is the AWS resource id (see the 'Import' section on AWS)
  - it's a state-only operation: nothing is created or modified in AWS; `terraform state rm <address>` backs it out

IMPORTANT: any resource created manually before 'apply' will not be managed by Terraform; use 'import' for it



