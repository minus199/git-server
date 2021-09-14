rm -rf ./certs
mkdir -p ./certs/server
mkdir -p ./certs/root-ca
cd ./certs

keytool -genkeypair -keyalg RSA -keysize 3072 \
  -alias root-ca -dname "CN=My Root CA,OU=Development,O=My Organization,C=DE" \
  -ext BC:c=ca:true -ext KU=keyCertSign \
  -validity 3650 \
  -keystore ./root-ca/ca.jks -storepass secret \
  -keypass secret

keytool -genkeypair -keyalg RSA -keysize 3072 -alias gradify.io \
  -dname "CN=gradify.io,OU=Development,O=My Organization,C=DE" \
  -ext BC:c=ca:false -ext EKU:c=serverAuth -ext "SAN:c=DNS:gradify.io,IP:127.0.0.1" \
  -validity 3650 \
  -keystore ./server/server.jks -storepass secret \
  -keypass secret

keytool -certreq -keystore ./server/server.jks -storepass secret -alias gradify.io -keypass secret -file ./server/server.csr
keytool -gencert -keystore ./root-ca/ca.jks -storepass secret -infile ./server/server.csr \
  -alias root-ca -keypass secret \
  -ext BC:c=ca:false -ext EKU:c=serverAuth -ext "SAN:c=DNS:gradify.io,IP:127.0.0.1" \
  -validity 3650 -rfc -outfile ./server/server.pem

keytool -importcert -noprompt \
 -keystore ./server/server.jks -storepass secret \
 -alias root-ca -keypass secret -file ./root-ca/ca.pem


keytool -importcert -noprompt -keystore ./server/server.jks -storepass secret -alias gradify.io -keypass secret -file ./server/server.pem

#openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout lee.key -out lee.crt -config openssl.cnf

sudo ln -s /etc/ssl/your-cert.crt /usr/share/ca-certificates/your-cert.crt
