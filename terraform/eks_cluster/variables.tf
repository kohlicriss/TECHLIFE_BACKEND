variable "resource_name" {
  description = "Name of the resource"
  type        = string
  default     = "vpc"
}

variable "vpc_cidr_block" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "private_subnet_cidr_blocks" {
  description = "List of CIDR blocks for private subnets"
  type        = list(string)
  default     = []
}

variable "public_subnet_cidr_blocks" {
  description = "List of CIDR blocks for public subnets"
  type        = list(string)
  default     = []
}

variable "availability_zones_private" {
  description = "List of availability zones for the VPC"
  type        = list(string)
  default     = []
}

variable "availability_zones_public" {
  description = "List of availability zones for public subnets"
  type        = list(string)
  default     = []
}

variable "master_iam_role_name" {
  description = "Name of the IAM role for the EKS master node"
  type        = string
  default     = "eks-master-role"
}

variable "worker_iam_role_name" {
  description = "Name of the IAM role for the EKS worker nodes"
  type        = string
  default     = "eks-worker-role"
}

variable "kubernetes_version" {
  description = "Kubernetes version for the EKS cluster"
  type        = string
  default     = "1.21"
}

variable "node_group" {
  description = "Name of the EKS node group"
  type        = map(object({
    ami_type = string
    instance_type = list(string)
    capacity_type = string
    desired_size  = number
    max_size      = number
    min_size      = number
  }))
  default     = {
    "eks-node-group" = {
      ami_type = "AL2023_ARM_64_STANDARD"
      instance_type = ["t3.medium"]
      capacity_type = "ON_DEMAND"
      desired_size  = 2
      max_size      = 3
      min_size      = 1
    }
  }
}