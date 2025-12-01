#!/usr/bin/env bash
set -e

# ========= CONFIG FROM ENV =========
: "${CLUSTER_NAME:?Must set CLUSTER_NAME }"
: "${REGION:?Must set REGION}"
: "${CSI_ROLE_NAME:=fetch-secrets}"
: "${ISTIO_ROLE_NAME:=istiogateway}"
: "${POLICY_NAME:=fetching-secrets-policy}"
: "${NAMESPACE_GATEWAY:=istio-gateway}"
: "${NAMESPACE_CSI:=kube-system}"
: "${SA_NAME_CSI:=secrets-store-csi-driver}"
: "${SA_NAME_GATEWAY:=default}"
# : "${EBS_ROLE_NAME:=ebs-csi-driver-role}"
# : "${EBS_NAMESPACE:=kube-system}"
# : "${EBS_SA_NAME:=ebs-csi-controller-sa}"
# : "${EBS_MANAGED_POLICY:=arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy}"


# ========= FETCH AWS INFO =========
echo "Fetching AWS account and OIDC provider..."
account_id=$(aws sts get-caller-identity --query "Account" --output text)
oidc_provider=$(aws eks describe-cluster \
  --name "$CLUSTER_NAME" \
  --region "$REGION" \
  --query "cluster.identity.oidc.issuer" \
  --output text | sed -e "s/^https:\/\///")

echo "Account ID: $account_id"
echo "OIDC Provider: $oidc_provider"

delete_role_and_policy_if_exists() {
  local role="$1"
  local policy_name="$2"

  # ===== Delete Role =====
  if aws iam get-role --role-name "$role" >/dev/null 2>&1; then
    echo "Deleting existing role: $role"

    # Detach managed policies
    for arn in $(aws iam list-attached-role-policies --role-name "$role" \
      --query 'AttachedPolicies[].PolicyArn' --output text); do
      aws iam detach-role-policy --role-name "$role" --policy-arn "$arn"
    done

    # Delete inline policies
    for inline_policy in $(aws iam list-role-policies --role-name "$role" \
      --query 'PolicyNames[]' --output text); do
      aws iam delete-role-policy --role-name "$role" --policy-name "$inline_policy"
    done

    aws iam delete-role --role-name "$role"
  fi

  # ===== Delete Customer Managed Policy =====
  if aws iam get-policy --policy-arn "arn:aws:iam::$account_id:policy/$policy_name" >/dev/null 2>&1; then
    echo "Deleting existing policy: $policy_name"

    # Delete non-default versions first
    for version in $(aws iam list-policy-versions \
      --policy-arn "arn:aws:iam::$account_id:policy/$policy_name" \
      --query 'Versions[?IsDefaultVersion==`false`].VersionId' --output text); do
      aws iam delete-policy-version \
        --policy-arn "arn:aws:iam::$account_id:policy/$policy_name" \
        --version-id "$version"
    done

    aws iam delete-policy --policy-arn "arn:aws:iam::$account_id:policy/$policy_name"
  fi
}


delete_role_and_policy_if_exists "$CSI_ROLE_NAME" "${POLICY_NAME}_${SA_NAME_CSI}"
delete_role_and_policy_if_exists "$ISTIO_ROLE_NAME" "${POLICY_NAME}_${SA_NAME_GATEWAY}"

# ========= TERRAFORM INIT =========
echo "Initializing Terraform..."
terraform -chdir=terraform/trust_policy_role init

# ========= APPLY ROLES =========
echo "Creating CSI Role..."
terraform -chdir=terraform/trust_policy_role workspace new csi || true
terraform -chdir=terraform/trust_policy_role workspace select csi
terraform -chdir=terraform/trust_policy_role apply -auto-approve \
  -var="account_id=$account_id" \
  -var="oidc_provider=$oidc_provider" \
  -var="role_name=$CSI_ROLE_NAME" \
  -var="policy_name=$POLICY_NAME" \
  -var="namespace=$NAMESPACE_CSI" \
  -var="sa_name=$SA_NAME_CSI" \
  -var-file=allow_secrets.tfvars

echo "Creating Gateway Role..."
terraform -chdir=terraform/trust_policy_role workspace new gateway || true
terraform -chdir=terraform/trust_policy_role workspace select gateway
terraform -chdir=terraform/trust_policy_role apply -auto-approve \
  -var="account_id=$account_id" \
  -var="oidc_provider=$oidc_provider" \
  -var="role_name=$ISTIO_ROLE_NAME" \
  -var="policy_name=$POLICY_NAME" \
  -var="namespace=$NAMESPACE_GATEWAY" \
  -var="sa_name=$SA_NAME_GATEWAY" \
  -var-file=allow_secrets.tfvars

echo "✅ IRSA roles created successfully"

# echo "Creating EBS CSI Driver Role..."
# terraform -chdir=terraform/trust_policy_role workspace new ebs || true
# terraform -chdir=terraform/trust_policy_role workspace select ebs

# terraform -chdir=terraform/trust_policy_role apply -auto-approve \
#   -var="account_id=$account_id" \
#   -var="oidc_provider=$oidc_provider" \
#   -var="role_name=$EBS_ROLE_NAME" \
#   -var="policy_name=none" \
#   -var="namespace=$EBS_NAMESPACE" \
#   -var="sa_name=$EBS_SA_NAME" \
#   -var="statements=[]" \
#   -var="managed_policy_arn=$EBS_MANAGED_POLICY"
