package com.slonigiraf.btrxdump.downloader;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CallsDownloaderTest {

    @DataProvider(name = "testDateStringForFileName")
    public static Object[][] dateStringForFileName() {
        return new Object[][] {
                {"https://cdn.bitrix24.ru/b1243275/voximplant/e08/e088db9991de290687c4ddc21e552d82/1163a8ab029ccb9c414ba7168a002eff.mp3", "noDate"},
                {"https://sig.bitrix24.ru/disk/downloadFile/126319/?&ncc=1&filename=2018-09-06_17-58-41__79169773734.mp3", "2018-09-06"},
                {"http://www-ru-27-22.voximplant.com/logs/2018/09/06/145840_8a2898d3f7e07325.1536245920.821616_185.164.149.22.log", "2018-09-06"}};
    }

    @Test(dataProvider = "testDateStringForFileName")
    public void testDateStringForFileName(String fileName, String expectedResult) {

        Assert.assertEquals(CallsDownloader.getSubfolderForFileUrl(fileName), expectedResult);
    }

    @DataProvider(name = "testFileNameFromUrl")
    public static Object[][] fileNameFromUrl() {
        return new Object[][] {
                {"https://cdn.bitrix24.ru/b1243275/voximplant/e08/e088db9991de290687c4ddc21e552d82/1163a8ab029ccb9c414ba7168a002eff.mp3", "1163a8ab029ccb9c414ba7168a002eff.mp3"},
                {"https://sig.bitrix24.ru/disk/downloadFile/126319/?&ncc=1&filename=2018-09-06_17-58-41__79169773734.mp3", "2018-09-06_17-58-41__79169773734.mp3"},
                {"http://www-ru-27-22.voximplant.com/logs/2018/09/06/145840_8a2898d3f7e07325.1536245920.821616_185.164.149.22.log", "145840_8a2898d3f7e07325.1536245920.821616_185.164.149.22.log"}};
    }

    @Test(dataProvider = "testFileNameFromUrl")
    public void testFileNameFromUrl(String fileName, String expectedResult) {
        Assert.assertEquals(CallsDownloader.getFileNameFromUrl(fileName), expectedResult);
    }


}