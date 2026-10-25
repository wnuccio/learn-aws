# --- Security groups -------------------------------------------------------

resource "aws_security_group" "bastion" {
  name   = "${var.name}-bastion-sg"
  vpc_id = var.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "bastion_from_my_ip" {
  security_group_id = aws_security_group.bastion.id
  cidr_ipv4         = var.my_ip_cidr
  ip_protocol       = "tcp"
  from_port         = 22
  to_port           = 22
}

resource "aws_security_group" "target" {
  name   = "${var.name}-target-sg"
  vpc_id = var.vpc_id
}

resource "aws_vpc_security_group_egress_rule" "bastion_to_target" {
  security_group_id            = aws_security_group.bastion.id
  referenced_security_group_id = aws_security_group.target.id
  ip_protocol                  = "tcp"
  from_port                    = 22
  to_port                      = 22
}

resource "aws_vpc_security_group_ingress_rule" "target_from_bastion" {
  security_group_id            = aws_security_group.target.id
  referenced_security_group_id = aws_security_group.bastion.id
  ip_protocol                  = "tcp"
  from_port                    = 22
  to_port                      = 22
}

# --- Instances -------------------------------------------------------------

# Latest Amazon Linux 2023 image, published by AWS as a public SSM parameter (login user: ec2-user)
data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# Only the public key goes to AWS; the private key stays on the laptop
resource "aws_key_pair" "main" {
  key_name   = "${var.name}-key"
  public_key = file(pathexpand(var.public_key_path))
}

resource "aws_instance" "bastion" {
  ami                         = data.aws_ssm_parameter.al2023.value
  instance_type               = "t3.micro"
  subnet_id                   = var.public_subnet_id
  vpc_security_group_ids      = [aws_security_group.bastion.id]
  associate_public_ip_address = true
  key_name                    = aws_key_pair.main.key_name

  tags = {
    Name = "${var.name}-bastion"
  }
}

resource "aws_instance" "target" {
  ami                         = data.aws_ssm_parameter.al2023.value
  instance_type               = "t3.micro"
  subnet_id                   = var.private_subnet_id
  vpc_security_group_ids      = [aws_security_group.target.id]
  associate_public_ip_address = false
  key_name                    = aws_key_pair.main.key_name

  tags = {
    Name = "${var.name}-target"
  }
}