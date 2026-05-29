#!/usr/bin/env bash
# Generate a 2048-bit RSA key pair for JWT RS256 signing.
# Usage: ./scripts/generate-jwt-keys.sh [output-dir]
set -euo pipefail

OUT_DIR="${1:-./jwt-keys}"
mkdir -p "$OUT_DIR"

openssl genrsa -out "$OUT_DIR/private.pem" 2048
openssl rsa -in "$OUT_DIR/private.pem" -pubout -out "$OUT_DIR/public.pem"
chmod 600 "$OUT_DIR/private.pem"

echo "Keys written to $OUT_DIR"
echo "Set in .env:"
echo "  JWT_PRIVATE_KEY_LOCATION=file:$OUT_DIR/private.pem"
echo "  JWT_PUBLIC_KEY_LOCATION=file:$OUT_DIR/public.pem"
