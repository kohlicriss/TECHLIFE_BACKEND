output "cluster_endpoint" {
  value       = aws_eks_cluster.eks_cluster.endpoint
  description = "The endpoint of the EKS cluster"
}

output "cluster_name" {
  value       = aws_eks_cluster.eks_cluster.name
  description = "The name of the EKS cluster"
}