package com.gigaspaces.newman.util;

import com.sun.deploy.util.SystemUtils;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Created by boris on 4/21/2015.
 */
public class FileUtilities {

    private final static String OS = System.getProperty("os.name").toLowerCase();

    public static void download(Logger logger, URI uri, String dirLocation, String destFileName) {
        makeDir(dirLocation);
        String targetFile = dirLocation + File.separator + destFileName;
        File destFile = new File(targetFile);
        logger.info("Downloading file: {} into: {}",uri, targetFile);
        try {
            FileUtils.copyURLToFile(uri.toURL(), destFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void unzip(Logger logger, String source, String destination){
        try {
            ZipFile zipFile = new ZipFile(source);
            logger.info("Extracting file: {} into directory: {}", source, destination);
            zipFile.extractAll(destination);
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    public static File makeDir(String dirLocation){
        File file = new File(dirLocation);
        file.mkdirs();
        return file;
    }

    /**
     * removes file or directory recursively
     * @param location
     */
    public static void rmFile(String location){
        try {
            FileUtils.forceDelete(new File(location));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    /*public static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }*/

    public static boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
    }

    /*public static boolean isSolaris() {
        return (OS.indexOf("sunos") >= 0);
    }*/

    public static String getScriptSuffix(){
        if (isWindows()) {
            return ".bat";
        } else if (isUnix()) {
            return ".sh";
        } else {
            throw new IllegalStateException("unknown script suffix");
        }
    }


}
