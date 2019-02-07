package com.slonigiraf.btrxdump.downloader;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class OneStreamCallsDownloader implements Runnable {
    Properties properties;
    WebDriver driver;
    long firstPageIndex;
    long lastPageIndex;
    long mp3Files = 0;
    long mp3Urls = 0;
    long logFiles = 0;
    long logUrls = 0;

    public OneStreamCallsDownloader(Properties properties, long firstPageIndex, long lastPageIndex) {
        this.properties = properties;
        this.firstPageIndex = firstPageIndex;
        this.lastPageIndex = lastPageIndex;
    }

    void getCallListPage(long pageIndex, int trialNumber) {
        String url = String.format("https://%s/telephony/detail.php?internal=true&grid_id=voximplant_statistic_detail&grid_action=pagination&page=page-%d",
                properties.getProperty("domain"), pageIndex);
        System.out.println(String.format("TRIAL_LEFT: %s, PAGE: %s", trialNumber, url));

        driver.get(url);
        if (driver.getPageSource().contains("stream")) {
            getCallFiles();
            Path outFile = Paths.get(String.format("%s/%d.html",
                    CallsDownloader.getHtmlDirectory(), pageIndex));
            try (BufferedWriter writer = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
                writer.write(driver.getPageSource());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            getCallListPage(pageIndex, --trialNumber);
        }
    }


    public void saveUrl(final Path file, final URL url,
                        int secsConnectTimeout, int secsReadTimeout, boolean sendCookies) {
        try (
                ReadableByteChannel rbc = Channels.newChannel(
                        streamFromUrl(url, secsConnectTimeout, secsReadTimeout, sendCookies, 100)
                );
                FileChannel channel = FileChannel.open(file,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)
        ) {
            channel.transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (Exception e) {
            System.err.println("ERROR reading: " + url);
        }
    }


    public InputStream streamFromUrl(URL url, int secsConnectTimeout, int secsReadTimeout, boolean sendCookies, int trialNumber) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (sendCookies) {
            Set<Cookie> cookies = driver.manage().getCookies();
            if (cookies != null) {
                StringBuilder cookiesString = new StringBuilder();
                for (Cookie cookie : cookies) {
                    cookiesString.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
                }
                conn.setRequestProperty("Cookie", cookiesString.toString());
            }
        }
        if (secsConnectTimeout > 0) conn.setConnectTimeout(secsConnectTimeout * 1000);
        if (secsReadTimeout > 0) conn.setReadTimeout(secsReadTimeout * 1000);
        if (conn.getResponseCode() == 503 && trialNumber > 0) {
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.err.println(String.format("LEFT_TRIAL: %d, responce 503 on reading: %s", trialNumber, url));
            return streamFromUrl(url, secsConnectTimeout, secsReadTimeout, sendCookies, --trialNumber);
        }
        return conn.getInputStream();
    }

    void getCallFiles() {
        List<WebElement> mp3Elements = driver.findElements(By.tagName("a"));
        List<String> hrefs = new ArrayList<>();
        for (WebElement element : mp3Elements) {
            try {
                hrefs.add(element.getAttribute("href"));
            } catch (org.openqa.selenium.StaleElementReferenceException e) {
                System.err.println("StaleElementReferenceException at: " + element);
            }
        }
        hrefs.stream().
                distinct().
                filter(url -> url != null).
                forEach(
                        url -> {
                            if (url.matches(".*\\.log$")) {
                                logUrls++;
                                if (isURLValid(url, false)) {
                                    logFiles++;
                                    saveCallFile(url);
                                }
                            } else if (url.matches(".*\\.mp3$")) {
                                mp3Urls++;
                                mp3Files++;
                                saveCallFile(url);
                            }

                        }
                );
    }

    void saveCallFile(String url) {
        String folder = CallsDownloader.getFolderNameForFileUrl(url);
        if (!CallsDownloader.isFolderExist(folder)) {
            try {
                Files.createDirectories(Paths.get(folder));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String fileName = CallsDownloader.getFileNameFromUrl(url);
        String filePath = String.format("%s/%s", folder, fileName);
        boolean sendCookies = false;
        if (url.matches(".*\\.mp3$")) {
            sendCookies = true;
        }
        try {
            saveUrl(Paths.get(filePath), new URL(url), 10, 10, sendCookies);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void login() {
        String url = String.format("http://%s", properties.getProperty("domain"));
        driver.get(url);
        driver.findElement(By.name("USER_LOGIN")).sendKeys(properties.getProperty("username"));
        driver.findElement(By.name("USER_PASSWORD")).sendKeys(properties.getProperty("password"));
        driver.findElement(By.id("AUTH_SUBMIT")).submit();
    }


    public boolean isURLValid(String URL, boolean sendCookies) {
        boolean valid = false;
        try {
            java.net.URL url = new URL(URL);
            HttpURLConnection httpURLConnect = (HttpURLConnection) url.openConnection();
            if (sendCookies) {
                Set<Cookie> cookies = driver.manage().getCookies();
                if (cookies != null) {
                    StringBuilder cookiesString = new StringBuilder();
                    for (Cookie cookie : cookies) {
                        cookiesString.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
                    }
                    httpURLConnect.setRequestProperty("Cookie", cookiesString.toString());
                }
            }

            httpURLConnect.setConnectTimeout(3000);
            httpURLConnect.connect();
            if (httpURLConnect.getResponseCode() == 200) {
                valid = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return valid;
    }


    @Override
    public void run() {
        driver = new ChromeDriver();
//        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        login();
        for (long pageIndex = firstPageIndex; pageIndex <= lastPageIndex; pageIndex++) {
            getCallListPage(pageIndex, 100);
        }
        driver.close();
        driver.quit();
        System.out.println(
                String.format(
                        "FROM:\t%d\tTO:\t%d\tLOG_URLS:\t%d\tLOG_FILES:\t%d\tMP3_URLS:\t%d\tMP3_FILES:\t%d",
                        firstPageIndex, lastPageIndex, logUrls, logFiles, mp3Urls, mp3Files)
        );
    }
}
