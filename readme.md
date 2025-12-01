Istio → OpenTelemetry → Prometheus → Grafana (metrics)
Istio → OpenTelemetry → Loki → Grafana (logs)
Istio → OpenTelemetry → Jaeger → Grafana (traces)


  initial steps 

# creat oidc provider
# menction cluter name , it specifies for wich cluster the oids provider is for

cluster_name=hrms_eks_cluster

# get a id for your cluster for OIDC
oidc_id=$(aws eks describe-cluster --name $cluster_name --query "cluster.identity.oidc.issuer" --output text | cut -d '/' -f 5)
echo $oidc_id

# verify if the oidc provider exists in the name of cluster using oids_id
aws iam list-open-id-connect-providers | grep $oidc_id | cut -d "/" -f4

# creat the oids provider, so that from the next time authentication and aitherisation can be done using it .
eksctl utils associate-iam-oidc-provider --cluster $cluster_name --approve

# after that creat get the account id and oidc provider id in this way
account_id=$(aws sts get-caller-identity --query "Account" --output text)
oidc_provider=$(aws eks describe-cluster --name hrms_eks_cluster --region ap-south-1 --query "cluster.identity.oidc.issuer" --output text | sed -e "s/^https:\/\///"
)


for every microservice this 5 steps are mandatory
1. create a role and include required permissions and register the service account with oidc so sts can verify its identity

2 files 
1.1. registering sa for oidc
cat >trust-relationship.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<Account id>:<OIDC ID>"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "$oidc_provider:aud": "sts.amazonaws.com",
          "$oidc_provider:sub": "system:serviceaccount:<name space>:<sa name>"
        }
      }
    }
  ]
}
EOF
1.2 its required permissions, this is a sample one for secrets manager
cat > secretsmanager-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": "arn:aws:secretsmanager:ap-south-1:<account id>:secret:*"
    }
  ]
}
EOF

# creat a role with the above file trust relation
aws iam create-role --role-name <Role Name> --assume-role-policy-document file://trust-relationship.json --description "<description>"

# crete olecy for the resources and attach it to the above role
aws iam create-policy --policy-name <policy name> --policy-document file://secretsmanager-policy.json

aws iam attach-role-policy --role-name <RoleName> --policy-arn=arn:aws:iam::<Account id>:policy/<policy name>

# annotate the service account with the role 
aws iam attach-role-policy --role-name <RoleName> --policy-arn=arn:aws:iam::<Account id>:policy/<policy name>


eksctl utils associate-iam-oidc-provider --cluster hrms_eks_cluster --approve

eksctl create iamserviceaccount \
  --name ebs-csi-controller-sa \
  --namespace kube-system \
  --cluster hrms_eks_cluster \
  --attach-policy-arn arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy \
  --approve \
  --role-name hrms_eks_cluster-ebs-csi-role


aws eks create-addon \
  --cluster-name hrms_eks_cluster \
  --addon-name aws-ebs-csi-driver \
  --service-account-role-arn arn:aws:iam::<account-id>:role/hrms_eks_cluster-ebs-csi-role
