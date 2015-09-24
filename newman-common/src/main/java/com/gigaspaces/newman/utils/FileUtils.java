package com.gigaspaces.newman.utils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        /*if (Files.isDirectory(path)) {
            for (Path f : Files.newDirectoryStream(path))
                delete(f);
        }
        Files.delete(path);*/
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

    public static Path download(URL source, Path target) throws IOException {
        try (ReadableByteChannel rbc = Channels.newChannel(source.openStream())) {
            String sourcePath = source.getPath();
            String filename = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
            Path output = append(target, filename);
            try (FileOutputStream fos = new FileOutputStream(output.toFile())) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
            return output;
        }catch(Exception e){
            logger.error(e.toString(), e);
            return null;
        }
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

}
