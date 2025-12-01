resource "aws_vpc" "main" {
  cidr_block = var.vpc_cidr_block
  enable_dns_hostnames = true
  enable_dns_support = true
    tags = {
        Name = "${var.resource_name}_vpc"
        "kubernetes.io/cluster/${var.resource_name}" = "shared"
        "kubernetes.io/role/internal-elb" = "1"
    }
}

resource "aws_subnet" "public_subnet" {
    count = length(var.public_subnet_cidr_blocks)
    vpc_id            = aws_vpc.main.id
    cidr_block        = var.public_subnet_cidr_blocks[count.index]
    availability_zone = var.availability_zones_public[count.index]
    map_public_ip_on_launch = true
    tags = {
        Name = "${var.resource_name}_public_subnet_${count.index}"
        "kubernetes.io/cluster/${var.resource_name}" = "shared"
        "kubernetes.io/role/elb" = "1"
    }
}

resource "aws_subnet" "private_subnet" {
  count = length(var.private_subnet_cidr_blocks)
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_cidr_blocks[count.index]
  availability_zone = var.availability_zones_private[count.index]

    tags = {
        Name = "${var.resource_name}_private_subnet_${count.index}"
        "kubernetes.io/cluster/${var.resource_name}" = "shared"
        "kubernetes.io/role/internal-elb" = "1"
    }
}

resource "aws_internet_gateway" "internet_gateway" {
  vpc_id = aws_vpc.main.id
    tags = {
        Name = "${var.resource_name}_internet_gateway"
    }
}

resource "aws_route_table" "public_route_table" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.internet_gateway.id
  }
    tags = {
        Name = "${var.resource_name}_public_route_table"
    }
}

resource "aws_route_table_association" "public_route_table_association" {
  count = length(var.public_subnet_cidr_blocks)
  subnet_id      = aws_subnet.public_subnet[count.index].id
  route_table_id = aws_route_table.public_route_table.id
}

resource "aws_eip" "nat" {
  domain   = "vpc"
}

resource "aws_nat_gateway" "nat_gateway" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public_subnet[0].id
    tags = {
        Name = "${var.resource_name}_nat_gateway"
    }
}

resource "aws_route_table" "private_route_table" {
  vpc_id = aws_vpc.main.id
    route {
        cidr_block = "0.0.0.0/0"
        nat_gateway_id = aws_nat_gateway.nat_gateway.id
    }
    tags = {
        Name = "${var.resource_name}_private_route_table"
    }
}

resource "aws_route_table_association" "private_route_table_association" {
  count = length(var.private_subnet_cidr_blocks)
  subnet_id      = aws_subnet.private_subnet[count.index].id
  route_table_id = aws_route_table.private_route_table.id
}