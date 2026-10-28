include "root" {
  path = find_in_parent_folders("root.hcl")
}

dependency "network" {
  config_path = "../network"

  # Placeholders while network has no outputs yet (e.g. run --all plan before the first apply)
  mock_outputs = {
    vpc_id            = "vpc-00000000"
    private_subnet_id = "subnet-11111111"
  }
  mock_outputs_allowed_terraform_commands = ["validate", "plan"]
}

inputs = {
  vpc_id            = dependency.network.outputs.vpc_id
  private_subnet_id = dependency.network.outputs.private_subnet_id
}