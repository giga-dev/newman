package com.gigaspaces.newman.utils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;


public class FileUtils {

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

    public static void zip(Path sourceFolder, Path targetZipFile) throws IOException {
        try {
            ZipFile zipFile = new ZipFile(targetZipFile.toFile());
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setIncludeRootFolder(false);
            zipFile.addFolder(sourceFolder.toFile(), zipParameters);
        } catch (ZipException e) {
            throw new IOException("Failed to zip " + sourceFolder + " into " + targetZipFile, e);
        }
    }

    public static void unzipFileFromZip(String fileName,Path targetFolder, Path inputZipFile) throws IOException {
        try {
            ZipFile zipFile = new ZipFile(inputZipFile.toFile());
            zipFile.extractFile(fileName, targetFolder.toString());
        } catch (ZipException e) {
            throw new IOException("Failed to unzip the file:" + fileName + "from the zip:" + inputZipFile + " into " + targetFolder, e);
        }
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
