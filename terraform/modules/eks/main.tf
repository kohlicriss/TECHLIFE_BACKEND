resource "aws_iam_role" "master_iam_role" {
    name = "${var.resource_name}-master-role"
    assume_role_policy = jsonencode({
        Version = "2012-10-17"
        Statement = [{
            Action = "sts:AssumeRole"
            Effect = "Allow"
            Principal = {
                Service = "eks.amazonaws.com"
            }
        }]
    })
}

resource "aws_iam_role_policy_attachment" "master_policy_attachment" {
    role       = aws_iam_role.master_iam_role.name
    policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_eks_cluster" "eks_cluster" {
    name     = var.resource_name
    version = var.kubernetes_version
    role_arn = aws_iam_role.master_iam_role.arn
    vpc_config {
        subnet_ids = var.subnet_ids
    }
    depends_on = [ aws_iam_role_policy_attachment.master_policy_attachment ]
  
}

resource "aws_iam_role" "worker_iam_role" {
    name = "${var.resource_name}-worker-role"
    assume_role_policy = jsonencode({
        Version = "2012-10-17"
        Statement = [{
            Action = "sts:AssumeRole"
            Effect = "Allow"
            Principal = {
                Service = "ec2.amazonaws.com"
            }
        }]
    })
}

resource "aws_iam_role_policy_attachment" "worker_policy_attachment" {
    for_each = toset([
        "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
        "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly",
        "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy",
        "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy"
    ])
    policy_arn = each.value
    role       = aws_iam_role.worker_iam_role.name
}

resource "aws_eks_node_group" "work_nodes_eks" {
    for_each = var.node_group
    
    cluster_name = aws_eks_cluster.eks_cluster.name
    node_group_name = each.key
    node_role_arn = aws_iam_role.worker_iam_role.arn
    subnet_ids = var.subnet_ids

    instance_types = each.value.instance_type
    capacity_type = each.value.capacity_type

    scaling_config {
        desired_size = each.value.desired_size
        max_size     = each.value.max_size
        min_size     = each.value.min_size
    }

    depends_on = [ aws_iam_role_policy_attachment.worker_policy_attachment ]
}