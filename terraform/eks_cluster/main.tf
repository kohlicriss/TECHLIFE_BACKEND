provider "aws" {
  region = "ap-south-2"
}

terraform {
  backend "s3" {
    bucket = "techlife-eks-cluster"
    key    = "terraform/state"
    region = "ap-south-2"
  }
}


module "vpc" {
    source = "github.com/KoteshwarChinnolla/terraform-modules//modules/vpc"
    resource_name = var.resource_name
    vpc_cidr_block = var.vpc_cidr_block
    private_subnet_cidr_blocks = var.private_subnet_cidr_blocks
    public_subnet_cidr_blocks = var.public_subnet_cidr_blocks
    availability_zones_private = var.availability_zones_private
    availability_zones_public = var.availability_zones_public
}


module "eks" {
    source = "github.com/KoteshwarChinnolla/terraform-modules//modules/eks"
    resource_name = var.resource_name
    vpc_id = module.vpc.vpc_id
    subnet_ids = module.vpc.public_subnet_ids
    kubernetes_version = var.kubernetes_version
    master_iam_role_name = var.master_iam_role_name
    worker_iam_role_name = var.worker_iam_role_name
    node_group = var.node_group

    depends_on = [ module.vpc ]
}

# https://devopscube.com/provsion-persistent-volume-on-eks/