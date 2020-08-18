package com.AnalyzeThis;

import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.HarReaderException;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarLog;
import de.sstoehr.harreader.model.HarPageTiming;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MagicAnalyzer {

    private String driverPath = System.getProperty("user.dir" ) + "/src/main/resources/browserDrivers/windowschromedriver.exe";
    private String sFileName = System.getProperty("user.dir" ) + "/src/main/resources/DataProvider/CurrentHAR.har";
    private int anomalyTime = 1000;
    private int pagesToAnalyze = 5;
    private WebDriver driver;
    private BrowserMobProxy proxy;
    private boolean failed = false;

    private String scriptString = "var performance = window.performance || window.mozPerformance || window.msPerformance || window.webkitPerformance || {}; var network = performance.getEntries() || {}; return network;";
    public void setUp() throws UnknownHostException {
        try {
            // start the proxy
            proxy = new BrowserMobProxyServer();
            proxy.enableHarCaptureTypes(CaptureType.getAllContentCaptureTypes());
            proxy.setTrustAllServers(true);
            proxy.setMitmDisabled(false);
            proxy.start(0);

            //get the Selenium proxy object - org.openqa.selenium.Proxy;
            Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
            String hostIp = Inet4Address.getLocalHost().getHostAddress();
            seleniumProxy.setHttpProxy(hostIp + ":" + proxy.getPort());
            seleniumProxy.setSslProxy(hostIp + ":" + proxy.getPort());
            // configure it as a desired capability
            DesiredCapabilities capabilities = new DesiredCapabilities().chrome();
            capabilities.setCapability("applicationCacheEnabled", false);
            LoggingPreferences logPrefs = new LoggingPreferences();
            logPrefs.enable(LogType.PERFORMANCE, Level.ALL);
            logPrefs.enable(LogType.CLIENT, Level.ALL);
            logPrefs.enable(LogType.BROWSER, Level.ALL);
            capabilities.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);

            capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--incognito --media-cache-size=1 --disk-cache-size=1");
            options.addArguments("disable-infobars");
            options.addArguments("start-maximized");
            capabilities.setCapability(ChromeOptions.CAPABILITY, options);

            //set chromedriver system property
            System.setProperty("webdriver.chrome.driver", driverPath);
            options.setProxy(seleniumProxy);
            driver = new ChromeDriver(capabilities);

            // enable more detailed HAR capture
            proxy.enableHarCaptureTypes(CaptureType.getAllContentCaptureTypes());

            failed = false;
        }
        catch (Exception e){failed = true;}


    }

    public void doMagic() throws HarReaderException, IOException, InterruptedException {
        int totalNumberOfPages = pagesToAnalyze;
        String fileReadPath = "pageList";
        String fileResultPath = "Result";
        String fileErrorPath = "Errors";
        String[] resultData = new String[totalNumberOfPages + 2];
        String[] errorData = new String[totalNumberOfPages + 2];
        try {
            String[][] pageList = ReadWriteHelper.readCSVFile(fileReadPath, totalNumberOfPages);
            resultData[0] = "URL, DOMInteractive, Load, FirstRequests, FirstTransferred, Finish, FinalRequests, FinalTransferred\n";
            for (int i = 0; i < totalNumberOfPages; i++) {
                setUp();
                if (!failed) {
                    driver.manage().deleteAllCookies();
                    String savePath = System.getProperty( "user.dir" ) + "/src/main/resources/DataProvider/currentHar.har";
                    proxy.newHar(savePath);
                    driver.get(pageList[i][0]);
                    Thread.sleep(1000);
                    int oldNumRequests = getFinalNumberRequests();
                    double fileSize = getFinalPageSize();
                    double domInteractive = getFinalDomInteractive();
                    double finishTime = getFinalFinishTime();
                    double loadedTime = getFinalLoadedTime();

                    if (domInteractive >= anomalyTime || loadedTime >= anomalyTime) {
                        driver.get(pageList[i][0]);
                        Thread.sleep(1000);
                        oldNumRequests = getFinalNumberRequests();
                        fileSize = getFinalPageSize();
                        domInteractive = getFinalDomInteractive();
                        finishTime = getFinalFinishTime();
                        loadedTime = getFinalLoadedTime();
                    }

                    try {
                        long lastHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
                        while (true) {
                            for (int j = 1; j <= 100; j++) {
                                //((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight/" + j + ");");
                                Actions builder = new Actions(driver);
                                builder.sendKeys(Keys.DOWN);
                                builder.perform();
                                Thread.sleep(50);
                            }
                            long newHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
                            if (newHeight == lastHeight) {
                                break;
                            }
                            lastHeight = newHeight;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    resultData[i + 1] = pageList[i][0] + "," +
                            Double.toString(domInteractive) + "," +
                            Double.toString(loadedTime) + "," +
                            Double.toString(oldNumRequests) + "," +
                            Double.toString(fileSize) + "," +
                            Double.toString(finishTime) + "," +
                            Double.toString(getFinalNumberRequests()) + "," +
                            Double.toString(getFinalPageSize());
                    errorData[i + 1] = pageList[i][0] + ", Passed";
                    System.out.println(resultData[i + 1]);
                    tearDown();
                } else {
                    resultData[i + 1] = pageList[i][0] + ", AN ERROR OCCURED HERE!, THIS PAGE NEEDS TO BE RETESTED";
                    errorData[i + 1] = pageList[i] + ", THIS PAGE DID NOT WORK!";
                }
            }
            for (String result : resultData) {
                System.out.println(result);
            }
        }
        finally {
            ReadWriteHelper.writeCSVFile(fileResultPath, resultData);
            ReadWriteHelper.writeCSVFile(fileErrorPath, errorData);
        }


    }
    public void tearDown() {
        if (driver != null) {
            proxy.stop();
            driver.quit();
        }
    }

    public void saveHarFile(String path){
        Har har = proxy.getHar();
        // Write HAR Data in a File
        File harFile = new File(path);
        try {
            har.writeTo(harFile);
        } catch (IOException ex) {
            System.out.println (ex.toString());
            System.out.println("Could not find file " + sFileName);
        }
    }

    public double getFileSize(String filePath){
        File file =new File(filePath);
        double bytes = file.length();
        return (bytes / 1024);
    }

    public int getOnLoad(String filepath) throws HarReaderException {
        HarReader harReader = new HarReader();
        de.sstoehr.harreader.model.Har har = harReader.readFromFile(new File(filepath));

        HarLog log = har.getLog();
        HarPageTiming myTest = log.getPages().get(0).getPageTimings();
        return myTest.getOnLoad().intValue();

    }

    public int getContentLoad(String filepath) throws HarReaderException {
        HarReader harReader = new HarReader();
        de.sstoehr.harreader.model.Har har = harReader.readFromFile(new File(filepath));

        HarLog log = har.getLog();
        HarPageTiming myTest = log.getPages().get(0).getPageTimings();
        return myTest.getOnContentLoad().intValue();

    }

    public double calculatePageLoadTime(String filename) throws HarReaderException {
        HarReader harReader = new HarReader();
        de.sstoehr.harreader.model.Har har = harReader.readFromFile(new File(filename));
        HarLog log = har.getLog();
        // Access all pages elements as an object
        long startTime =   log.getPages().get(0).getStartedDateTime().getTime();
        // Access all entries elements as an object
        List<HarEntry> hentry = log.getEntries();
        long loadTime = 0;
        int entryIndex = 0;
        //Output "response" code of entries.
        for (HarEntry entry : hentry)
        {
            long entryLoadTime = entry.getStartedDateTime().getTime() + entry.getTime();
            if(entryLoadTime > loadTime){
                loadTime = entryLoadTime;
            }
            entryIndex++;
        }
        long loadTimeSpan = loadTime - startTime;
        double webLoadTime = ((double)loadTimeSpan) / 1000;
        return Math.round(webLoadTime * 100.0) / 100.0;
    }

    public int getNumberRequests(String filename) throws HarReaderException {
        HarReader harReader = new HarReader();
        de.sstoehr.harreader.model.Har har = harReader.readFromFile(new File(filename));

        HarLog log = har.getLog();
        return log.getEntries().size();
    }
    private int getFinalPageSize(){
        String netData = ((JavascriptExecutor)driver).executeScript(scriptString).toString();
        Matcher dataLengthMatcher1 = Pattern.compile("transferSize=(.*?),").matcher(netData);
        int sumEncoded = 0;
        while (dataLengthMatcher1.find()){
            //System.out.println("File size: " + dataLengthMatcher1.group(1));
            sumEncoded = sumEncoded + Integer.parseInt(dataLengthMatcher1.group(1));
        }
        return sumEncoded;
    }

    private double getFinalDomInteractive(){
        String netData = ((JavascriptExecutor)driver).executeScript(scriptString).toString();
        Matcher domInteractive = Pattern.compile("domInteractive=(.*?),").matcher(netData);
        double amountDom = 0.0;
        while (domInteractive.find()){
            //System.out.println("DomInteractive: " + domInteractive.group(1));
            if (Double.parseDouble(domInteractive.group(1)) >= amountDom){
                amountDom = Double.parseDouble(domInteractive.group(1));
            }
        }
        return amountDom;
    }
    private double getFinalLoadedTime(){
        String netData = ((JavascriptExecutor)driver).executeScript(scriptString).toString();
        Matcher loadEventEnd = Pattern.compile("loadEventEnd=(.*?),").matcher(netData);
        double loadEventTime = 0.0;
        while (loadEventEnd.find()){
           // System.out.println("LoadEventEnd: " + loadEventEnd.group(1));
            if (Double.parseDouble(loadEventEnd.group(1)) >= loadEventTime){
                loadEventTime = Double.parseDouble(loadEventEnd.group(1));
            }
        }
        return loadEventTime;
    }

    private double getFinalFinishTime(){
        String netData = ((JavascriptExecutor)driver).executeScript(scriptString).toString();
        Matcher maxResponse = Pattern.compile("responseEnd=(.*?),").matcher(netData);
        double makResponseTime = 0.0;
        while (maxResponse.find()){
            //System.out.println("MaxResponse: " + maxResponse.group(1));
            if (Double.parseDouble(maxResponse.group(1)) >= makResponseTime){
                makResponseTime = Double.parseDouble(maxResponse.group(1));
            }
        }
        return makResponseTime;
    }
    private int getFinalNumberRequests(){
        String netData = ((JavascriptExecutor)driver).executeScript(scriptString).toString();
        Matcher maxResponse = Pattern.compile("transferSize=(.*?),").matcher(netData);
        int numRequests = 0;
        while (maxResponse.find()){
            numRequests++;
        }
        return numRequests;
    }
    public int getFinalTransferSize(){
        String netData = ((JavascriptExecutor)driver).executeScript(scriptString).toString();
        Matcher dataLengthMatcher2 = Pattern.compile("encodedBodySize=(.*?),").matcher(netData);
        int sumEncoded2 = 0;
        while (dataLengthMatcher2.find()){
            sumEncoded2 = sumEncoded2 + Integer.parseInt(dataLengthMatcher2.group(1));
        }
        return sumEncoded2;
    }

    public int getFinalTransferSize2(){
        int totalBytes = 0;
        for (LogEntry entry : driver.manage().logs().get(LogType.PERFORMANCE)) {
            if (entry.getMessage().contains("Network.dataReceived")) {
                Matcher dataLengthMatcher = Pattern.compile("dataLength\":(.*?),").matcher(entry.getMessage());
                dataLengthMatcher.find();
                totalBytes = totalBytes + Integer.parseInt(dataLengthMatcher.group(1));
                //Do whatever you want with the data here.
            }
        }
        return totalBytes;
    }


    private String fileToString(String filepath) throws IOException {
        InputStream is = new FileInputStream(filepath);
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));
        String line = buf.readLine(); StringBuilder sb = new StringBuilder();
        while(line != null){
            sb.append(line).append("\n"); line = buf.readLine();
        }
        return sb.toString();
    }

    public int getTransferSizeString (String filepath) throws IOException {
        String fileString = fileToString(filepath);
        int totalSize = 0;
        Matcher dataLengthMatcher2 = Pattern.compile("\"transferSize\": (.*?),").matcher(fileString);
        while (dataLengthMatcher2.find()){
            //System.out.println("Transfer size: " + dataLengthMatcher2.group(1));
            totalSize = totalSize + Integer.parseInt(dataLengthMatcher2.group(1));
        }
        return totalSize;
    }

    public int getTotalRequests (String filepath) throws IOException {
        String fileString = fileToString(filepath);
        int totalSize = 0;
        Matcher dataLengthMatcher2 = Pattern.compile("\"transferSize\": (.*?),").matcher(fileString);
        while (dataLengthMatcher2.find()){
            //System.out.println("Transfer size: " + dataLengthMatcher2.group(1));
            totalSize++;
        }
        return totalSize;
    }
}
