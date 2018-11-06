package eu.prunet.util;

import eu.prunet.security.rhelchecker.DownloadException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SimpleDownloader {



    public static void downloadWithRetry(Path targetFile, String sUrl, int retry, int redirect, Proxy proxy) throws DownloadException {
        if (redirect == 0 || retry == 0) {
            throw new DownloadException("The file cannot be downloaded " + sUrl);
        }
        HttpURLConnection conn = null;
        InputStream is = null;
        Path tmpFile = null;
        try {
            URL url = new URL(sUrl);
            conn =  (HttpURLConnection) (proxy == null ?url.openConnection() : url.openConnection(proxy));
            conn.setReadTimeout(5000);
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1");
            conn.addRequestProperty("Referer", "google.com");
            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    String newUrl = conn.getHeaderField("Location");
                    downloadWithRetry(targetFile, newUrl, retry, redirect - 1,proxy );
                }
                downloadWithRetry(targetFile, sUrl, retry-1, redirect,proxy);
            } else {
                tmpFile = Files.createTempFile(targetFile.getParent(), ".", ".tmp");
                is = conn.getInputStream();
                System.err.println("Downloading " + sUrl + "...");
                Files.copy(is, tmpFile, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(tmpFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                System.err.println(sUrl + " downloaded successfully");
            }
        } catch (MalformedURLException ex) {
            System.err.println("Impossible to download " + sUrl);
        } catch (IOException ex) {
            System.err.println("An IO Error orccured during download of " + sUrl + " retry");
            downloadWithRetry(targetFile, sUrl, retry-1, redirect, proxy);
        } finally {
            closeQuietly(is);
            closeQuietly(conn);
            deleteQuietly(tmpFile);
        }

    }

    static void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception ignore) {
            }
        }
    }
    static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignore) {
            }
        }
    }
    static void closeQuietly(HttpURLConnection c) {
        if (c != null) {
            try {
                c.disconnect();
            } catch (Exception ignore) {
            }
        }
    }


}
