call env.bat

set CN=%1

echo "Generating %CN% keys"
keytool -genkeypair  -keysize 2048 -genkey -alias %CN% -keyalg RSA -keystore %KEYSTORE_DIR%\%CN%.keystore^
 -storepass %PASSWORD% -keypass %PASSWORD% ^
 -dname "CN=%CN%, OU=Async, O=RMI, L=Avigdor, S=NA, C=ISRAEL"


echo "Generating %CN% certificate chain"
echo "Adding ca certificate as trustcacerts"
keytool -keystore %KEYSTORE_DIR%\ca.keystore -alias ca -storepass %PASSWORD% -keypass %PASSWORD% -exportcert | ^
keytool -keystore %KEYSTORE_DIR%\%CN%.keystore -alias ca-certificate -storepass %PASSWORD% -keypass %PASSWORD%^
 -v -noprompt -trustcacerts -importcert

echo "request %CN% certificate from CA"
keytool -keystore %KEYSTORE_DIR%\%CN%.keystore -alias %CN% -storepass %PASSWORD% -keypass %PASSWORD% -certreq | ^
keytool -keystore %KEYSTORE_DIR%\ca.keystore -alias ca -storepass %PASSWORD% -keypass %PASSWORD% -gencert | ^
keytool -keystore %KEYSTORE_DIR%\%CN%.keystore -alias %CN% -storepass %PASSWORD% -keypass %PASSWORD%^
 -noprompt -importcert

