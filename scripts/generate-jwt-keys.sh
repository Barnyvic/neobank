#!/usr/bin/env bash
# Generate a 2048-bit RSA private key for JWT RS256 (public key is derived at runtime).
# Usage: ./scripts/generate-jwt-keys.sh [output-dir]
set -euo pipefail

OUT_DIR="${1:-./jwt-keys}"
mkdir -p "$OUT_DIR"

openssl genrsa -out "$OUT_DIR/private.pem" 2048
chmod 600 "$OUT_DIR/private.pem"

echo "Private key written to $OUT_DIR/private.pem"
echo ""
echo "Add to .env:"
echo "  JWT_PRIVATE_KEY_LOCATION=file:$OUT_DIR/private.pem"
echo ""
echo "Do not commit private.pem. The jwt-keys/ directory is gitignored."
