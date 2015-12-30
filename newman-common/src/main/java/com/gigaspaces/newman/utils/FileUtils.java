package com.gigaspaces.newman.utils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    public static Path append(Path path, String subfolder) {
        return path.resolve(subfolder);
    }

    public static Path append(String path, String subfolder) {
        return Paths.get(path, subfolder);
    }

    public static Path createFolder(Path folder) throws IOException {
        return Files.createDirectories(folder);
    }

    public static void delete(Path path) throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(path.toFile());
    }

    public static void clearDirectory(Path path) throws IOException {
        org.apache.commons.io.FileUtils.cleanDirectory(path.toFile());
    }

    public static String readTextFile(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    public static String readTextFile(InputStream is) throws IOException {
        return org.apache.commons.io.IOUtils.toString(is);
    }

    public static List readTextFileLines(InputStream is) throws IOException {
        return org.apache.commons.io.IOUtils.readLines(is);
    }

    public static void download(URL source, Path target) throws IOException {
        if (source.getProtocol().equalsIgnoreCase("https")) {
            try {
                downloadSSL(source, target);
            }
            catch (Exception e) {
                throw new IOException(e);
            }
            return;
        }
        try (ReadableByteChannel rbc = Channels.newChannel(source.openStream())) {
            String sourcePath = source.getPath();
            String filename = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
            Path output = append(target, filename);
            try (FileOutputStream fos = new FileOutputStream(output.toFile())) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
        }catch(Exception e){
            logger.error(e.toString(), e);
            throw e;
        }
    }

    private static HttpsURLConnection getHttpsConnection(URL url, SSLContext sc) throws NoSuchAlgorithmException, KeyManagementException, IOException {
        TrustManager[] trustAllCerts = getMockTrustManagers();
        HttpsURLConnection connection;
        try {
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            connection = (HttpsURLConnection) url.openConnection();
        }
        catch (Exception e){
            logger.error("failed to connect to url");
            throw e;
        }
        return connection;
    }

    private static void downloadSSL(URL url, Path target) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        HttpsURLConnection connection = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            connection = getHttpsConnection(url, sc);
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setSSLSocketFactory(sc.getSocketFactory());
            String sourcePath = url.getPath();
            String filename = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
            Path output = append(target, filename);
            try (InputStream in = connection.getInputStream();
                 OutputStream out = new FileOutputStream(output.toFile())) {
                byte[] buff = new byte[1024 * 5];
                int read;
                while ((read = in.read(buff)) != -1) {
                    out.write(buff, 0, read);
                }
            }
        }
        catch (Exception e) {
            logger.error(e.toString(), e);
            throw e;
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static TrustManager[] getMockTrustManagers() {
        return new TrustManager[] { new X509ExtendedTrustManager() {

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {

                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws java.security.cert.CertificateException {

                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s, Socket socket) throws java.security.cert.CertificateException {

                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s, Socket socket) throws java.security.cert.CertificateException {

                }

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws java.security.cert.CertificateException {

                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws java.security.cert.CertificateException {

                }
            } };
    }

    public static void unzip(Path source, Path targetFolder) throws IOException {
        try {
            ZipFile zipFile = new ZipFile(source.toFile());
            zipFile.extractAll(targetFolder.toString());
        } catch (ZipException e) {
            throw new IOException("Failed to unzip " + source + " into " + targetFolder, e);
        }
    }

    public static void zip(Collection<File> fileList, Path targetZipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(targetZipFile.toFile()); ZipOutputStream zos = new ZipOutputStream(fos)){
            for (File file : fileList) {
                if (!file.isDirectory()) { // we only zip files, not directories
                    addToZip(file, zos);
                }
            }
        }
    }

    private static void addToZip(File file, ZipOutputStream zos) throws IOException {

        FileInputStream fis = new FileInputStream(file);

        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String zipFilePath = file.getName();
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }


    public static void copyFile(Path source, Path target) throws IOException {
        org.apache.commons.io.FileUtils.copyFile(source.toFile(), target.toFile());
    }

    public static Collection listFilesInFolder(final File folder) {
        return org.apache.commons.io.FileUtils.listFiles(folder, null, true);
    }

    public static Collection listFilesInFolder(final File folder, String[] extensions) {
        return org.apache.commons.io.FileUtils.listFiles(folder, extensions, true);
    }

    public static void validateUris(Collection<URI> resources) throws IOException {
        for (URI uri : resources) {
            InputStream inputStream = null;
            HttpsURLConnection connection = null;
            try {
                if (uri.toURL().getProtocol().equalsIgnoreCase("https")){
                    connection = getHttpsConnection(uri.toURL(), SSLContext.getInstance("TLS"));
                    inputStream = connection.getInputStream();
                    return;
                }
                inputStream = uri.toURL().openStream();
                logger.info("able to connect to URI: " + uri);
            }
            catch (Exception e){
                logger.error("can't connect URI: " + uri, e);
                throw new IOException(e);
            }
            finally {
                if(inputStream != null){
                    inputStream.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        final Path target = Paths.get("testResource");
        createFolder(target);
        download(URI.create("https://192.168.11.141:8443/api/newman/resource/master/14708-405/gigaspaces-xap-premium-11.0.0-m8-b14708-405.zip").toURL(), target);
        download(URI.create("https://192.168.11.141:8443/api/newman/metadata/master/14708-405/gigaspaces-xap-premium-11.0.0-m8-b14708-405.zip").toURL(), target);
    }

}
