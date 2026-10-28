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
- [x] `apply`, then SSH to the private instance through the bastion (`ssh -J`)
  - apply blocked from 2026-10-13 (`RunInstances`: `This account is currently blocked`) until AWS Support lifted it on 2026-10-24
  - verified: `hostname -I` on the target shows `10.0.2.x`; its `curl` to the internet times out; direct `ssh` from the laptop fails
  - host keys removed afterwards with `ssh-keygen -R` (the IPs may come back at the next apply)
- [ ] ~~Optional: reach the RDS of `lambda2/` through the bastion (SSH tunnel)~~ skipped (2026-10-28): the two VPCs are separate, so it would need a second bastion in `lambda2/`; discussed in theory only
- [x] Compare: SSM Session Manager as an alternative (no open port, no keys)
  - [x] new unit `terraform/bastion/ssm/`, with a `dependency` on `network` (like `instances`)
  - [x] IAM role for EC2 with `AmazonSSMManagedInstanceCore`, assigned through an `aws_iam_instance_profile`
  - [x] one EC2 in the private subnet: no key pair, no SSH rule, no public IP
  - [x] three interface endpoints (`ssm`, `ssmmessages`, `ec2messages`), with a security group allowing 443 from the instance
    - one `aws_vpc_endpoint` with `for_each`; private DNS needs `enable_dns_hostnames = true` on the VPC (added in `network`)
  - [x] install the Session Manager plugin (`brew install --cask session-manager-plugin`)
  - [x] `apply`, then test as in "Test SSM" of `README.md` (`aws ssm start-session`); **`destroy` right after**
    - destroy is slow: the endpoints stay in `deleting` for several minutes while AWS removes their ENIs
- [x] **`destroy` immediately after** (EC2 and public IPv4 billed hourly)

**Status:** ✅ Complete — `ssh -J` through the bastion verified (2026-10-24); shell on the private instance through Session Manager, with no bastion, no open port and no key, destroyed (2026-10-28); the RDS tunnel skipped

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
- [x] `apply`, then `docker build` and `docker push` to ECR: the service retries until the image exists
  - 2026-10-15: blocked by the account block (as step 10); 2026-10-24: worked
  - before the push, a stopped task about every 2 minutes with `CannotPullContainerError ... not found`; the next retry after the push started the task, no forced deployment needed
  - the task definition was revision 2: `destroy` only deregisters a revision (INACTIVE), and the counter of a family never resets
  - in zsh, `"$REPO:latest"` is wrong (`:l` is a modifier): use `"${REPO}:latest"`
- [x] Find the task's public IP (CLI or console), `curl` it; check the logs in CloudWatch
- [x] **`destroy` immediately after** (billed per second while running)

**Status:** ✅ Complete — task running on Fargate, `curl` returned `Hello, World!`, logs checked, destroyed (2026-10-24)

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
- [x] New module `terraform/remote-state/`, included in Terragrunt like the others; the existing modules keep their local state
  - [x] a single `aws_ssm_parameter` (String, standard tier: free, created instantly, easy to change by hand)
    - `insecure_value` instead of `value`, which is always `sensitive` and hidden in plans
    - switching an existing parameter from `value` to `insecure_value` gave a perpetual diff (provider 5.100.0): fixed by recreating it
  - [x] a `remote_state` block in the module's own `terragrunt.hcl` (not in `root.hcl`)
    - backend `s3`, bucket named with the account ID (`get_aws_account_id()`), `encrypt = true`, `use_lockfile = true`
    - `key = "${path_relative_to_include()}/terraform.tfstate"`
    - `generate`: Terragrunt writes `backend.tf` in the module, like `provider.tf`
- [x] Bootstrap: the first `init` with `--backend-bootstrap` creates the bucket (versioning, encryption, public access blocked)
- [x] `apply`, then check the state at `remote-state/terraform.tfstate` in the bucket
- [x] Lock: leave an `apply` waiting at the "yes" prompt; meanwhile the `.tflock` file is in the bucket, and a second `apply` from another terminal fails with "state locked"
- [x] Drift: change the parameter's value by hand; `plan` refreshes and proposes to set it back to the code
- [x] `destroy`: the state file stays in the bucket, empty; its previous versions are visible (versioning)
  - the lock file has versions too: each apply writes it, then deletes it (a delete marker)
- [x] Delete the bucket by hand from the console (empty it first, versions included)

**Status:** ✅ Complete — bootstrap, lock, drift and versioning verified in `terraform/remote-state/` (2026-10-21); bucket deleted

---

### 14. Terragrunt Environments
- [x] `dev/` and `prod/` configurations of one module (`remote-state`, free and instant, instead of `lambda/`), differing only in their inputs
  - module code in `terraform/modules/remote-state/` (no `terragrunt.hcl`); deployments in `terraform/live/dev/remote-state/` and `terraform/live/prod/remote-state/`, via `terraform { source = ... }`
  - the old `terraform/remote-state/` module removed
- [x] Parameter name and value come from inputs, so the environments don't collide: `/learn-aws/<env>/demo`
- [x] `env.hcl` per environment holds all the per-environment data (`env`, `parameter_value`), read with `read_terragrunt_config` (data, not config, so not an `include`)
  - the two deployment `terragrunt.hcl` files are identical
- [x] Remote state: `remote_state` moved to `terraform/live/backend.hcl`, a second `include` next to `root.hcl` (not in `root.hcl`, so the other modules keep their local state)
  - `path_relative_to_include("backend")` gives the keys `dev/remote-state/` and `prod/remote-state/` in the same bucket
  - region read from `root.hcl` with `read_terragrunt_config` (an included file can't see `include.root`)
- [x] "Test Remote State" (dev only: bootstrap, lock, drift, versions) and "Test Environments" (dev and prod side by side) in `README.md`, independent of each other
- [x] Both tests run, everything destroyed, bucket deleted

**Status:** ✅ Complete — two environments from one module, separate state keys in one bucket (2026-10-22)

---

### 15. Terragrunt Dependency Blocks
- Problem: one root module means one state and one `apply` for parts with different lifecycles (the network rarely changes, the instances often)
- Split `terraform/bastion/` in place (old version in git; nothing deployed, so no state to migrate), simple layout (no `modules/` + `live/`, local state)
- [x] `terraform/bastion/network/`: VPC, both subnets, internet gateway, public route table and its association
  - outputs: `vpc_id`, `public_subnet_id`, `private_subnet_id`
- [x] `terraform/bastion/instances/`: security groups, key pair, AMI lookup, both instances; same outputs as before, plus `known_hosts_cleanup_command`
  - `dependency "network"` in its `terragrunt.hcl`, outputs passed as `inputs`; `aws_subnet.public.id` becomes `var.public_subnet_id`, and so on
  - `mock_outputs` for `plan` before `network` exists, allowed only for `validate` and `plan`
- [x] Remove the old files from `terraform/bastion/` (`main.tf`, `outputs.tf`, `variables.tf`, `terragrunt.hcl`, local caches)
- [x] `run --all plan`, then `run --all apply` from `terraform/bastion/`: `network` first
  - `run --all apply` asks one confirmation for the whole stack, then applies each unit with `-auto-approve`
- [x] Same SSH test as step 10 ("Test Bastion" updated in `README.md`)
- [x] Try `destroy` in `network/` alone while `instances` exists
  - Terragrunt 1.x doesn't check dependents by default: only Terraform's own prompt; answered no
  - with `--destroy-dependencies-check` it lists `instances` and asks to confirm; answered `n`
- [x] `run --all destroy`: reverse order

**Status:** ✅ Complete — two units wired by a `dependency` block, applied and destroyed in order with `run --all` (2026-10-25)

---

## Key Concepts to Understand

- **Terraform** = Infrastructure as Code (IaC) — defines *where and how* your Lambda runs
- **Lambda** = Compute logic — the actual code that executes
- **Using Terraform to deploy Lambda** = Modern, reproducible, version-controlled approach

## Setup Notes

- Using personal hotspot for AWS access (corporate network has SSL blocking)
- AWS credentials stored locally, never committed to repo
- This is a personal learning project, separate from work