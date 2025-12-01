provider "aws" {
  region = "ap-south-1"
}

module "s3" {
  source = "github.com/KoteshwarChinnolla/terraform-modules//modules/s3_bucket"
  bucket_name = "hrms-postgress-bucket"
}