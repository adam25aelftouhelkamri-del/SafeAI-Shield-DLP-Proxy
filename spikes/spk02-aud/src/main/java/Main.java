import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * SafeAI Shield - Prueba de concepto SPK-02.
 * Audita el firewall, los puertos abiertos y los permisos de la clave privada,
 * y firma el informe con ECDSA (EC 256 + SHA-256).
 *
 * @author Abdellah El Ftouh El Kamriu
 */
public class Main {
    public static void main(String[] args) throws Exception {
        List<String> reportLines = new ArrayList<>();
        String keyFilePath = "C:\\dev\\audit-test\\private-key.txt";

        // Comandos de Windows que se ejecutan desde Java
        ProcessBuilder ports = new ProcessBuilder("netstat", "-an");
        ProcessBuilder firewall = new ProcessBuilder("netsh", "advfirewall", "show", "allprofiles", "state");
        ProcessBuilder fileper = new ProcessBuilder("icacls", keyFilePath);

        // ===== 1. FIREWALL =====
        Process processFirewall = firewall.start();
        List<String> results = new ArrayList<>();   // Ej.: "Public: ON"

        try (BufferedReader readerFirewall = new BufferedReader(new InputStreamReader(processFirewall.getInputStream()))) {
            String currentProfile = "";
            String line;

            while ((line = readerFirewall.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.endsWith("Profile Settings:")) {     // Ej.: "Public Profile Settings:"
                    String[] words = trimmed.split("\\s+");
                    currentProfile = words[0];
                }

                if (trimmed.startsWith("State")) {               // Ej.: "State      ON"
                    String[] words = trimmed.split("\\s+");
                    String lastWord = words[words.length - 1];
                    results.add(currentProfile + ": " + lastWord);
                }
            }
        }
        processFirewall.waitFor();

        // Perfiles desactivados
        List<String> offProfiles = new ArrayList<>();
        for (String result : results) {
            if (result.endsWith("OFF")) {
                offProfiles.add(result.split(":")[0]);
            }
        }

        if (results.size() != 3) {
            reportLines.add("Firewall: ERROR - could not read firewall state (found " + results.size() + " of 3 profiles)");
        } else if (!offProfiles.isEmpty()) {
            reportLines.add("Firewall: WARNING - profile(s) off: " + String.join(", ", offProfiles));
        } else {
            reportLines.add("Firewall: OK - all profiles active (Domain, Private, Public)");
        }

        // ===== 2. PUERTOS ABIERTOS =====
        Process processPorts = ports.start();
        Set<String> openPorts = new TreeSet<>();   // Sin duplicados
        List<String> notAllowed = new ArrayList<>();
        List<String> allowed = new ArrayList<>();
        List<String> allowedPorts = List.of("443", "80", "8080");   // Lista blanca

        try (BufferedReader readerPorts = new BufferedReader(new InputStreamReader(processPorts.getInputStream()))) {
            String line;

            while ((line = readerPorts.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.contains("LISTENING")) {
                    String[] words = trimmed.split("\\s+");
                    String address = words[1];
                    int pos = address.lastIndexOf(":");
                    // Se ignoran las direcciones locales (solo accesibles desde el propio equipo)
                    if (!address.startsWith("127.") && !address.startsWith("[::1]")) {
                        String port = address.substring(pos + 1);
                        openPorts.add(port);
                    }
                }
            }

            if (openPorts.isEmpty()) {
                reportLines.add("Open ports: ERROR - could not read netstat output (no LISTENING lines found)");
            } else {
                for (String port : openPorts) {
                    if (allowedPorts.contains(port)) {
                        allowed.add(port);
                    } else {
                        notAllowed.add(port);
                    }
                }
                if (notAllowed.isEmpty()) {
                    reportLines.add("Open ports: OK - open and allowed: " + String.join(", ", allowed));
                } else {
                    reportLines.add("Open ports: WARNING - unexpected ports listening: " + String.join(", ", notAllowed));
                }
            }
        }
        processPorts.waitFor();

        // ===== 3. PERMISOS DE LA CLAVE PRIVADA =====
        Process processFilePer = fileper.start();
        List<String> NotAllowedGroups = new ArrayList<>();
        List<String> allowedGroups = new ArrayList<>();   // Solo SYSTEM y Administrators

        try (BufferedReader readerfileper = new BufferedReader(new InputStreamReader(processFilePer.getInputStream()))) {
            String line;

            while ((line = readerfileper.readLine()) != null) {
                String trimmed = line.trim();
                int pos = trimmed.lastIndexOf(":");             // Ej.: "BUILTIN\Users:(I)(RX)"
                if (pos == -1) continue;
                String permission = trimmed.substring(0, pos);
                if (permission.endsWith("Administrators") || permission.endsWith("SYSTEM")) {
                    String name = permission.substring(permission.lastIndexOf("\\") + 1);
                    allowedGroups.add(name);
                } else {
                    String name = permission.substring(permission.lastIndexOf("\\") + 1);
                    NotAllowedGroups.add(name);
                }
            }

            if (NotAllowedGroups.isEmpty() && allowedGroups.isEmpty()) {
                reportLines.add("File permissions: ERROR - file not found: " + keyFilePath);
            } else {
                if (!NotAllowedGroups.isEmpty()) {
                    reportLines.add("File permissions: WARNING - too broad access: " + String.join(", ", NotAllowedGroups));
                } else {
                    reportLines.add("File permissions: OK - only SYSTEM and Administrators have access");
                }
            }
        }
        processFilePer.waitFor();

        // ===== 4. INFORME =====
        String report = "=== SafeAI Shield - NIST CSF Audit Report ===\n" + "Date: " + LocalDateTime.now().withNano(0) + "\n" + String.join("\n", reportLines);
        System.out.println(report);

        // ===== 5. FIRMA DIGITAL =====
        System.out.println("\n===== SIGNATURE =====");

        // Par de claves EC de 256 bits (nuevo en cada ejecución)
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair pair = generator.generateKeyPair();
        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();

        // Firmar con la clave privada
        byte[] data = report.getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(privateKey);
        signer.update(data);
        byte[] signature = signer.sign();

        // Base64 para mostrar los bytes como texto
        Base64.Encoder b64 = Base64.getEncoder();
        String publicKeyB64 = b64.encodeToString(publicKey.getEncoded());
        String signatureB64 = b64.encodeToString(signature);

        System.out.println("Algorithm : SHA256withECDSA (EC 256)");
        System.out.println("Public key: " + publicKeyB64);
        System.out.println("Signature : " + signatureB64);

        // Verificar con la clave pública
        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(publicKey);
        verifier.update(data);
        System.out.println("Verified  : " + verifier.verify(signature));

        // Simular manipulación: alguien oculta un WARNING
        String tampered = report.replace("WARNING", "OK");
        Signature verifierTampered = Signature.getInstance("SHA256withECDSA");
        verifierTampered.initVerify(publicKey);
        verifierTampered.update(tampered.getBytes(StandardCharsets.UTF_8));
        System.out.println("Tampered  : " + verifierTampered.verify(signature));

        // ===== 6. GUARDAR =====
        Path outDir = Path.of("C:\\dev\\audit-test");
        Files.writeString(outDir.resolve("audit-report.txt"), report, StandardCharsets.UTF_8);
        Files.writeString(outDir.resolve("audit-report.sig"), signatureB64);
        Files.writeString(outDir.resolve("public-key.txt"), publicKeyB64);
        System.out.println("Saved in  : " + outDir);


    }
}