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
  - [x] code in `org.example.msk` (`Producer`, `Message`, `ProducerHandler`, `RealMessageProducer`, `ConsumerHandler`); topic created by the producer Lambda (`AdminClient`, 2 partitions)
  - [x] Terraform: producer and consumer roles/functions/log groups, API Gateway, event source mapping in provisioned mode (no NAT/endpoints)
- [ ] `terraform apply`, test, **`terraform destroy` immediately after** (billed hourly regardless of use)

**Status:** 🚧 In progress — everything is written; first apply done, the producer returned `Sent` but the mapping stuck in `Creating`, so the consumer path is still unverified; all destroyed

**Pending / open points:**
- re-run apply and test with the two fixes below (`mvn clean package` first); check the mapping reaches `Enabled` and the consumer log shows `received messages-N ...`
  - fix 1: self-referencing 9098 ingress/egress rules on `msk-sg` (the mapping's ENI uses the cluster's security group, not the Lambda's)
  - fix 2: `slf4j-api` 2.0.7 pinned in `pom.xml` (`kafka-clients` brought 1.7.x, which silently dropped all logs)
- the mapping may be created before the topic exists (the producer creates it on its first call): if it stays in `Creating`, find a way to create the topic first
- orphan mapping `03126fa0-0762-40ab-835b-bf09f4a52acf` (eu-west-3) was stuck in `Creating`; it moved to `Disabled` after a day and was deleted on 2026-10-10; check Billing for stray costs
- lesson: a mapping in `Creating` can't be deleted, so a failed apply can block `terraform destroy`; recovery was `terraform state rm` on the mapping, then `destroy`

---

### 9. Terragrunt
- [ ] Add a `terragrunt.hcl` per root module (`terraform/`, `terraform/rds/`, `terraform/msk/`)
- [ ] Factor shared provider/backend config into a root `terragrunt.hcl`, inherited via `include`
- [ ] Replace `terraform apply`/`destroy` with `terragrunt apply`/`destroy` (and `run-all` across modules)
- [ ] Compare: what Terragrunt buys over plain Terraform once there are 3 root modules (DRY config, remote state, orchestration)

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