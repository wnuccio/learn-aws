# --- IAM -------------------------------------------------------------------

# The SSM agent on the instance calls Systems Manager with this role's credentials
resource "aws_iam_role" "instance" {
  name = "${var.name}-instance-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

# Registration through ssm, and the control and data channels through ssmmessages and ec2messages
resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.instance.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# An EC2 instance gets its role through an instance profile, not directly
resource "aws_iam_instance_profile" "instance" {
  name = "${var.name}-instance-profile"
  role = aws_iam_role.instance.name
}

# --- Security group --------------------------------------------------------

# No ingress rules: nothing connects to the instance; the agent only calls out
resource "aws_security_group" "instance" {
  name   = "${var.name}-instance-sg"
  vpc_id = var.vpc_id
}

# For the three interface endpoints, which the agent calls over HTTPS
resource "aws_security_group" "endpoints" {
  name   = "${var.name}-endpoints-sg"
  vpc_id = var.vpc_id
}

resource "aws_vpc_security_group_egress_rule" "instance_to_endpoints" {
  security_group_id            = aws_security_group.instance.id
  referenced_security_group_id = aws_security_group.endpoints.id
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
}

resource "aws_vpc_security_group_ingress_rule" "endpoints_from_instance" {
  security_group_id            = aws_security_group.endpoints.id
  referenced_security_group_id = aws_security_group.instance.id
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
}

# --- Instance --------------------------------------------------------------

# Latest Amazon Linux 2023 image, with the SSM agent preinstalled
data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# No key pair and no public IP: the only way in is a Session Manager session
resource "aws_instance" "main" {
  ami                         = data.aws_ssm_parameter.al2023.value
  instance_type               = "t3.micro"
  subnet_id                   = var.private_subnet_id
  vpc_security_group_ids      = [aws_security_group.instance.id]
  associate_public_ip_address = false
  iam_instance_profile        = aws_iam_instance_profile.instance.name

  tags = {
    Name = "${var.name}-instance"
  }
}

# --- VPC endpoints ---------------------------------------------------------

# Private doors to the three services the agent calls: ssm (registration), ssmmessages (control and data channels),
# ec2messages (older command channel). With private DNS, e.g. ssm.<region>.amazonaws.com resolves to the endpoint
# inside the VPC; this needs enable_dns_hostnames on the VPC.
resource "aws_vpc_endpoint" "ssm" {
  for_each = toset(["ssm", "ssmmessages", "ec2messages"])

  vpc_id              = var.vpc_id
  service_name        = "com.amazonaws.${var.aws_region}.${each.key}"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = [var.private_subnet_id]
  security_group_ids  = [aws_security_group.endpoints.id]
  private_dns_enabled = true

  tags = {
    Name = "${var.name}-${each.key}"
  }
}