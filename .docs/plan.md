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
- [x] Add an API Gateway **HTTP API** in `terraform/main.tf`, with Lambda proxy integration
- [x] Add `aws_lambda_permission` so API Gateway is allowed to invoke the function
- [x] Adapt `HelloWorldHandler` to accept `APIGatewayV2HTTPEvent` / return `APIGatewayV2HTTPResponse` (add the `aws-lambda-java-events` dependency); read `name` from the query string and keep the body as plain text — no JSON (un)marshalling needed
- [x] `terraform apply`, then test the endpoint with `curl` (e.g. `curl "$API_URL/hello?name=World"`)
- [x] `terraform destroy` to close the loop

**Status:** ✅ Complete — API Gateway HTTP API wired to the Lambda, tested via curl

---

### 5. S3 Basics
- [ ] Add an `aws_s3_bucket` in Terraform
- [ ] Extend `HelloWorldHandler` to write a small object to the bucket (e.g. the `name` + a timestamp) after building its response
- [ ] `terraform apply`, then `curl` the API Gateway endpoint and verify the object appears in the bucket (console or `aws s3 ls`)
- [ ] Leave running (no destroy needed — S3 is effectively free at this scale)

**Status:** 🔮 Next up

---

### 6. RDS + VPC Basics
- [ ] New Terraform root module (`terraform/rds/`) with a VPC, subnets, security group, and an `aws_db_instance`
- [ ] Move/extend a Lambda into that VPC to read/write a simple table via JDBC
- [ ] `terraform apply`, test, **`terraform destroy` immediately after** (billed hourly regardless of use)

**Status:** 🔮 Future work

---

### 7. MSK Basics
- [ ] New Terraform root module (`terraform/msk/`), likely MSK Serverless
- [ ] A simple producer/consumer (Lambda-triggered-by-MSK)
- [ ] `terraform apply`, test, **`terraform destroy` immediately after** (billed hourly regardless of use)

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