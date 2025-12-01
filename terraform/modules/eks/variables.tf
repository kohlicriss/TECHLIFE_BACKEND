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

variable "resource_name" {
  description = "Name of the resource"
  type        = string
  default     = "eks"
}

variable "vpc_id" {
  description = "VPC ID where the EKS cluster will be created"
  type        = string
}

variable "subnet_ids" {
  description = "List of subnet IDs where the EKS cluster will be deployed"
  type        = list(string)
}

variable "kubernetes_version" {
  description = "Kubernetes version for the EKS cluster"
  type        = string
  default     = "1.21"
}

variable "node_group" {
  description = "Name of the EKS node group"
  type        = map(object({
    instance_type = list(string)
    capacity_type = string
    desired_size  = number
    max_size      = number
    min_size      = number
  }))
  default     = {
    "eks-node-group" = {
      instance_type = ["t3.medium"]
      capacity_type = "ON_DEMAND"
      desired_size  = 2
      max_size      = 3
      min_size      = 1
    }
  }
}