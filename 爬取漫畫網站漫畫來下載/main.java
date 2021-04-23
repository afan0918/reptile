import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

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
import java.util.Scanner;

public class main {
    public static void main(String[] args) {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);//建立一個模擬器模擬chrome

        webClient.getOptions().setThrowExceptionOnScriptError(false);//選擇js渲染出錯的話是否拋出異常
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);//選擇http通訊端口非200時是否丟出異常
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setCssEnabled(false);//選擇是否啟用CSS
        webClient.getOptions().setJavaScriptEnabled(true); //啟用js渲染網頁(才能爬取動態網頁資料，但時間會長很多)
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());//很重要，選擇支持AJAX

        System.out.println("請輸入要爬取的漫畫所在集數URL");
        Scanner scanner=new Scanner(System.in);
        String reptileURL=scanner.next();

        for(int index=0;true;++index) {
            HtmlPage page;
            try {
                page = webClient.getPage(reptileURL + "index_" +index+".html");//載入網頁
            } catch (Exception e) {
                e.printStackTrace();
                break;//拿不到就直接認定本集結束了
            }

            //等待js渲染結束
            webClient.waitForBackgroundJavaScript(20000);

            //將加載完的頁面轉換成xml格式
            assert page != null;
            String pageXml = page.asXml();

            //把需要的資料硬拉出來
            int left = pageXml.indexOf("var mhurl1=\"");
            int right = pageXml.indexOf("\";mhpicurl=");
            StringBuilder jpg = new StringBuilder();
            for (int i = left + 12; i < right; i++) {
                jpg.append(pageXml.charAt(i));
            }
            System.out.println(jpg);

            String jpgWeb = "https://p5.manhuapan.com/";

            //把解析取得的圖片URL拿來做下載動作，可自行調整下載位置
            String fileName_https = downloadImageFromUrl(jpgWeb + jpg, "G:\\comicbook" + File.separator, String.valueOf(index));
            System.out.println(fileName_https);
        }

        webClient.close();
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
                    "Mozilla/5.0 (Linux; Android 4.2.1; Nexus 7 Build/JOP40D) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166  Safari/535.19");
            httpUrlConnection.connect();

            String imageType;
            if (httpUrlConnection.getResponseCode() == 200) {
                //成功取得response，
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
        } catch (IOException e) {
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

    public static HttpURLConnection getHttpURLConnectionFromHttps(String url) {
        HttpURLConnection httpUrlConnection = null;
        
        //建立一個信任所有憑證的X509TrustManager，放到TrustManager裡面
        TrustManager[] trustAllCerts;
        try {
            // Activate the new trust manager
            trustAllCerts = new TrustManager[] { new X509TrustManager() {
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
            } };

            //設置SSL設定
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            //跟HTTP一樣，用Url建立連線
            httpUrlConnection = (HttpURLConnection) (new URL(url)).openConnection();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return httpUrlConnection;
    }
}
