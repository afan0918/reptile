package afan.thread;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class reptileThread extends Thread{
    private int page;
    private String url;
    private int jpgIndex;

    public void setURL(String url,int jpgIndex,int page){
        this.url=url;
        this.jpgIndex=jpgIndex;
        this.page=page;
        System.out.println(url);
        System.out.println(jpgIndex);
        System.out.println(page);
    }

    @Override
    public void run() {
        //把解析取得的圖片URL拿來做下載動作，可自行調整下載位置
        String fileName_https = downloadImageFromUrl(url+jpgIndex+".jpg", "G:\\comicbook" + File.separator, String.valueOf(page));
        System.out.println(fileName_https);
    }

    private static String downloadImageFromUrl(String url, String fileDirectoryPath, String fileNameWithoutFormat) {
        String filePath = null;
        BufferedInputStream in = null;
        ByteArrayOutputStream out = null;
        HttpURLConnection httpUrlConnection = null;
        FileOutputStream file = null;

        try {
            if (url.startsWith("https://")) {
                //HTTPS時
                httpUrlConnection = getHttpURLConnectionFromHttps(url);
            }
            //如果不是HTTPS或是沒成功得到httpUrlConnection，用HTTP的方法
            if(httpUrlConnection == null) {
                httpUrlConnection = (HttpURLConnection) (new URL(url)).openConnection();
            }

            // 設置User-Agent，偽裝成一般瀏覽器，不然有些伺服器會擋掉機器程式請求
            httpUrlConnection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 11; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85");
            httpUrlConnection.connect();

            String imageType;
            if (httpUrlConnection.getResponseCode() == 200) {//成功取得response
                //取得contentType
                String contentType = httpUrlConnection.getHeaderField("Content-Type");
                // 只處理image的回應
                if ("image".equals(contentType.substring(0, contentType.indexOf("/")))) {
                    //得到對方Server提供的圖片副檔名，如jpg, png等
                    imageType = contentType.substring(contentType.indexOf("/") + 1);

                    if (!"".equals(imageType)) {
                        //由HttpUrlConnection取得輸入串流
                        in = new BufferedInputStream(httpUrlConnection.getInputStream());
                        out = new ByteArrayOutputStream();

                        //建立串流Buffer
                        byte[] buffer = new byte[1024];

                        file = new FileOutputStream(fileDirectoryPath + File.separator + fileNameWithoutFormat + "." + imageType);

                        int readByte;
                        while ((readByte = in.read(buffer)) != -1) {
                            //輸出檔案
                            out.write(buffer, 0, readByte);
                        }

                        byte[] response = out.toByteArray();
                        file.write(response);

                        //下載成功後，返回檔案路徑
                        filePath = fileDirectoryPath + File.separator + fileNameWithoutFormat + "." + imageType;
                    }
                }
            }
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            //關閉各種串流
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (httpUrlConnection != null) {
                    httpUrlConnection.disconnect();
                }
                if (file != null) {
                    file.close();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filePath;
    }

    public static HttpURLConnection getHttpURLConnectionFromHttps(String url) throws KeyManagementException, NoSuchAlgorithmException, IOException {
        HttpURLConnection httpUrlConnection = null;

        //建立一個信任所有憑證的X509TrustManager，放到TrustManager裡面
        TrustManager[] trustAllCerts;

        // Activate the new trust manager
        trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {     // TODO Auto-generated method stub
                //不作任何事
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {     // TODO Auto-generated method stub
                //不作任何事
            }

            public X509Certificate[] getAcceptedIssuers() {
                //不作任何事
                return null;
            }
        }};

        //設置SSL設定
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        //跟HTTP一樣，用Url建立連線
        httpUrlConnection = (HttpURLConnection) (new URL(url)).openConnection();

        return httpUrlConnection;
    }
}
