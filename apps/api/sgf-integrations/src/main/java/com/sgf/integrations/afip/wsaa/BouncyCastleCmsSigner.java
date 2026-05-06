package com.sgf.integrations.afip.wsaa;

import com.sgf.core.domain.BadRequestException;
import com.sgf.integrations.afip.service.AfipProperties;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Component;

@Component
public class BouncyCastleCmsSigner implements CmsSigner {

    private final AfipProperties properties;

    public BouncyCastleCmsSigner(AfipProperties properties) {
        this.properties = properties;
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public String sign(String loginTicketRequestXml) {
        try {
            SigningMaterial material = loadSigningMaterial();

            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider("BC")
                    .build(material.privateKey());
            DigestCalculatorProvider digestProvider = new JcaDigestCalculatorProviderBuilder()
                    .setProvider("BC")
                    .build();

            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(digestProvider).build(signer, material.certificate())
            );
            generator.addCertificates(new JcaCertStore(java.util.List.of(material.certificate())));

            CMSSignedData signedData = generator.generate(new CMSProcessableByteArray(loginTicketRequestXml.getBytes()), true);
            return Base64.getEncoder().encodeToString(signedData.getEncoded());
        } catch (Exception ex) {
            throw new BadRequestException("Could not sign AFIP LoginTicketRequest: " + ex.getMessage());
        }
    }

    private SigningMaterial loadSigningMaterial() throws Exception {
        if (properties.pkcs12Path() != null && !properties.pkcs12Path().isBlank()) {
            return loadFromPkcs12(Path.of(properties.pkcs12Path()), properties.pkcs12Password(), properties.pkcs12Alias());
        }
        X509Certificate certificate = loadCertificate(Path.of(requirePath(properties.certificatePath(), "certificate")));
        PrivateKey privateKey = loadPrivateKey(Path.of(requirePath(properties.privateKeyPath(), "private key")));
        return new SigningMaterial(certificate, privateKey);
    }

    private X509Certificate loadCertificate(Path path) throws Exception {
        try (var in = Files.newInputStream(path)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    private PrivateKey loadPrivateKey(Path path) throws Exception {
        String pem = Files.readString(path);
        String base64Body = pem.contains("BEGIN")
                ? extractPemBody(pem)
                : pem.replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64Body);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private String extractPemBody(String pem) throws IOException {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object object = parser.readObject();
            if (object instanceof org.bouncycastle.openssl.PEMKeyPair pemKeyPair) {
                return Base64.getEncoder().encodeToString(pemKeyPair.getPrivateKeyInfo().getEncoded());
            }
            if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo keyInfo) {
                return Base64.getEncoder().encodeToString(keyInfo.getEncoded());
            }
        }
        return pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
    }

    private String requirePath(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("AFIP " + label + " path is not configured");
        }
        return value;
    }

    private SigningMaterial loadFromPkcs12(Path path, String password, String aliasOverride) throws Exception {
        if (password == null) {
            password = "";
        }
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(path)) {
            keyStore.load(input, password.toCharArray());
        }
        String alias = aliasOverride != null && !aliasOverride.isBlank()
                ? aliasOverride
                : keyStore.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
        if (privateKey == null || certificate == null) {
            throw new BadRequestException("PKCS12 keystore does not contain a valid AFIP private key/certificate");
        }
        return new SigningMaterial(certificate, privateKey);
    }

    private record SigningMaterial(X509Certificate certificate, PrivateKey privateKey) {
    }
}
