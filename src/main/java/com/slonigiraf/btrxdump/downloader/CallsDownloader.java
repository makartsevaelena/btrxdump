package com.slonigiraf.btrxdump.downloader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallsDownloader {
    static Properties properties;
    static String htmlDirectory;

    public static void main(String[] args) throws FileNotFoundException {
        System.setOut(new PrintStream(new FileOutputStream("log_file.txt")));
        System.setErr(new PrintStream(new FileOutputStream("err_file.txt")));
        long browsersCount = 10;

        properties = getProperties();
        System.setProperty("webdriver.chrome.driver",
                "/Users/adr/programming/selenium/chromedriver");

        cleanTargetDirectory();
        htmlDirectory = createDirectories();
        int lastCallPageIndex = Integer.parseInt(properties.getProperty("lastCallPageIndex"));


        ExecutorService executor = Executors.newCachedThreadPool();

        long pagesPerBrowserCount = lastCallPageIndex / browsersCount;

        for (int pageIndex = 1; pageIndex <= lastCallPageIndex; pageIndex += pagesPerBrowserCount) {
            long lastCurrentIndex = pageIndex + pagesPerBrowserCount - 1;
            if (lastCurrentIndex > lastCallPageIndex) {
                lastCurrentIndex = lastCallPageIndex;
            }
            OneStreamCallsDownloader oneStreamDownloader = new OneStreamCallsDownloader(properties, pageIndex, lastCurrentIndex);
            executor.execute(oneStreamDownloader);
        }
    }

    static Properties getProperties() {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream("/Users/adr/programming/nicgm_btrx.conf");
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }

    static String createDirectories() {
        String folder = String.format("%s/%s", properties.getProperty("btrxData"), "html");
        if (!isFolderExist(folder)) {
            try {
                Files.createDirectories(Paths.get(folder));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return folder;
    }

    static Pattern LOG_PATTERN = Pattern.compile(".*logs/(\\d+/\\d+/\\d+)/.*");
    static Pattern MP3_PATTERN = Pattern.compile(".*filename=(\\d+-\\d+-\\d+)_.*");
    static Pattern[] patterns = {LOG_PATTERN, MP3_PATTERN};

    static String getSubfolderForFileUrl(String fileName) {
        String result = "noDate";
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(fileName);
            while (matcher.find()) {
                result = matcher.group(1);
            }
        }
        result = result.replaceAll("/", "-");
        return result;
    }

    static String getFolderNameForFileUrl(String url) {
        String subFolder = getSubfolderForFileUrl(url);
        String type = "unknown";
        String fileName = getFileNameFromUrl(url);
        String[] nameExtension = fileName.split("\\.");
        if (nameExtension.length >= 2) {
            type = nameExtension[nameExtension.length - 1];
        }
        String result = String.format("%s/%s/%s", properties.getProperty("btrxData"), type, subFolder);
        return result;
    }

    static boolean isFolderExist(String name) {
        File directory = new File(name);
        return directory.isDirectory();
    }


    static Pattern DEFAULT_NAME_PATTERN = Pattern.compile(".*/(.+?)$");
    static Pattern MP3_NAME_PATTERN = Pattern.compile(".*filename=(.+)$");
    static Pattern[] namePatterns = {DEFAULT_NAME_PATTERN, MP3_NAME_PATTERN};

    static String getFileNameFromUrl(String url) {
        String result = "unknown";
        for (Pattern pattern : namePatterns) {
            Matcher matcher = pattern.matcher(url);
            while (matcher.find()) {
                result = matcher.group(1);
            }
        }
        if(result.equals("unknown")){
            System.err.println("PROBLEM PARSING URL FOR FILE NAME: "+url);
        }
        return result;
    }

    static void cleanTargetDirectory() {
        try {
            deleteRecursive(new File(properties.getProperty("btrxData")));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static void deleteRecursive(File f) throws Exception {
        try {
            if (f.isDirectory()) {
                for (File c : f.listFiles()) {
                    deleteRecursive(c);
                }
            }
            if (!f.delete()) {
                throw new Exception("Delete command returned false for file: " + f);
            }
        } catch (Exception e) {
            throw new Exception("Failed to delete the folder: " + f, e);
        }
    }

    public static String getHtmlDirectory() {
        return htmlDirectory;
    }
}
