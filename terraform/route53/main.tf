provider "aws" {
  region     = "us-west-2"
  access_key = var.access_key
  secret_key = var.secret_key
}


data "aws_route53_zone" "primary" {
  name = var.domine_name
}


resource "aws_route53_record" "www-dev" {
  zone_id = data.aws_route53_zone.primary.zone_id
  name    = "hrms.${var.domine_name}"
  type    = "CNAME"
  ttl     = 301

  allow_overwrite = true
  records         = [var.load_balencer] 
}

variable "access_key" {
  type = string
}

variable "secret_key" {
  type = string
}

variable "load_balencer" {
  type = string
}

variable "domine_name" {
  type    = string
  default = "anasolconsultancyservices.com"
}
