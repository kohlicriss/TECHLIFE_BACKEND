statements = [
  {
    actions   = ["secretsmanager:GetSecretValue", "secretsmanager:DescribeSecret"]
    resources = ["*"]
    effect    = "Allow"
  }
]
