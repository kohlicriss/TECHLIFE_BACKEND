resource_name = "techlife"
vpc_cidr_block = "10.0.0.0/16"
private_subnet_cidr_blocks = []
public_subnet_cidr_blocks = ["10.0.0.0/24", "10.0.2.0/24"]
availability_zones_private = []
availability_zones_public = ["ap-south-2a", "ap-south-2b"]
master_iam_role_name = "techlife_master_role"
worker_iam_role_name = "techlife_worker_role"
kubernetes_version = "1.33"
node_group = {
  "k8_node_group" = {
    ami_type = "AL2023_ARM_64_STANDARD"
    instance_type = ["t4g.medium"]
    capacity_type = "SPOT"
    desired_size  = 3
    max_size      = 3
    min_size      = 1
  }
}