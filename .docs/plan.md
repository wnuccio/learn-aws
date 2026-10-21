# Learning Plan: AWS Lambda with Java & Terraform

## Repository Structure

```
learn-aws/
├── terraform/        # IaC for infrastructure
├── lambda/          # Java Lambda function code
└── .docs/           # Learning notes
```

## Quick Start Plan

### 1. Set Up Locally (1-2 hours)
- [x] Reactivate AWS account & set up CLI credentials
- [x] Create an IAM user with programmatic access
- [x] Install Terraform & AWS CLI locally
- [x] Configure AWS CLI with long-term credentials

**Status:** ✅ Complete — AWS CLI working via personal hotspot

---

### 2. Create Your First Simple Lambda (2-3 hours)
- [x] Write a basic Java 17 Maven function in `lambda/` (e.g., "Hello World" handler)
- [x] Deploy it manually via AWS Console to understand the flow
- [x] Test it via the AWS Console

**Status:** ✅ Complete — HelloWorld Lambda deployed and tested manually

---

### 3. Automate with Terraform (2-3 hours)
- [x] Create `terraform/` with a Lambda resource definition
- [x] Re-deploy the same function via Terraform
- [x] Learn how Terraform manages state

**Status:** ✅ Complete — Lambda & IAM role deployed via Terraform, state file understood

---

### 4. Add an HTTP Trigger via API Gateway
- [x] Add an API Gateway **HTTP API** in `terraform/lambda/main.tf`, with Lambda proxy integration
- [x] Add `aws_lambda_permission` so API Gateway is allowed to invoke the function
- [x] Adapt `HelloWorldHandler` to accept `APIGatewayV2HTTPEvent` / return `APIGatewayV2HTTPResponse` (add the `aws-lambda-java-events` dependency); read `name` from the query string and keep the body as plain text — no JSON (un)marshalling needed
- [x] `terraform apply`, then test the endpoint with `curl` (e.g. `curl "$API_URL/hello?name=World"`)
- [x] `terraform destroy` to close the loop

**Status:** ✅ Complete — API Gateway HTTP API wired to the Lambda, tested via curl

---

### 5. S3 Basics
- [x] Add an `aws_s3_bucket` in Terraform
- [x] Extend `HelloWorldHandler` to write a small object to the bucket (e.g. the `name` + a timestamp) after building its response
- [x] `terraform apply`, then `curl` the API Gateway endpoint and verify the object appears in the bucket (console or `aws s3 ls`)
- [x] `terraform destroy` to close the loop

**Status:** ✅ Complete — S3 bucket wired to the Lambda, object write verified

---

### 6. DynamoDB Basics
- [x] Add an `aws_dynamodb_table` in Terraform (e.g. partition key `id`)
- [x] Extend `HelloWorldHandler` to write an item to the table (e.g. `name` + timestamp), alongside or instead of the S3 write
- [x] Add IAM permissions (`dynamodb:PutItem`) to `lambda_role`
- [x] `terraform apply`, then `curl` the API Gateway endpoint and verify the item appears (console or `aws dynamodb scan`)
- [x] `terraform destroy` to close the loop

**Status:** ✅ Complete — DynamoDB table wired to the Lambda, item write verified

---

### 7. RDS + VPC Basics
- [x] New Terraform root module (`terraform/lambda2/`) with a VPC, subnets, security group, and an `aws_db_instance`
- [x] Move/extend a Lambda into that VPC to read/write a simple table via JDBC
- [x] `terraform apply`, test, **`terraform destroy` immediately after** (billed hourly regardless of use)

**Status:** ✅ Complete — Lambda in a private VPC reads/writes RDS PostgreSQL via JDBC, verified through API Gateway (module in `terraform/lambda2/`)

---

### 8. MSK Basics
- [x] New Terraform root module (`terraform/msk/`), likely MSK Serverless
  - [x] provider + region (`eu-west-3` supports MSK Serverless), private VPC with two subnets, security groups (Lambda ↔ MSK on 9098), `aws_msk_serverless_cluster` with IAM auth
- [x] A simple producer/consumer: producer Lambda behind API Gateway (`POST` writes to a topic), consumer Lambda triggered by MSK (event source mapping) that logs what it reads
  - [x] code in `org.example.msk` (`Producer`, `Message`, `ProducerHandler`, `RealMessageProducer`, `ConsumerHandler`, `CreateTopicHandler`, `TopicCreator`); 
  - [x] topic created at apply time by a dedicated Lambda (`AdminClient`, 2 partitions)
  - [x] Terraform: producer, consumer and topic-creator roles/functions (one shared Lambda log group), API Gateway, `aws_lambda_invocation` that creates the topic, event source mapping in on-demand mode with VPC interface endpoints for Lambda and STS, depending on the invocation and the endpoints
- [x] `terraform apply`, test, **`terraform destroy` immediately after** (billed hourly regardless of use)

**Status:** ✅ Complete — single `terraform apply` (2026-10-10, third run): mapping `Enabled`, `POST` → producer → topic → mapping → consumer verified in `/aws/lambda/msk-demo`; destroyed

**How it got there (three runs):**
- runs 1 and 2: the mapping in provisioned mode stuck in `Creating` until Terraform's 10-minute timeout, with no error
  - CloudTrail: it passed every check (cluster describe, bootstrap brokers, dry-run ENI creation), then never created its network interface; cause unknown
  - fixes kept along the way: topic created at apply time by its own Lambda, self-referencing 9098 rules on `msk-sg`, `slf4j-api` 2.x pinned
- run 3: on-demand mode plus VPC endpoints for Lambda and STS (`endpoints-sg`, 443 from `msk-sg`) worked at the first try
- the consumer Lambda works outside the VPC

**Notes (closed, no further verification planned):**
- both orphan mappings were deleted; none left in eu-west-3 (checked 2026-10-12)
- gateway log group renamed to `/aws/apigateway/msk-demo-producer-gateway`: takes effect at the next apply
- not verified: whether a function error in the topic creator fails the apply; the default `lifecycle_scope` of the invocation (believed `CREATE_ONLY`)
- lesson: a mapping in `Creating` can't be deleted, so a failed apply can block `terraform destroy`; recovery was `terraform state rm` on the mapping, then `destroy`

---

### 9. Terragrunt
- [x] Add a `terragrunt.hcl` per root module (`terraform/lambda/`, `terraform/lambda2/`, `terraform/msk/`)
- [x] Factor shared provider config into `terraform/root.hcl`, inherited via `include`
- [x] Replace `terraform apply`/`destroy` with `terragrunt apply`/`destroy` (and `run --all` across modules)
- [x] Compare: what Terragrunt buys over plain Terraform once there are 3 root modules (DRY config, remote state, orchestration)

**Status:** ✅ Complete — `run --all plan` across the three modules; apply/destroy via Terragrunt in `lambda/`

---

### 10. Bastion Host with EC2
- [x] New Terraform root module (`terraform/bastion/`), included in Terragrunt like the others
  - [x] VPC with a public subnet (internet gateway, route table) and a private subnet
  - [x] bastion EC2 in the public subnet, with a public IP; SSH allowed only from my IP
  - [x] small private EC2 in the private subnet, SSH allowed only from the bastion's security group
  - [x] `aws_key_pair` from a local public key (private key never committed)
- [ ] `apply`, then SSH to the private instance through the bastion (`ssh -J`)
  - apply blocked on 2026-10-13: the network parts are created, `RunInstances` fails with `Blocked: This account is currently blocked and not recognized as a valid account` (twice); dry runs succeed, billing is fine
  - waiting on an AWS Support case (Account and billing)
- [ ] Optional: reach the RDS of `lambda2/` through the bastion (SSH tunnel)
- [ ] Compare: SSM Session Manager as an alternative (no open port, no keys)
- [ ] **`destroy` immediately after** (EC2 and public IPv4 billed hourly)

**Status:** ⏸️ On hold — module written; EC2 launches blocked at account level, waiting for AWS Support

---

### 11. Containers with ECS on Fargate
- [x] Install Docker on the Mac: Colima (`brew install colima docker`, then `colima start`)
  - Apple Silicon builds ARM images: the task definition must use `ARM64` (or build with `--platform linux/amd64`)
- [x] Code in `org.example.fargate`, same pom and same jar: `Greeter` (TDD) and `GreeterServer` with the JDK's HTTP server
- [x] `Dockerfile` at the repo root: Java 17 base image + `target/learn-aws.jar`; built with a separate `docker build`, no Maven plugin
  - `.dockerignore` lets only the jar into the build context
  - verified locally: plain `java`, `docker run -p 8080:8080`, and IntelliJ (needs "Bind ports" in the Docker run configuration)
- [x] New Terraform root module (`terraform/fargate/`), included in Terragrunt like the others
  - [x] ECR repository
  - [x] VPC with a public subnet (no NAT gateway, no load balancer); security group allowing the app port only from my IP
  - [x] ECS cluster, task definition (smallest size: 0.25 vCPU, 0.5 GB), service with one task and a public IP
  - [x] task execution role only (ECR pull, logs): the code calls no AWS service, so no task role
  - [x] commands for push, task IP, curl and logs in the "Test Fargate" section of `README.md`
- [ ] `apply`, then `docker build` and `docker push` to ECR: the service retries until the image exists
  - 2026-10-15: `apply` and push worked; the service can't place the task: `your account is currently blocked` (same block as step 10); destroyed
  - added to the AWS Support case
  - in zsh, `"$REPO:latest"` is wrong (`:l` is a modifier): use `"${REPO}:latest"`
- [ ] Find the task's public IP (CLI or console), `curl` it; check the logs in CloudWatch
- [ ] **`destroy` immediately after** (billed per second while running)

**Status:** ⏸️ On hold — module validated and applied; the task can't start because of the account block, waiting for AWS Support (as step 10)

---

### 12. Secrets Manager
- Scenario: the secret is a third-party API key (a made-up value), given by the provider, so it can't be generated by Terraform
- [x] Extend the `terraform/lambda/` module (fast to deploy, no RDS needed)
  - [x] `aws_secretsmanager_secret`: the container only (name, description, KMS key), no value; with `recovery_window_in_days = 0`
  - [x] no `aws_secretsmanager_secret_version`: Terraform manages only the container and never knows the value
  - [x] secret name passed to the Lambda as an environment variable, and exposed as the `secret_name` output
  - [x] `secretsmanager:GetSecretValue` on the secret's ARN only, in the Lambda role
  - [x] a second route on the same API (`GET /secret`), dispatched by the existing handler (`switch` on the route key)
- [x] Code in `org.example.lambda`: `GetSecret` (TDD, with `FakeSecretRepository`) and `RealSecretRepository` using the SDK (`secretsmanager` module)
  - the response shows the key masked (first six characters, then `*`)
  - no value yet (`ResourceNotFoundException`): `Optional.empty()`, mapped to 404 "Secret has no value", not a generic 500
- [x] Test as in "Test Secrets Manager" of `README.md`: `curl` before the value is set (the mapped error), `put-secret-value` by hand (value in the command, fine for the kata), `curl` again (the masked value)
- [x] `destroy`

**Status:** ✅ Complete — 404 before the value, masked key after `put-secret-value`, verified via curl (2026-10-20)

---

### 13. Terragrunt Remote State
- Done in an isolated experiment module, so the existing modules and their workflow stay untouched
- Versions: Terragrunt 1.1.6, Terraform 1.16.4 (native S3 locking available)
- [ ] New module `terraform/remote-state/`, included in Terragrunt like the others; the existing modules keep their local state
  - [ ] a single `aws_ssm_parameter` (String, standard tier: free, created instantly, easy to change by hand)
  - [ ] a `remote_state` block in the module's own `terragrunt.hcl` (not in `root.hcl`)
    - backend `s3`, bucket named with the account ID (`get_aws_account_id()`), `encrypt = true`, `use_lockfile = true`
    - `key = "${path_relative_to_include()}/terraform.tfstate"`
    - `generate`: Terragrunt writes `backend.tf` in the module, like `provider.tf`
- [ ] Bootstrap: the first `init` with `--backend-bootstrap` creates the bucket (versioning, encryption, public access blocked)
- [ ] `apply`, then check the state at `remote-state/terraform.tfstate` in the bucket
- [ ] Lock: leave an `apply` waiting at the "yes" prompt; meanwhile the `.tflock` file is in the bucket, and a second `apply` from another terminal fails with "state locked"
- [ ] Drift: change the parameter's value in the console; `plan` refreshes and proposes to set it back to the code
- [ ] `destroy`: the state file stays in the bucket, empty; its previous versions are visible (versioning)
- [ ] Delete the bucket by hand from the console (empty it first, versions included)

**Status:** 🔜 Next

---

### 14. Terragrunt Environments
- [ ] `dev/` and `prod/` configurations of one module (e.g. `lambda/`), differing only in their inputs
  - module code in `terraform/modules/lambda/`; deployments in `terraform/live/dev/lambda/` and `terraform/live/prod/lambda/`, via `terraform { source = ... }`
  - env-wide inputs in an `env.hcl` per environment
- [ ] Resource names come from inputs, so the environments wouldn't collide (e.g. the S3 bucket name doesn't depend on any input today)
- [ ] State: local, or remote as in step 13 (to decide when we get there)
- [ ] `plan` both; apply at most one (no parallel deployments)

**Status:** 🔮 Future work

---

### 15. Terragrunt Dependency Blocks
- [ ] Split a module (e.g. network vs Lambda) and pass outputs with a `dependency` block
- [ ] See how `run --all` orders the modules

**Status:** 🔮 Future work

---

## Key Concepts to Understand

- **Terraform** = Infrastructure as Code (IaC) — defines *where and how* your Lambda runs
- **Lambda** = Compute logic — the actual code that executes
- **Using Terraform to deploy Lambda** = Modern, reproducible, version-controlled approach

## Setup Notes

- Using personal hotspot for AWS access (corporate network has SSL blocking)
- AWS credentials stored locally, never committed to repo
- This is a personal learning project, separate from work