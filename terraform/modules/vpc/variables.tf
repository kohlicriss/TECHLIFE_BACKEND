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