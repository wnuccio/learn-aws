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

## Secrets Manager
- `secret`: has a name (public) and a value (the private part)
  - the secret is a container: name, description, KMS key (plus tags, resource policy, rotation)
  - the value lives in a `version`: each `put-secret-value` adds a new one
  - the latest version is labeled `AWSCURRENT`, the one returned by `GetSecretValue`
  - rotation: a new `put-secret-value` moves `AWSCURRENT` to the new version, the old one becomes `AWSPREVIOUS`
    - clients asking for `AWSCURRENT` get the new value with no change, no deploy
    - `get-secret-value --version-stage AWSPREVIOUS`: reads the old value (e.g. to roll back)
    - `list-secret-version-ids --secret-id <name>`: lists the versions with their labels
    - a value cached in a warm Lambda stays old until a cold start
  - a secret can have no version: `CreateSecret` without a value (API, CLI, Terraform) creates only the container
  - a secret with no version: `GetSecretValue` fails with `ResourceNotFoundException`
- `Secrets Manager`:
  - `storing the secret`
    - the value is encrypted at rest with KMS
    - the default encryption key is `aws/secretsmanager`
    - the value is decrypted on `GetSecretValue`
  - `retrieving the secret`
    - no owner concept: creating a secret grants no right to read it
    - `secretsmanager:GetSecretValue`: a user or role with this policy can read the secret
- `administrator`: the account admin has all the rights,
  - he can grant the read permission to a user or role, via a specific IAM policy
  - by default only identities of the same account can read it;
  - cross-account needs a resource policy and a customer-managed key
  - attaching a role to a Lambda requires `iam:PassRole` on that role
- in Terraform: `aws_secretsmanager_secret` is the container; after `apply` the secret has no version;
  - the value can be set by hand (console, CLI), to keep it out of the code and the state
  - `aws_secretsmanager_secret_version` is a version: not declared when the value is set by hand
  - `recovery_window_in_days = 0` deletes the secret at `destroy` (otherwise it's only scheduled for deletion in 7-30 days)


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
- `ENI` (Elastic Network Interface): a virtual network card, attached to a host (EC2, Lambda, Fargate task)
  - `subnet`: exactly one (so one AZ)
  - `private IP`: from the subnet range
  - `public IP`: optional, mapped to the private one by the IGW
  - `security groups`: the firewall sits on the card
  - `MAC address`: like any network card
  - `host`: a resource can have more than one ENI (e.g. one per subnet)
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
    - members of the same security group can't talk to each other by default: 
      - a self-referencing rule (the group itself as source/destination) is needed
      - for both `egress` (on the sender) and `ingress` (on the receiver)
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

### Public subnet with a web server
- 1: an `Internet Gateway` (IGW) attached to the VPC
- 2: a `subnet`: an IP range within the VPC block
- 3: a `route table` associated with the subnet, with `0.0.0.0/0` → IGW
  - it directs traffic **leaving** the subnet, by destination: allows the replies to the clients
- 4: an `EC2 instance` in the subnet: AWS creates its primary ENI, with a private IP from the subnet range
- 5: a `public IP` on the instance (on its ENI): the IGW maps it 1:1 to the private IP
- 6: an `ingress rule` on the instance's `security group`: TCP 80 from `0.0.0.0/0`
  - security groups are stateful: replies to allowed requests go out with no egress rule
- and a web server listening on port 80

sequence of events:
- request:
  - 1: a client on the internet sends a request to the instance's `public IP`
  - 2: the `IGW` receives it and translates the destination: public IP → `private IP` of the instance's ENI
  - 3: `the security group` (firewall) checks its ingress rules: TCP 80 is allowed, so the request reaches the instance
    - the route table has no role here
- reply:
  - 4: the instance sends the reply from its `private IP` to the client's address
  - 5: the `security group` (it's stateful firewall) lets the reply out
  - 6: the subnet's `route table` sends it to the IGW (`0.0.0.0/0`)
  - 7: the `IGW` translates the source: private IP → `public IP`, then forwards it to the client

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
  - here provisioned mode stuck in `Creating` with no error; on-demand mode with interface endpoints for Lambda and STS worked
  - the mapping calls the Lambda (invoke) and STS (role credentials) APIs over 443 from its ENI: endpoint security group allows 443 from the cluster's group
- the consumer function itself can stay outside the VPC
- `kafka-clients` brings `slf4j-api` 1.7.x: pin 2.x or `slf4j-simple` 2.x finds no binder and every log line is dropped
- a mapping stuck in `Creating` can't be deleted: `terraform destroy` fails until it leaves that state
  - workaround: `terraform state rm` the mapping, then `destroy` the rest

## SSH
- SSH (Secure Shell): a protocol to open a terminal on a remote machine over an encrypted connection
- server: the remote machine runs `sshd`, listening on port 22; 
- client: the `ssh` command
- philosophy
  - the `public key` is a lock; 
    - it can be shared with anyone and put on the server;
  - the `private key` is the only key that opens the lock; 
    - it never travels over the network; 
    - it stays on the client
  - basic communication:
    - the server sends a `challenge` to the client
    - the client `signs the challenge` with its private key
    - the server `checks the signature` with the public key
  - only the private key can produce a valid signature, but anyone with the public key can check it
- setup: 
  - the client generates a **key pair** locally (public + private)
  - the client's **public key** (the lock) is put on the server, in `~/.ssh/authorized_keys`; 
  - the **private key** (the key) stays on the client
- communication:
  - 1: the server proves its identity with its host key; the client saves it in `~/.ssh/known_hosts` at the first connection
  - 2: the client sends its public key (the lock) to log-in
  - 3: the server checks that the lock is in `authorized_keys`, otherwise refuses
  - 4: the server sends a challenge; the client signs it with its private key
  - 5: the server verifies the signature with the public key
  - 6: the encrypted connection is open
- Example:
  - `ssh-keygen -t ed25519`: creates the key pair (`~/.ssh/id_ed25519` private, `~/.ssh/id_ed25519.pub` public)
  - `ssh -i <private_key> <user>@<host>`: opens a shell on the remote machine
  - in a bastion host there are two machines: the bastion (public) and the target machine (private)
  - `ssh -J <user>@<bastion> <user>@<target_host>`: jumps through the bastion to the target machine

## Bastion host
- bastion host: a machine in a public subnet from which you jump to the machines in the private subnets of the same VPC
- why: private machines have no public IP and no internet route; the bastion is a single, controlled entry point
- bastion: EC2 in a public subnet (public IP, internet gateway route); its security group allows 22 only from my IP
- target: EC2 in a private subnet; its security group allows 22 only from the bastion's security group

```
internet ──22──> [bastion EC2]  public subnet (public IP, internet gateway route)
                      │ 22
                      ▼
                 [private EC2]  private subnet (no public IP, no internet route)
```

- alternative: SSM Session Manager gives a shell through the AWS API, with IAM access control

## ECS (Elastic Container Service) and Fargate
- `image`: a frozen file system (OS files - kernel excluded, JDK, the jar) plus a start command
- `container`: a running instance of an image
- `mvn package` builds the jar, 
- `docker build` creates the image from the jar
- entry point: a class with `main` that starts an HTTP server
- `ECR`: AWS's image registry; one `repository` per image; 
- `docker push` uploads the image to the registry
- `ECS`: the orchestrator that pulls the image from ECR and runs the containers (task definition, task, service, cluster)
  - the `launch type` says where the containers physically run:
  - `EC2 launch type`: on your own EC2 instances, which you manage, patch and scale
  - `Fargate launch type`: on AWS-managed machines
- `task definition`: a template with a list of container definitions (image, port, env vars), plus CPU, memory, IAM roles, logging
- `task`: a running instance of a task definition
  - it has one or more containers sharing one IP and one lifecycle
  - analogy: task definition = docker compose file, task = `docker compose up`
- `service`: a supervisor that 
  - keeps N tasks running, replacing crashed ones
  - rolls out the new `task definition` revisions
  - IMPORTANT: a batch task (one-off) has no service, as it starts and stops; a service is for long-running tasks
- `cluster`: on Fargate, just a name grouping services
  - it usually has many services, each representing a component of the application (e.g. frontend, backend)
- chain in Terraform: cluster → service → task definition → container definition → ECR image
- a task has its own IP, so no API Gateway is needed; the IP changes at every restart
- two IAM roles, both trusting `ecs-tasks.amazonaws.com`:
  - `task execution role`: used by ECS to pull the image from ECR and write logs (`AmazonECSTaskExecutionRolePolicy`)
  - `task role`: used by the code at runtime for SDK calls (e.g. S3); the equivalent of the Lambda role
  - missing execution role → the task never starts; missing task role → `AccessDenied` from the code
- each task gets its own ENI in a subnet
- starting a task, step by step:
  - 1: the `ECS` (control plane) - outside the VPC - decides to start the task and picks a Fargate host
  - 2: the `ECS agent` on that host - inside the VPC - assumes the execution role: the permission to pull
  - 3: the agent calls `ECR` - outside the VPC - through the task's ENI → route table → IGW
    - so the task needs outbound access: public subnet + public IP
  - 4: the image is downloaded and the container starts
  - the code in the container never sees the execution role's credentials, only the task role's

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

## State
- three entities: 
  - the `code` (`main.tf`, desired configuration)
  - the `state` (`terraform.tfstate`)
  - the `cloud` (real resources)
- the state 
  - holds a `link` per resource: it tracks the resource using its ID
  - a `drift` is a misalignment between the link and the real resource: the latter can be different or removed
  - Terraform never scans the account: it only sees the resources linked in the state

## Operations
- move to /terraform directory
- `terraform init`: downloads the provider plugin declared in `main.tf` and configures the backend; creates no state
- `terraform plan`: two phases
  - 1: `refresh` (state ↔ cloud): reads each linked resource from AWS and updates its attributes in the state; drops the link if the resource is absent
  - 2: `compare` (code ↔ state (refreshed)): each difference between code and state becomes an action
    - the state is only a view of reality; **the desired side is the code**
    - a resource in the state but no longer in the code is destroyed
  - actions: each resource is planned to be created, updated on the cloud, replaced (destroy + create), destroyed
  - e.g.: a linked resource deleted by hand from the console - refresh drops it, plan (due to compare) creates it again
- `terraform apply`: the plugin translates the HCL declarations to AWS API calls; 
  - the first apply creates the state; the following ones update it
- `terraform destroy`: removes the linked resources, in the correct order, then empties the state
- `terraform state list`: lists the addresses of all the resources currently linked in the state
- `terraform import <address> <aws_id>`
  - creates a link to a resource that exists in AWS and is declared in the code, but is not tracked in the state (e.g. created by hand)
  - without it, plan says "create" and apply fails with "already exists"
  - `<address>` is the terraform resource address (ex. `aws_cloudwatch_log_group.lambda_log_group`)
  - `<aws_id>` is the AWS resource id (see the 'Import' section on AWS)
  - it's a state-only operation: nothing is created or modified in AWS
- `terraform state rm <address>` 
  - removes a link; the resource stays in AWS, but it's no longer tracked by Terraform
  - when: stop managing a resource without deleting it, or one that can't be deleted (e.g. an MSK mapping stuck in `Creating`)
  - remove the block from the code too, otherwise plan creates it again

## Remote state
- with a team, the state must be a single shared copy: a local file per person is not feasible
- remote state: the state file lives in an S3 bucket, one key per module, readable by the team and by the CI pipeline
- `lock`: a `<key>.tflock` file next to the state, created at the start of a plan/apply and deleted at the end
  - it only says "someone is working on this state": a concurrent run fails with "state locked"
  - native in S3 since Terraform 1.10 (`use_lockfile = true`); older setups used a DynamoDB table
- bucket `versioning`: keeps every previous version of the state (history, rollback)
- the bucket is not part of any state: here it's created by Terragrunt's bootstrap, and deleted by hand from the console
  - it outlives `destroy`, which only empties the state files




# Terragrunt

## Overview
- a thin wrapper around Terraform: it helps to remove duplications between Terraform modules
- it defines common blocks in in a `root.hcl` in a parent folder 
- each module contains a `terragrunt.hcl` with an `include` block to inherit the `root.hcl`
- commands: `terragrunt plan`/`apply`/`destroy` in a module; `terragrunt run --all <cmd>` across all modules
- .terragrunt-cache/`: a copy of the terraform module, generated by Terragrunt; commands are executed here
  - the cache also holds the files Terragrunt generates from its config (e.g. `provider.tf` from a `generate` block)
  - the cache also holds `terraform.tfstate`: deleting it with live resources loses track of them

## Sharing a value between modules
- declare the same `variable` in each module, without a `default`
- declare an input with the same name in `root.hcl` (`inputs` block): Terragrunt injects it into every module
- input `aws_region` → env variable `TF_VAR_aws_region` → Terraform `var.aws_region` (names must match)
