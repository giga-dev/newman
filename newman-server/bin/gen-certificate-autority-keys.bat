call env.bat

echo "Generating ca keys"

IF EXIST %KEYSTORE_DIR%\ca.keystore del /F %KEYSTORE_DIR%\ca.keystore

keytool -genkeypair  -keysize 2048 -genkey -alias ca -keyalg RSA -keystore %KEYSTORE_DIR%/ca.keystore -storepass %PASSWORD% -keypass %PASSWORD% -dname "CN=Certificate Authority, OU=Async, O=RMI, L=Avigdor, S=NA, C=ISRAEL"
