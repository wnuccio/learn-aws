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
- a `route` matches only the http method + path (e.g. `POST /hello`); the gateway doesn't validate anything else
- query parameters, headers and body are passed through to the lambda untouched, so unexpected ones are silently accepted
- a single `lambda` can serve several routes (e.g. GET /hello and POST /hello):
  - one 'integration'
  - one `route` resource per rout, pointing to the same `integration`
  - the single lambda `handler` acts as a dispatcher on the http method (or on `getRouteKey()`)

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

## DynamoDB
- `partition key`: identifies which physical partition an item lives on (Dynamo hashes it); 
  - with no sort key, it's the full primary key and must be unique per item
- `sort key`: orders/filters items that share the same partition key; 
  - the full primary key is `partition key + sort key` together, and only that combination must be unique
- querying by partition key alone returns the whole **item collection**: all items in that partition (an "aggregate")
- querying by partition key + sort key returns one specific item from the collection
- a range condition (`>`, `between`, `begins_with`) returns a subset of the collection

## VPC
- a VPC is a private network (a CIDR block, e.g. `10.0.0.0/16`); 
- `CIDR`: `base-address/prefix-length` (e.g. `10.0.0.0/16`); 
  - the prefix fixes the left bits (network part), the rest vary (host addresses); 
  - e.g.: `10.0.0.0/24` → from `10.0.0.0` to `10.0.0.255`
- VPC networking applies to resources with a network interface in a subnet (EC2, RDS, Lambda only if explicitly VPC-attached)
- an ENI (network interface) is the address assigned to a resource within a subnet, plus the firewall (security groups) applied to traffic to and from that address
  - a resource can have more than one ENI (e.g. one per subnet)
- many AWS services (S3, Lambda by default, DynamoDB) have their own public API endpoint and live outside any VPC
- a `subnet` lives inside a VPC
  - a public subnet
    - has route table which sends `0.0.0.0/0` to an Internet Gateway, 
    - a resource within it still needs its own public IP to actually be reachable from outside
  - a private subnet 
    - has route table which no route to the Internet Gateway
    - can still get outbound internet access via a NAT Gateway; inbound from the internet stays blocked
- security group: 
  - a set of inbound/outbound rules (protocol + port + source/destination) attached to a resource's network interface; 
  - source/destination can be a CIDR range or another security group (by ID)
  - govern raw network traffic (e.g. a JDBC connection) through packet filtering
  - different from IAM, which is identity-based and governs calls to AWS APIs (`s3:PutObject`, `lambda:InvokeFunction`); 

### Subnets and availability zones
```
Region eu-west-3
├── AZ eu-west-3a
│   ├── subnet 10.0.0.0/25    (private)
│   └── subnet 10.0.1.0/24    (public)     ← allowed: several subnets in one AZ
└── AZ eu-west-3b
    └── subnet 10.0.0.128/25  (private)
```
- a VPC spans the whole region;
- an AZ is a physically separate data center in a region
- **a subnet belongs to exactly one AZ**; an AZ can contain many subnets (many-to-one)
- subnets of the same VPC can't overlap in address range
- MSK and RDS need subnets in at least two AZs; subnets cost nothing

### Architecture
```
         (public internet)
                │
                ▼
        API Gateway  (outside VPC)
                │
       lambda:InvokeFunction
     (control plane, not network)
                ▼
┌───────────────────── VPC ──────────────────────┐
│                                                  │
│   ┌──────────┐   JDBC/TCP    ┌──────────┐       │
│   │  Lambda  │ ─────────────▶│   RDS    │       │
│   │ (private │ (sec. group   │ (private │       │
│   │  subnet) │  allows it)   │  subnet) │       │
│   └────┬─────┘                └──────────┘      │
│        │                                        │
└────────┼────────────────────────────────────────┘
         │ outbound only (NAT Gateway / VPC Endpoint)
         ▼
 S3 / DynamoDB  (outside VPC, public endpoints)
```
- API Gateway, S3 and DynamoDB are outside the VPC; Lambda and RDS are inside, each in a private subnet
- gateway → lambda: control-plane invocation, no VPC networking involved
- lambda → RDS: real network traffic (JDBC/TCP), gated by security groups
- lambda → S3 / DynamoDB: also outside the VPC, so each needs a NAT Gateway or VPC Endpoint once the Lambda is VPC-attached

## MSK (Managed Streaming for Kafka)
- AWS manages the Kafka cluster (brokers, Zookeeper, etc.) 
- a `topic` is split in partitions
- the producer picks the partition via the message `key` (hashed): same key → same partition;
- each `partition` is an append-only log; every message gets an increasing `offset`; 
- consumers read in offset order; order of messages is then guaranteed within a partition
- to keep related events in order (e.g. all events of a `userId`), use that id as the key
- a `consumer group` is a set of consumers sharing a `group.id`, reading a topic together
- **each partition is assigned to exactly one consumer** in the group (but a consumer can hold many partitions)
- a message is processed by one consumer per group; another group with a different `group.id` gets its own full copy of the stream
- within a group, consumers are `competing consumers` but they compete for partitions, not for single messages
  - in fact, within each partition there is only one consumer, to ensure correct order in processing
- parallelism limit: with 3 partitions, at most 3 consumers in a group work; a fourth stays idle
- `rebalancing`: when a consumer joins, leaves or crashes, the group coordinator (a broker) reassigns the partitions
- each group tracks its committed offset per partition (stored in Kafka itself); after a restart it resumes from there

### Lambda as the consumer
- with a Lambda event source mapping, Lambda is the consumer group:
  - the `event source mapping` resource listens to one `topic`
  - it polls the topic (via the so-called event pollers) and invokes the function with a batch of records
  - the `batch` is a Map<"topic-partition", List<Record>>, so it may contain any partition from the topic
  - in each `partition`, the `records` are in offset order
  - `offsets` are committed after the function succeeds
  - on exception the batch is retried → at-least-once, so the consumer must tolerate duplicates

```
                         Topic "orders" (3 partitions)
        ┌──────────────────────────────────────────────────────────┐
        │ P0: [0][1][2][3][4] ...   keys hashed to P0 (e.g. john)  │
        │ P1: [0][1][2] ...         keys hashed to P1 (e.g. mary)  │
        │ P2: [0][1][2][3] ...      keys hashed to P2 (e.g. anna)  │
        └──────────────────────────────────────────────────────────┘
              ▲                ▲                ▲
              │ append         │ append         │ append
          (key=john)       (key=mary)       (key=anna)
                         Producer(s)


   Consumer group A (group.id = "billing")      Consumer group B (group.id = "audit")
   each partition → exactly one consumer        its own independent copy of the stream

        P0 ─────▶ Consumer A1                        P0 ┐
        P1 ─────▶ Consumer A2                        P1 ├─▶ Consumer B1
        P2 ─────▶ Consumer A2                        P2 ┘
```
- a key always hashes to the same partition, so `john`'s events stay in order in P0
- group A: 3 partitions shared between 2 consumers (A1 holds P0, A2 holds P1 and P2); a partition is never split between two consumers of the same group, which preserves its ordering
- group B: different `group.id`, so it receives all the messages again (here a single consumer holds all three partitions)
- rebalance: if A2 crashes, P1 and P2 move to A1; if a third consumer joins group A, one partition moves to it; a fourth would stay idle (only 3 partitions)
- each group keeps its own committed offset per partition (A can be at offset 3 on P0 while B is at offset 1)
- the MSK-triggered Lambda plays the role of group A: Lambda manages the group and the assignment, the function only receives batches

### MSK with Lambdas
- There are three Lambdas: 
  - the `topic creator` runs once at `apply`, thanks to `aws_lambda_invocation` resource
  - the `producer` writes to it on each `POST`
  - the `consumer` is triggered by the `mapping` on the topic and writes on the `logs` 
- MSK Serverless has no topic auto-creation
- a topic is data inside the cluster, is not an AWS resource: Terraform can't create it
  - workaround: a Lambda in the VPC creates it with `AdminClient`, invoked by `aws_lambda_invocation` during `apply`
- the event source mapping polls through an ENI that uses the cluster's subnets and security group, not the Lambda's
  - the cluster security group needs self-referencing rules on 9098 (inbound and outbound)
- the mapping polls with the function's execution role, so no `aws_lambda_permission` is needed
  - IAM auth needs `kafka-cluster:Connect`, `DescribeTopic`, `ReadData`, `DescribeGroup`, `AlterGroup` on the role, plus `AWSLambdaMSKExecutionRole`
- provisioned mode (`provisioned_poller_config`) needs no NAT or VPC endpoints; on-demand mode does (Lambda, STS)
- `kafka-clients` brings `slf4j-api` 1.7.x: pin 2.x or `slf4j-simple` 2.x finds no binder and every log line is dropped
- a mapping stuck in `Creating` can't be deleted: `terraform destroy` fails until it leaves that state
  - workaround: `terraform state rm` the mapping, then `destroy` the rest

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



