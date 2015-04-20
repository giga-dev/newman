call env.bat

# main entry to create server and client keys signed by ca.

rd %KEYSTORE_DIR% /Q /S

mkdir %KEYSTORE_DIR%

call gen-certificate-autority-keys.bat

call gen-key-and-certificate.bat server

call gen-key-and-certificate.bat client
