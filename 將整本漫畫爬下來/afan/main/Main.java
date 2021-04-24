package afan.main;

import afan.thread.reptileThread;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import javax.swing.*;
import java.io.File;
import java.util.Scanner;

/**
 * @author Afan Chen
 */
public class Main {
    public static void main(String[] args) {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);//建立一個模擬器模擬chrome

        webClient.getOptions().setThrowExceptionOnScriptError(false);//選擇js渲染出錯的話是否拋出異常
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);//選擇http通訊端口非200時是否丟出異常
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setCssEnabled(false);//選擇是否啟用CSS
        webClient.getOptions().setJavaScriptEnabled(true); //啟用js渲染網頁(才能爬取動態網頁資料，但時間會長很多)
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());//很重要，選擇支持AJAX

        System.out.println("請輸入要爬取的漫畫URL，例如:  https://manhua.fzdm.com/39/");
        System.out.println("格式已經給你了，記得不要亂搞阿喂!!");

        Scanner scanner = new Scanner(System.in);
        String inputURL = scanner.next();

        //選擇存檔目的資料夾，最好是一個空資料夾
        //乾選擇介面跳不出來，我等等再想一下，先不能選擇好了，請自己寫，反正我也沒有要把她打包成軟體的意思
        //String FilePath = ChoosePath();

        String FilePath="G:\\comicbook";//存檔位置，會在底下建立集數資料夾

        boolean flag=false;//標記漫畫已經結束

        for(int j=1;j<1000;++j) {

            StringBuilder reptileURL= new StringBuilder(inputURL);

            if(j<10) reptileURL.append('0');
            if(j<100) reptileURL.append('0');
            reptileURL.append(j).append("/");

            //弄好存檔資料夾
            String SaveFilePath=FilePath+"\\"+ j;

            //建立一個資料夾來存漫畫
            File f = new File(SaveFilePath);
            if (f.mkdir()) {
                System.out.println("資料夾"+j+"不存在，建立資料夾"+j+"成功");
            } else {
                System.out.println("資料夾"+j+"已存在");
            }

            //隨便假設頁數為100頁，反正畫家很懶，一話不會超過的...吧
            //需要特別注意的是每十頁要重新解析一次圖檔位置，被坑ㄌ
            for (int index = 0; index < 100; index += 10) {

                HtmlPage page;
                try {
                    System.out.println("正在讀取頁面: "+reptileURL + "index_" + index + ".html");
                    page = webClient.getPage(reptileURL + "index_" + index + ".html");//載入網頁
                } catch (Exception e) {
                    e.printStackTrace();
                    if(index==0) flag=true;
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
                jpg.append(pageXml, left + 12, right - 4);

                System.out.println(jpg);

                //解析後產生的網址其中一種
                String jpgWeb = "https://p5.manhuapan.com/";

                jpgWeb += jpg.substring(0, 8);
                System.out.println(jpgWeb);

                for (int i = -1; i < 9; ++i) {
                    reptileThread reptilethread = new reptileThread();
                    reptilethread.setURL(jpgWeb, Integer.parseInt(String.valueOf(jpg).substring(8)) + i, index + i + 1,SaveFilePath);
                    reptilethread.start();

                    //我發現爬太快會產生拿不到圖片的情況，可能被擋，或者單純是太快了
                    //後來發現是她每次只給十頁
                    try {
                        Thread.sleep(1000);//無法隨機，JVM不允許，推測是預編譯，必須先設定好
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            //結束爬蟲
            if(flag) break;
        }

        //關閉掉模擬的瀏覽器
        webClient.close();
    }

    /**
     * 不知道為什麼不能呼叫出來，推測可能是因為我沒有建立swing jframe.
     * @return 存檔路徑
     */
    private static String ChoosePath() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showSaveDialog(null);
        String path = "";
        if (result == JFileChooser.APPROVE_OPTION) {
            path = fileChooser.getSelectedFile().getAbsolutePath();
        }
        return path;
    }
}
