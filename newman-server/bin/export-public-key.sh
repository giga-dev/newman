#!/usr/bin/env bash
. ./env.sh

echo -e "${Yellow}Exporting server public key to PEM format${Color_Off}"

# Export certificate from keystore
keytool -exportcert -alias server -keystore $KEYSTORE_DIR/server.keystore \
  -storepass $PASSWORD -rfc -file $KEYSTORE_DIR/server-cert.pem

# Extract public key from certificate
openssl x509 -in $KEYSTORE_DIR/server-cert.pem -pubkey -noout > $KEYSTORE_DIR/server-public.pem

# Clean up certificate file
rm -f $KEYSTORE_DIR/server-cert.pem

echo -e "${Green}Public key exported to $KEYSTORE_DIR/server-public.pem${Color_Off}"
