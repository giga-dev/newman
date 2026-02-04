package com.gigaspaces.newman;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for SSH-based console output operations.
 */
public class SSHUtils {

    private static final Logger logger = LoggerFactory.getLogger(SSHUtils.class);

    /**
     * Result of an SSH command execution.
     */
    public static class SSHResult {
        private final boolean success;
        private final String output;
        private final String error;

        public SSHResult(boolean success, String output, String error) {
            this.success = success;
            this.output = output;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public String getOutput() { return output; }
        public String getError() { return error; }
    }

    /**
     * Reads the public key from the keys directory.
     */
    public static String readPublicKey(String keysDir) throws IOException {
        Path publicKeyPath = Paths.get(keysDir, "server-public.pem");

        if (!Files.exists(publicKeyPath)) {
            throw new IOException("Public key file not found: " + publicKeyPath);
        }

        return new String(Files.readAllBytes(publicKeyPath));
    }

    /**
     * Decrypts PEM content that was encrypted by the frontend using hybrid RSA/AES encryption.
     */
    public static String decryptPemContent(String encryptedKeyBase64, String encryptedPemBase64,
            String ivBase64, String keysDir, String keystorePassword) throws Exception {

        Path keystorePath = Paths.get(keysDir, "server.keystore");

        // Load private key from keystore
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystorePath.toFile())) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }
        java.security.PrivateKey privateKey = (java.security.PrivateKey) keyStore.getKey("server", keystorePassword.toCharArray());

        // Decrypt AES key with RSA-OAEP (SHA-256 for both hash and MGF1 to match JavaScript Web Crypto)
        javax.crypto.Cipher rsaCipher = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPPadding");
        java.security.spec.MGF1ParameterSpec mgf1Spec = java.security.spec.MGF1ParameterSpec.SHA256;
        javax.crypto.spec.OAEPParameterSpec oaepSpec = new javax.crypto.spec.OAEPParameterSpec(
                "SHA-256", "MGF1", mgf1Spec, javax.crypto.spec.PSource.PSpecified.DEFAULT);
        rsaCipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey, oaepSpec);
        byte[] aesKeyBytes = rsaCipher.doFinal(Base64.getDecoder().decode(encryptedKeyBase64));

        // Decrypt PEM content with AES-GCM
        javax.crypto.spec.SecretKeySpec aesKey = new javax.crypto.spec.SecretKeySpec(aesKeyBytes, "AES");
        byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
        javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(128, ivBytes);

        javax.crypto.Cipher aesCipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(javax.crypto.Cipher.DECRYPT_MODE, aesKey, gcmSpec);
        byte[] pemBytes = aesCipher.doFinal(Base64.getDecoder().decode(encryptedPemBase64));

        return new String(pemBytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Builds the remote command for fetching log file content.
     * Uses awk for date filtering when sinceDate is provided.
     */
    public static String buildLogFileCommand(String logFile, String sinceDate, int lines) {
        if (sinceDate != null && !sinceDate.trim().isEmpty()) {
            // Filter from sinceDate, then limit lines (head for first N lines from that date)
            // Use first char of sinceDate to identify timestamp lines (adapts to any date format)
            // Non-timestamp lines included only if previous timestamp matched
            int len = sinceDate.length();
            String escapedDate = sinceDate.replace("\"", "\\\"");
            String firstChar = sinceDate.substring(0, 1).replace("\"", "\\\"");
            return "awk 'substr($0,1,1)==\"" + firstChar + "\" { inc=(substr($0,1," + len + ")>=\"" + escapedDate + "\") } inc' " + logFile + " | head -n " + lines;
        } else {
            // Use tail for file-based logs without date filtering
            return "tail -n " + lines + " " + logFile;
        }
    }

    /**
     * Builds the remote command for fetching journalctl service logs.
     */
    public static String buildJournalctlCommand(String service, String since, int lines) {
        StringBuilder journalCmd = new StringBuilder();
        journalCmd.append("journalctl -u ").append(service).append(" -n ").append(lines);
        if (since != null && !since.trim().isEmpty()) {
            journalCmd.append(" --since '").append(since.replace("'", "\\'")).append("'");
        }
        return journalCmd.toString();
    }

    /**
     * Creates a temporary PEM file with proper permissions.
     */
    public static Path createTempPemFile(String pemContent) throws IOException {
        Path tempPemFile = Files.createTempFile("newman-ssh-", ".pem");
        Files.write(tempPemFile, pemContent.getBytes());
        // Set proper permissions (owner read only)
        tempPemFile.toFile().setReadable(false, false);
        tempPemFile.toFile().setReadable(true, true);
        tempPemFile.toFile().setWritable(false, false);
        return tempPemFile;
    }

    /**
     * Deletes a temporary PEM file safely.
     */
    public static void deleteTempPemFile(Path tempPemFile) {
        if (tempPemFile != null) {
            try {
                Files.deleteIfExists(tempPemFile);
            } catch (IOException e) {
                logger.warn("Failed to delete temp PEM file: " + tempPemFile, e);
            }
        }
    }

    /**
     * Executes an SSH command on a remote host.
     */
    public static SSHResult executeSSHCommand(String host, String user, Path pemFile,
            String remoteCommand, int timeoutSeconds) {

        List<String> command = new ArrayList<>();
        command.add("ssh");
        command.add(host);
        command.add("-l");
        command.add(user);
        command.add("-i");
        command.add(pemFile.toString());
        command.add("-o");
        command.add("StrictHostKeyChecking=no");
        command.add("-o");
        command.add("ConnectTimeout=10");
        command.add(remoteCommand);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new SSHResult(false, null, "SSH command timed out after " + timeoutSeconds + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0 && output.length() == 0) {
                return new SSHResult(false, null, "SSH command failed with exit code: " + exitCode);
            }

            return new SSHResult(true, output.toString(), null);

        } catch (Exception e) {
            logger.error("Failed to execute SSH command for host " + host, e);
            return new SSHResult(false, null, "Failed to execute SSH command: " + e.getMessage());
        }
    }
}
