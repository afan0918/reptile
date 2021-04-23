package afan.main;

import afan.thread.reptileThread;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);//建立一個模擬器模擬chrome

        webClient.getOptions().setThrowExceptionOnScriptError(false);//選擇js渲染出錯的話是否拋出異常
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);//選擇http通訊端口非200時是否丟出異常
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setCssEnabled(false);//選擇是否啟用CSS
        webClient.getOptions().setJavaScriptEnabled(true); //啟用js渲染網頁(才能爬取動態網頁資料，但時間會長很多)
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());//很重要，選擇支持AJAX

        System.out.println("請輸入要爬取的漫畫所在集數URL");
        Scanner scanner = new Scanner(System.in);
        String reptileURL = scanner.next();

        //隨便假設頁數為100頁，反正畫家很懶，一話不會超過的...吧
        //需要特別注意的是每十頁要重新解析一次圖檔位置，被坑ㄌ
        for (int index = 0; index < 100; index += 10) {
            HtmlPage page;
            try {
                page = webClient.getPage(reptileURL + "index_" + index + ".html");//載入網頁
            } catch (Exception e) {
                e.printStackTrace();
                break;//拿不到就直接認定本集結束了
            }

            //等待js渲染結束
            webClient.waitForBackgroundJavaScript(20000);

            String pageXml;
            //將加載完的頁面轉換成xml格式
            if (page != null) {
                pageXml = page.asXml();
            } else {
                break;
            }

            //把需要的資料硬拉出來
            int left = pageXml.indexOf("var mhurl1=\"");
            int right = pageXml.indexOf("\";mhpicurl=");
            StringBuilder jpg = new StringBuilder();
            for (int i = left + 12; i < right - 4; i++) {
                jpg.append(pageXml.charAt(i));
            }
            System.out.println(jpg);

            String jpgWeb = "https://p5.manhuapan.com/";

            jpgWeb += jpg.substring(0, 8);
            System.out.println(jpgWeb);

            for (int i = -1; i < 9; ++i) {
                reptileThread reptilethread = new reptileThread();
                reptilethread.setURL(jpgWeb, Integer.parseInt(String.valueOf(jpg).substring(8)) + i, index + i + 1);
                reptilethread.start();

                //我發現爬太快會產生拿不到圖片的情況，可能被擋，或者單純是太快了
                //後來發現是她每次只給十頁
                try {
                    Thread.sleep(1000);//
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //關閉掉模擬的瀏覽器
        webClient.close();
    }
}
