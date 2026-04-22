#!/bin/bash
KEY_ARN=$(awslocal kms create-key --key-spec SYMMETRIC_DEFAULT --key-usage ENCRYPT_DECRYPT --query 'KeyMetadata.Arn' --output text)
echo "KMS_KEY_ARN=${KEY_ARN}" > /tmp/kms-key-arn.env
echo "Created KMS key: ${KEY_ARN}"
