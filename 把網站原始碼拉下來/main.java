import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Scanner;

public class main {
    public static void main(String[] args) {
        Scanner scanner=new Scanner(System.in);
        System.out.println("請輸入欲爬取網站網址");
        String url = scanner.next();
        //打開瀏覽器創建httpclient對象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //輸入網址
        HttpGet httpGet = new HttpGet(url);
        //偽裝Chrome發送請求避免被擋
        httpGet.setHeader("User-Agent",
                "Mozilla/5.0 (Linux; Android 11; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85");
        //發送請求
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //響應結果
        assert httpResponse != null;
        HttpEntity httpEntity = httpResponse.getEntity();
        //解析結果
        String result = null;
        try {
            result = EntityUtils.toString(httpEntity, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //檢視
        System.out.println(result);
        //後面可以自行做進一步的解析
    }
}
