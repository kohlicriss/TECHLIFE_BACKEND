variable "account_id" {
  type        = string
  description = "AWS Account ID"
}

variable "oidc_provider" {
  type        = string
  description = "OIDC provider URL without https://"
}

variable "namespace" {
  type        = string
  description = "Kubernetes namespace for the service account"
}

variable "sa_name" {
  type        = string
  description = "Service account name"
}

variable "role_name" {
  type        = string
  description = "IAM Role name to be created"
}

variable "statements" {
  description = "A map of IAM policy statements"
  type = list(object({
    actions   = list(string)
    resources = list(string)
    effect    = string
  }))
  default = []
}

variable "policy_name" {
  type        = string
  description = "IAM Policy name to create/attach"
  default     = ""
}

# variable "managed_policy_arn" {
#   type        = string
#   default     = null
#   description = "Optional AWS-managed policy ARN to attach to the role"
# }


provider "aws" {
  region = "us-east-1"
}

module "irsa" {
  source = "github.com/KoteshwarChinnolla/terraform-modules//modules/iam_oidc_trust_polecy/"
  account_id   = var.account_id
  oidc_provider = var.oidc_provider
  namespace    = var.namespace
  sa_name      = var.sa_name
  role_name    = var.role_name
  policy_name  = var.policy_name
  statements   = var.statements
}


output "role_name" {
  value = module.irsa.role_name
}

output "role_arn" {
  value = module.irsa.role_arn
}

# output "policy_arn" {
#   value       = length(aws_iam_policy.policy) > 0 ? aws_iam_policy.policy[0].arn : null
#   description = "ARN of the IAM policy if created, else null"
# }

