# Quiz (2026-10-26)
- score: 8/10; gaps were mostly "who does what, and where"
## Conceptual misses
- API Gateway 503: its 30 s integration limit is fixed; the cause was a slow cold start at 128 MB, fixed with `memory_size = 1024`
- a Lambda with `vpc_config` runs in Lambda-managed infrastructure; only its ENI is in the VPC (outbound only)
- API Gateway calls the Lambda `Invoke` API, not the function's network
- the event source mapping is run by the Lambda service, not by MSK; the poller is the real Kafka consumer
- `AlterGroup` / `DescribeGroup`: the poller's consumer group offsets, not attaching function instances
## Details to remember
- `dependency` block: orders the units and passes the dependency's outputs to the dependent
- `mock_outputs` only for `validate` and `plan`, so `apply` never uses fake IDs
- `ResourceNotFoundException` covers both "no value yet" and "wrong secret name": both return 404
- `file()` reads the content of the public key, not a path
- SSH: the private key signs, the public key verifies; no server challenge, the client signs fresh session data
- `ssh -J` only forwards TCP; the session and the key stay between the laptop and the target
- the two `terragrunt.hcl` files of `dev` and `prod` are identical; `env.hcl` differs
- only the first `apply` creates the lock file; a second one fails because it exists
## Small slips
- SSH port is 22
- `path_relative_to_include`: from the included file's folder to the including one
- stateful security group = connection tracking
- a task definition is an independent versioned template, not inside a cluster
