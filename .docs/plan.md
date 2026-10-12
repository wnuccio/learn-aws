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

## Key Concepts to Understand

- **Terraform** = Infrastructure as Code (IaC) — defines *where and how* your Lambda runs
- **Lambda** = Compute logic — the actual code that executes
- **Using Terraform to deploy Lambda** = Modern, reproducible, version-controlled approach

## Setup Notes

- Using personal hotspot for AWS access (corporate network has SSL blocking)
- AWS credentials stored locally, never committed to repo
- This is a personal learning project, separate from work