//业务层实现类，重写业务层接口中的方法,实现相应的功能
package com.example.seafarers_spider.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.seafarers_spider.dao.SeafarersDao;
import com.example.seafarers_spider.models.Documents;
import com.example.seafarers_spider.models.SeaServices;
import com.example.seafarers_spider.models.Seafarers;
import com.example.seafarers_spider.service.ISeafarersService;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service//表示该类是业务层
public class ISeafarersServiceImpl extends ServiceImpl<SeafarersDao, Seafarers> implements ISeafarersService {

    //导入数据层接口对象
    @Autowired
    private SeafarersDao seafarersDao;

    //一、爬取数据
    /* 1.通过Htmlunit工具，获取列表页的HtmlPage对象
     * 参数：浏览器客户端webClient对象，页面url
     * 返回值：搜索页HtmlPage对象 */
    @Override
    public HtmlPage htmlunitPageDown(WebClient webClient, String url) {
        //1.获取页面信息,生成一个HtmlPage对象
        HtmlPage htmlPage = null;
        try {
            WebRequest webRequest = new WebRequest(new URL(url));//生成一个WebRequest对象
            htmlPage = webClient.getPage(webRequest);//使用WebClient对象的getPage方法，生成一个HtmlPage对象，这个对象就是输入网址后的首页对象
        } catch (IOException e) {
            e.printStackTrace();//在所有的联网获取页面处可能会出现异常，因为页面有可能获取不到，所以用try catch捕获并处理异常，防止异常传到调用者处
        }

        //2.异步JS执行需要耗时,所以这里线程要阻塞30秒,等待异步JS执行结束
        webClient.waitForBackgroundJavaScript(600000);//给页面充足的加载时间
        webClient.setJavaScriptTimeout(600000);

        //3.返回这个HtmlPage对象
        return htmlPage;
    }

    /* 2.模拟点击，获取含有10个船员信息的首页
     * 参数：客户端服务器WebClient对象，HtmlunitPageDown返回的HtmlPage对象,列表页a标签点击按钮对应的data-id
     * 返回值：带有10条船员信息的HtmlPage对象*/
    @Override
    public HtmlPage homePageClick(WebClient webClient, HtmlPage htmlPage, String dataId) {
        //1.根据data-id这一属性找到对应的职位按钮
        DomElement more = null;
        DomNodeList<DomElement> domElements = htmlPage.getElementsByTagName("a");//获取所有的a标签
        for (DomElement domElement : domElements) {//遍历所有标签，找到需要点击的那个唯一的标签
            if (domElement.getAttribute("data-id").equals(dataId)) {//通过属性名class，属性值获取到特定的domElement
                more = domElement;
                break;
            }
        }

        //2.模拟点击
        if (more != null) {//判断这个按钮存在时才进行点击
            try {
                htmlPage = more.click();
            } catch (IOException e) {
                e.printStackTrace();
            }
            webClient.waitForBackgroundJavaScript(600000);//给click事件充足时间
        }

        //3.返回
        return htmlPage;
    }

    /* 3.模拟点击，获取所有的船员信息的页面
     * 参数：浏览器客户端WebClient对象，首页的HtmlPage对象
     * 返回值：包含完整信息的HtmlPage对象*/
    @Override
    public HtmlPage completePageClick(WebClient webClient, HtmlPage htmlPage) {
        //1.获取首页的那个button按钮
        DomElement more = null;
        DomNodeList<DomElement> domElements = htmlPage.getElementsByTagName("button");//因为要通过class获取标签，所以要先获取全部的button标签
        for (DomElement domElement : domElements) {//遍历所有标签，找到需要点击的那个唯一的标签
            if (domElement.getAttribute("class").equals("btn btn-default col-xs-4 col-xs-offset-4 w_p_next")) {//通过属性名class，属性值获取到特定的domElement
                more = domElement;
                break;
            }
        }

        //2.循环点击这个button按钮，直到这个按钮消失（在这个网页中，没有信息可以加载时，按钮会消失，对应的button标签也会消失）
        while (more != null) {//当这个标签还存在的时候，说明此时还有信息没加载完
            try {
                htmlPage = more.click();//模拟点击一下加载按钮，加载新的10条数据出来
            } catch (IOException e) {
                e.printStackTrace();
            }
            webClient.waitForBackgroundJavaScript(600000);//给click事件充足时间
            //3.重新加载读取后的页面，在其中找到button按钮
            more = null;//重置more，如果没有找到相应的标签，则跳出循环
            domElements = htmlPage.getElementsByTagName("button");//因为要通过class获取标签，所以要先获取全部的button标签
            for (DomElement domElement : domElements) {//遍历所有标签，找到需要点击的那个唯一的标签
                if (domElement.getAttribute("class").equals("btn btn-default col-xs-4 col-xs-offset-4 w_p_next")) {//通过属性名class，属性值获取到特定的domElement
                    more = domElement;
                    break;
                }
            }
        }

        //4.将完整信息页面的HtmlPage对象返回
        return htmlPage;
    }

    /* 4.获取页面信息，并将页面的所有信息加载到本地文件中的 主方法
     * 参数：船员信息页面的url
     * 返回值：无 */
    @Override
    public void pageDown(String url) {
        //1.定义浏览器客户端对象：新建一个模拟谷歌Chrome浏览器的浏览器客户端对象，这个对象贯穿始终，在方法结束时将这个客户端关闭
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        //对这个客户端全局常量进行一些配置
        webClient.getOptions().setThrowExceptionOnScriptError(false); //当JS执行出错的时候是否抛出异常, 这里选择不需要
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);//当HTTP的状态非200时是否抛出异常, 这里选择不需要
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setCssEnabled(false);//是否启用CSS, 因为不需要展现页面, 所以不需要启用
        webClient.getOptions().setJavaScriptEnabled(true);//很重要，启用JS
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());//很重要，设置支持AJAX

        //2.定义放置参数的HashMap，键：html中a标签的data-id值，值：文件存储路径/home/zyh/桌面/BiShe/data下的分类
        HashMap<String, String> filePaths = new HashMap<String, String>();
        //将所有的data-id与对应文件路径信息放入HashMap中
        //Deck Department 甲板部
        filePaths.put("13", "/Deck_Department/MASTER.txt");
        filePaths.put("11", "/Deck_Department/CHIEF_OFFICER.txt");
        filePaths.put("3", "/Deck_Department/SECOND_OFFICER.txt");
        filePaths.put("5", "/Deck_Department/THIRD_OFFICER.txt");
        filePaths.put("39", "/Deck_Department/FOURTH_OFFICER.txt");
        filePaths.put("37", "/Deck_Department/TRAINEE_OFFICER.txt");
        filePaths.put("7", "/Deck_Department/BOSUN.txt");
        filePaths.put("6", "/Deck_Department/ABLE_SEAMAN.txt");
        filePaths.put("44", "/Deck_Department/ORDINARY_SEAMAN.txt");
        filePaths.put("14", "/Deck_Department/CARPENTER.txt");
        filePaths.put("30", "/Deck_Department/SAND_BLASTER.txt");
        filePaths.put("15", "/Deck_Department/DECK_CADET.txt");
        filePaths.put("82", "/Deck_Department/DECK_HAND.txt");
        //Engine Department 引擎部
        filePaths.put("9", "/Engine_Department/CHIEF_ENGINEER.txt");
        filePaths.put("1", "/Engine_Department/SECOND_ENGINEER.txt");
        filePaths.put("2", "/Engine_Department/THIRD_ENGINEER.txt");
        filePaths.put("4", "/Engine_Department/FOURTH_ENGINEER.txt");
        filePaths.put("33", "/Engine_Department/TRAINEE_ENGINEER.txt");
        filePaths.put("47", "/Engine_Department/ELECTRICAL_ENGINEER.txt");
        filePaths.put("24", "/Engine_Department/GAS_ENGINEER.txt");
        filePaths.put("29", "/Engine_Department/REEF_ENGINEER.txt");
        filePaths.put("34", "/Engine_Department/TECHNICIAN.txt");
        filePaths.put("56", "/Engine_Department/ELECTRICIAN.txt");
        filePaths.put("35", "/Engine_Department/TRAINEE_ELECTRICIAN.txt");
        filePaths.put("28", "/Engine_Department/PUMPMAN.txt");
        filePaths.put("46", "/Engine_Department/FITTER.txt");
        filePaths.put("26", "/Engine_Department/MOTORMANOILER.txt");
        filePaths.put("57", "/Engine_Department/MOTORMAN_GRADE_2WIPER.txt");
        filePaths.put("58", "/Engine_Department/WELDER.txt");
        filePaths.put("52", "/Engine_Department/ELECTRONIC_OFFICER.txt");
        filePaths.put("27", "/Engine_Department/SUPERINTENDENT.txt");
        filePaths.put("17", "/Engine_Department/ENGINE_CADET.txt");
        filePaths.put("22", "/Engine_Department/ELECTRICAL_CADET.txt");
        //Catering Department 餐饮部
        filePaths.put("12", "/Catering_Department/CHIEF_STEWARD.txt");
        filePaths.put("32", "/Catering_Department/STEWARD.txt");
        filePaths.put("59", "/Catering_Department/CHIEF_COOK.txt");
        filePaths.put("10", "/Catering_Department/COOK.txt");
        filePaths.put("80", "/Catering_Department/BARTENDER.txt");
        filePaths.put("81", "/Catering_Department/WAITER.txt");
        filePaths.put("8", "/Catering_Department/MESS_MAIDBOY.txt");
        //Offshore Specific 离岸特定
        filePaths.put("64", "/Offshore_Specific/MASTER.txt");
        filePaths.put("65", "/Offshore_Specific/CHIEF_OFFICER.txt");
        filePaths.put("83", "/Offshore_Specific/SDPO.txt");
        filePaths.put("60", "/Offshore_Specific/SECOND_OFFICER.txt");
        filePaths.put("66", "/Offshore_Specific/THIRD_OFFICER.txt");
        filePaths.put("67", "/Offshore_Specific/JUNIOR_OFFICER.txt");
        filePaths.put("71", "/Offshore_Specific/CHIEF_ENGINEER.txt");
        filePaths.put("72", "/Offshore_Specific/SECOND_ENGINEER.txt");
        filePaths.put("73", "/Offshore_Specific/THIRD_ENGINEER.txt");
        filePaths.put("62", "/Offshore_Specific/FOURTH_ENGINEER.txt");
        filePaths.put("69", "/Offshore_Specific/ETO.txt");
        filePaths.put("63", "/Offshore_Specific/CRANE_OPERATOR.txt");
        filePaths.put("61", "/Offshore_Specific/ABLE_SEAMAN.txt");
        filePaths.put("68", "/Offshore_Specific/WELDER.txt");
        filePaths.put("70", "/Offshore_Specific/ELECTRICIAN.txt");
        filePaths.put("75", "/Offshore_Specific/RIGGER.txt");
        filePaths.put("76", "/Offshore_Specific/PIPE_LAYER.txt");
        filePaths.put("77", "/Offshore_Specific/DIVER.txt");
        filePaths.put("78", "/Offshore_Specific/TECHNICIAN.txt");
        filePaths.put("79", "/Offshore_Specific/OTHER.txt");

        //3.提前建立首页的HtmlPage对象和一些局部变量，防止循环中一直新建对象导致占用内存过大
        HtmlPage searchHtmlPage = null;//搜索页
        HtmlPage homeHtmlPage = null;//带有10个船员信息的主页
        HtmlPage completeHtmlPage = null;//带有全部船员信息的完整的页面
        String dataId = null;//html页面中的对应职位按钮的a标签中的data-id
        String filePath = null;//对应职位的文件存储路径
        String fullPath = null;//对应职位的文件存储完整路径
        String html = null;//xml格式的字符类型数据

        //4.根据输入的url，获取搜索页的HtmlPage对象，调用utils.htmlunit.HtmlunitPageDown的方法
        searchHtmlPage = htmlunitPageDown(webClient, url);

        //5.遍历HashMap中的data-id和文件存储路径
        Set<Map.Entry<String, String>> paths = filePaths.entrySet();//获取所有键值对对象的集合
        for (Map.Entry<String, String> path : paths) {//遍历键值对对象的集合，得到每一个键值对对象
            dataId = path.getKey();//获取其中的键和值
            filePath = path.getValue();
            //6.将dataId作为参数传入homePageClick中，获取分类职位的带有10个信息的首页
            homeHtmlPage = homePageClick(webClient, searchHtmlPage, dataId);
            //7.根据首页的HtmlPage对象，获取完整的船员信息的HtmlPage对象
            completeHtmlPage = completePageClick(webClient, homeHtmlPage);
            //8.通过字符输出缓冲流，将页面转换成xml格式，储存在文件中，方便日后分析
            html = completeHtmlPage.asXml();//将页面HtmlPage对象转换成xml格式字符串
            fullPath = "/home/zyh/桌面/BiShe/data" + filePath;//合成完整路径
            BufferedWriter fwriter = null;
            try {
                fwriter = new BufferedWriter(new FileWriter(fullPath));
                fwriter.write(html);//将xml字符串写入文件
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fwriter.flush();
                    fwriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //9.关闭浏览器客户端，释放资源
        webClient.close();
    }

    //二、解析数据
    /* 1.解析一个文件
     * 参数：文件路径filePath
     * 返回值：无 */
    @Override
    public void analyze(String filePath) {
        //1.拼接成完整文件存储路径
        String fullPath = "/home/zyh/桌面/BiShe/data" + filePath;

        //2.解析文件，生成相应的Document对象
        Document document = null;
        try {
            document = Jsoup.parse(new File(fullPath), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //3.根据class，使用选择器，获取所有存储船员信息的标签
        Elements elements = null;
        try {
            elements = document.select("div.w_all_wrap");//div标签下，class值为w_all_warp的信息
        } catch (Exception e) {
            e.printStackTrace();
        }

        //4.判断这个elements中是否有元素，存在则继续执行；不存在则说明是没有船员信息的空页面，则跳出程序
        if (elements.size() != 0) {
            //5.生成一个储存所有船员id的列表,用于对船员信息准确定位
            ArrayList<String> dataIds = new ArrayList<String>();
            for (Element element : elements) {
                dataIds.add(element.attr("data-id"));//获取每个船员信息的id，用于select选择器对每个船员信息进行精准定位
            }
            //6.遍历这个列表，依次将每个船员信息进行解析
            for (String dataId : dataIds) {
                //7.通过选择器将某个船员的信息全部解析出来
                //id
                int id = Integer.parseInt(dataId);
                //name,age
                String nameAndAgeCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(1) > div:nth-child(3)";
                String nameAndAge = document.select(nameAndAgeCssQuery).text();
                String name = nameAndAge.split(", ")[0];
                String age = nameAndAge.split(", ")[1];
                //appliedFor
                String appliedForCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2) > b:nth-child(1)";
                String appliedFor = document.select(appliedForCssQuery).text();
                //alternative
                String alternativeCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(2) > b:nth-child(1)";
                String alternative = document.select(alternativeCssQuery).text();
                //availability
                String availabilityCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(3) > td:nth-child(2) > b:nth-child(1)";
                String availability = document.select(availabilityCssQuery).text();
                //salary
                String salaryCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(4) > td:nth-child(2) > b:nth-child(1)";
                String salary = document.select(salaryCssQuery).text();
                //citizenship
                String citizenshipCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(5) > td:nth-child(2)";
                String citizenship = document.select(citizenshipCssQuery).text();
                //nationality
                String nationalityCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(6) > td:nth-child(2)";
                String nationality = document.select(nationalityCssQuery).text();
                //english
                String englishCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(7) > td:nth-child(2)";
                String english = document.select(englishCssQuery).text();
                //usVisa
                String usVisaCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(8) > td:nth-child(2)";
                String usVisa = document.select(usVisaCssQuery).text();
                //schengenVisa
                String schengenVisaCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(9) > td:nth-child(2)";
                String schengenVisa = document.select(schengenVisaCssQuery).text();
                //nearestAirport
                String nearestAirportCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(10) > td:nth-child(2)";
                String nearestAirport = document.select(nearestAirportCssQuery).text();
                //vesselTypes
                String vesselTypesCssQuery = "#summary-" + dataId + " > div:nth-child(2) > p:nth-child(1) > b:nth-child(2)";
                String vesselTypes = document.select(vesselTypesCssQuery).text();
                //engineTypesAndModels
                String engineTypesAndModelsCssQuery = "#summary-" + dataId + " > div:nth-child(2) > p:nth-child(2) > b:nth-child(2)";
                String engineTypesAndModels = document.select(engineTypesAndModelsCssQuery).text();
                //lastKnownContract
                String lastKnownContractCssQuery = "#summary-" + dataId + " > div:nth-child(2) > p:nth-child(3) > b:nth-child(2)";
                String lastKnownContract = document.select(lastKnownContractCssQuery).text();
                //coc
                String cocCssQuery = "#certificates-" + dataId + " > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2) > b:nth-child(1)";
                String coc = document.select(cocCssQuery).text();
                //endorsement
                String endorsementCssQuery = "#certificates-" + dataId + " > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(2) > b:nth-child(1)";
                String endorsement = document.select(endorsementCssQuery).text();
                //document
                String documentsCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) tr";
                Elements documentsElements = document.select(documentsCssQuery);
                ArrayList<Documents> documentsArrayList = new ArrayList<Documents>();//新建一个存储证书对象的list集合
                for (int i = 1; i <= documentsElements.size(); i++) {
                    //证书名
                    String nameOfDocumentCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(1)";
                    String nameOfDocument = document.select(nameOfDocumentCssQuery).text();
                    //证书编号
                    String numberCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(2)";
                    String number = document.select(numberCssQuery).text();
                    //发行日期
                    String issueDateCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(3)";
                    String issueDate = document.select(issueDateCssQuery).text();
                    //到期时间
                    String expireDateCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(4)";
                    String expireDate = document.select(expireDateCssQuery).text();
                    //签发地点
                    String placeOfIssueCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(5)";
                    String placeOfIssue = document.select(placeOfIssueCssQuery).text();
                    //将这些信息写入对象
                    Documents documents = new Documents(nameOfDocument, number, issueDate, expireDate, placeOfIssue);
                    //将这个对象加入list集合中
                    documentsArrayList.add(documents);
                }
                String documentsJson = JSON.toJSONString(documentsArrayList, SerializerFeature.WriteMapNullValue);//将所有的证书通过fastjson转换为json对象
                //sea services
                String seaServicesCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) tr";
                Elements seaServicesElements = document.select(seaServicesCssQuery);
                ArrayList<SeaServices> seaServicesArrayList = new ArrayList<SeaServices>();//新建一个存储出海经历的list集合
                for (int i = 1; i <= seaServicesElements.size(); i++) {
                    //船名
                    String vesselNameCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(1)";
                    String vesselName = document.select(vesselNameCssQuery).text();
                    //旗帜
                    String flagCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(2)";
                    String flag = document.select(flagCssQuery).text();
                    //发动机类型
                    String engineTypeCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(3)";
                    String engineType = document.select(engineTypeCssQuery).text();
                    //职级
                    String rankCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(4)";
                    String rank = document.select(rankCssQuery).text();
                    //经理/船员代理
                    String managerCrewAgentCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(5)";
                    String managerCrewAgent = document.select(managerCrewAgentCssQuery).text();
                    //开始时间
                    String fromCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(6)";
                    String from = document.select(fromCssQuery).text();
                    //结束时间
                    String toCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(7)";
                    String to = document.select(toCssQuery).text();
                    //将这些信息写入对象
                    SeaServices seaServices = new SeaServices(vesselName, flag, engineType, rank, managerCrewAgent, from, to);
                    //将这些信息加入到list集合中
                    seaServicesArrayList.add(seaServices);
                }
                String seaServicesJson = JSON.toJSONString(seaServicesArrayList, SerializerFeature.WriteMapNullValue);//将所有的出海经历通过fastjson转换为json对象
                //lastUpdate
                String lastUpdateCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > div:nth-child(1) > div:nth-child(3) > b:nth-child(1)";
                String lastUpdate = document.select(lastUpdateCssQuery).text();
                //8.创建SeafarersModel模型类对象
                Seafarers newSeafarer = new Seafarers(id, name, age, appliedFor, alternative, availability, salary, citizenship, nationality, english, usVisa, schengenVisa, nearestAirport, vesselTypes, engineTypesAndModels, lastKnownContract, coc, endorsement, documentsJson, seaServicesJson, lastUpdate);
                //9.使用数据层接口的方法将数据存入数据库
                try {
                    seafarersDao.insert(newSeafarer);//因为id的值唯一，因此当数据重复时可能会报错，因此在这里捕获异常
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* 2.解析所有文件 主方法
     * 参数：无
     * 返回值：无 */
    @Override
    public void analysis() {
        //1.定义文件路径，从存储xml字符串的文件中提取提取信息
        ArrayList<String> filePaths = new ArrayList<String>();
        //将文件路径全部存入
        //Deck Department 甲板部
        filePaths.add("/Deck_Department/MASTER.txt");
        filePaths.add("/Deck_Department/CHIEF_OFFICER.txt");
        filePaths.add("/Deck_Department/SECOND_OFFICER.txt");
        filePaths.add("/Deck_Department/THIRD_OFFICER.txt");
        filePaths.add("/Deck_Department/FOURTH_OFFICER.txt");
        filePaths.add("/Deck_Department/TRAINEE_OFFICER.txt");
        filePaths.add("/Deck_Department/BOSUN.txt");
        filePaths.add("/Deck_Department/ABLE_SEAMAN.txt");
        filePaths.add("/Deck_Department/ORDINARY_SEAMAN.txt");
        filePaths.add("/Deck_Department/CARPENTER.txt");
        filePaths.add("/Deck_Department/SAND_BLASTER.txt");
        filePaths.add("/Deck_Department/DECK_CADET.txt");
        filePaths.add("/Deck_Department/DECK_HAND.txt");
        //Engine Department 引擎部
        filePaths.add("/Engine_Department/CHIEF_ENGINEER.txt");
        filePaths.add("/Engine_Department/SECOND_ENGINEER.txt");
        filePaths.add("/Engine_Department/THIRD_ENGINEER.txt");
        filePaths.add("/Engine_Department/FOURTH_ENGINEER.txt");
        filePaths.add("/Engine_Department/TRAINEE_ENGINEER.txt");
        filePaths.add("/Engine_Department/ELECTRICAL_ENGINEER.txt");
        filePaths.add("/Engine_Department/GAS_ENGINEER.txt");
        filePaths.add("/Engine_Department/REEF_ENGINEER.txt");
        filePaths.add("/Engine_Department/TECHNICIAN.txt");
        filePaths.add("/Engine_Department/ELECTRICIAN.txt");
        filePaths.add("/Engine_Department/TRAINEE_ELECTRICIAN.txt");
        filePaths.add("/Engine_Department/PUMPMAN.txt");
        filePaths.add("/Engine_Department/FITTER.txt");
        filePaths.add("/Engine_Department/MOTORMANOILER.txt");
        filePaths.add("/Engine_Department/MOTORMAN_GRADE_2WIPER.txt");
        filePaths.add("/Engine_Department/WELDER.txt");
        filePaths.add("/Engine_Department/ELECTRONIC_OFFICER.txt");
        filePaths.add("/Engine_Department/SUPERINTENDENT.txt");
        filePaths.add("/Engine_Department/ENGINE_CADET.txt");
        filePaths.add("/Engine_Department/ELECTRICAL_CADET.txt");
        //Catering Department 餐饮部
        filePaths.add("/Catering_Department/CHIEF_STEWARD.txt");
        filePaths.add("/Catering_Department/STEWARD.txt");
        filePaths.add("/Catering_Department/CHIEF_COOK.txt");
        filePaths.add("/Catering_Department/COOK.txt");
        filePaths.add("/Catering_Department/BARTENDER.txt");
        filePaths.add("/Catering_Department/WAITER.txt");
        filePaths.add("/Catering_Department/MESS_MAIDBOY.txt");
        //Offshore Specific 离岸特定
        filePaths.add("/Offshore_Specific/MASTER.txt");
        filePaths.add("/Offshore_Specific/CHIEF_OFFICER.txt");
        filePaths.add("/Offshore_Specific/SDPO.txt");
        filePaths.add("/Offshore_Specific/SECOND_OFFICER.txt");
        filePaths.add("/Offshore_Specific/THIRD_OFFICER.txt");
        filePaths.add("/Offshore_Specific/JUNIOR_OFFICER.txt");
        filePaths.add("/Offshore_Specific/CHIEF_ENGINEER.txt");
        filePaths.add("/Offshore_Specific/SECOND_ENGINEER.txt");
        filePaths.add("/Offshore_Specific/THIRD_ENGINEER.txt");
        filePaths.add("/Offshore_Specific/FOURTH_ENGINEER.txt");
        filePaths.add("/Offshore_Specific/ETO.txt");
        filePaths.add("/Offshore_Specific/CRANE_OPERATOR.txt");
        filePaths.add("/Offshore_Specific/ABLE_SEAMAN.txt");
        filePaths.add("/Offshore_Specific/WELDER.txt");
        filePaths.add("/Offshore_Specific/ELECTRICIAN.txt");
        filePaths.add("/Offshore_Specific/RIGGER.txt");
        filePaths.add("/Offshore_Specific/PIPE_LAYER.txt");
        filePaths.add("/Offshore_Specific/DIVER.txt");
        filePaths.add("/Offshore_Specific/TECHNICIAN.txt");
        filePaths.add("/Offshore_Specific/OTHER.txt");

        //2.遍历文件名列表，将每个文件的船员信息数据存入数据库
        for (String filePath : filePaths) {
            analyze(filePath);
        }
    }

    //三、增量爬虫,定时更新数据
    /* 1.模拟点击搜索，获取含有10个船员信息的首页
     * 参数：客户端服务器WebClient对象，htmlunitPageDown返回的HtmlPage对象
     * 返回值：带有10条船员信息的HtmlPage对象 */
    @Override
    public HtmlPage updateHomePageClick(WebClient webClient, HtmlPage htmlPage) {
        //1.根据id进行判断，找到点击按钮，生成DomElement对象
        DomElement more = htmlPage.getElementById("b_submit");

        //2.调用DomElement对象的click方法，模拟点击按钮，生成一个新的HtmlPage对象
        try {
            htmlPage = more.click();
        } catch (IOException e) {
            e.printStackTrace();
        }
        webClient.waitForBackgroundJavaScript(600000);//给click事件充足时间

        //3.将这个页面的HtmlPage对象返回
        return htmlPage;
    }

    /* 2.模拟点击，获取最新的船员信息页面
     * 参数：客户端服务器WebClient对象，updateHomePageClick返回的HtmlPage对象
     * 返回值：所有的比数据库中更新的船员信息*/
    @Override
    public HtmlPage updateCompletePageDown(WebClient webClient, HtmlPage htmlPage) {
        //1.首先判断带有10个信息的首页的最后一条数据是否比数据库最新的数据更早
        //1.1 解析首页信息
        Document document = null;
        try {
            document = Jsoup.parse(htmlPage.asXml());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //1.2 使用css选择器，从首页中获取第10条数据的更新时间
        String pageLastUpdate = document.select("div.w_all_wrap:nth-child(10) > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > div:nth-child(1) > div:nth-child(3) > b:nth-child(1)").text();
        //1.3 将该页面的第10条解析为日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");//日期格式转换器
        Date pageDate = null;
        try {
            pageDate = sdf.parse(pageLastUpdate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println("页面日期是：" + pageLastUpdate);//====================================================
        //1.4 从数据库中取出最新的船员信息的更新日期，将其转换为日期格式
        QueryWrapper<Seafarers> qw = new QueryWrapper<Seafarers>();//创建条件查询对象
        qw.select("last_update");//查询最近更新日期
        qw.orderByDesc("to_date(last_update,'DD.MM.YYYY')");
        qw.last("limit 1");
        String sqlLastUpdate = seafarersDao.selectOne(qw).getLastUpdate();
        System.out.println("数据库日期是：" + sqlLastUpdate);//================================================
        Date sqlDate = null;
        try {
            sqlDate = sdf.parse(sqlLastUpdate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //2.循环点击，判断后续的每一页的最后一个数据是否比数据库的数据新
        int i = 2;//标志位，表示搜索到了第几页，用于后续css选择器
        //2.1 获取首页的那个button按钮
        DomElement more = null;
        DomNodeList<DomElement> domElements = htmlPage.getElementsByTagName("button");//因为要通过class获取标签，所以要先获取全部的button标签
        for (DomElement domElement : domElements) {//遍历所有标签，找到需要点击的那个唯一的标签
            if (domElement.getAttribute("class").equals("btn btn-default col-xs-4 col-xs-offset-4 w_p_next")) {//通过属性名class，属性值获取到特定的domElement
                more = domElement;
                break;
            }
        }
        if (more != null) {
            System.out.println("more存在");//================================================
            while (sqlDate.compareTo(pageDate) <= 0) {//相等也不能保证数据完整性，因此一定要数据库信息比页面信息新才可以
                //2.2 点击这个button按钮
                try {
                    htmlPage = more.click();//模拟点击一下加载按钮，加载新的10条数据出来
                } catch (IOException e) {
                    e.printStackTrace();
                }
                webClient.waitForBackgroundJavaScript(600000);//给click事件充足时间
                //2.3 重新加载读取后的页面，在新的页面中找到新的button按钮
                domElements = htmlPage.getElementsByTagName("button");//因为要通过class获取标签，所以要先获取全部的button标签
                for (DomElement domElement : domElements) {//遍历所有标签，找到需要点击的那个唯一的标签
                    if (domElement.getAttribute("class").equals("btn btn-default col-xs-4 col-xs-offset-4 w_p_next")) {//通过属性名class，属性值获取到特定的domElement
                        more = domElement;
                        break;
                    }
                }
                //2.4 在新加载的页面中获取第10个船员的更新时间
                document = Jsoup.parse(htmlPage.asXml());//根据新的html页面，获取新的document对象
                String cssQuery = "div.w_r_c:nth-child(" + i + ") > div:nth-child(10) > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > div:nth-child(1) > div:nth-child(3) > b:nth-child(1)";
                System.out.println("到第" + i + "页了");//================================================
                i++;//将标志位加1
                pageLastUpdate = document.select(cssQuery).text();
                try {
                    pageDate = sdf.parse(pageLastUpdate);//获取更新时间
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                System.out.println("页面日期是：" + pageLastUpdate);//================================================
            }
        }


        //3.将这个html页面返回
        return htmlPage;
    }

    /* 3.获取页面信息，并将最新船员信息的页面的信息加载到本地文件中 主方法
     * 参数：船员信息页面的url
     * 返回值：无 */
    @Override
    public void updatePageDown(String url) {
        //1.定义浏览器客户端对象：新建一个模拟谷歌Chrome浏览器的浏览器客户端对象，这个对象贯穿始终，在方法结束时将这个客户端关闭
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        //对这个客户端全局常量进行一些配置
        webClient.getOptions().setThrowExceptionOnScriptError(false); //当JS执行出错的时候是否抛出异常, 这里选择不需要
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);//当HTTP的状态非200时是否抛出异常, 这里选择不需要
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setCssEnabled(false);//是否启用CSS, 因为不需要展现页面, 所以不需要启用
        webClient.getOptions().setJavaScriptEnabled(true);//很重要，启用JS
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());//很重要，设置支持AJAX

        //2.获得搜索页的页面
        HtmlPage updateSearchHtmlPage = htmlunitPageDown(webClient, url);

        //3.获取只有10个信息的页面
        HtmlPage homeHtmlPage = updateHomePageClick(webClient, updateSearchHtmlPage);

        //4.获取全部新船员信息的页面
        HtmlPage updateCompleteHtmlPage = updateCompletePageDown(webClient, homeHtmlPage);

        //5.将页面信息转换为xml字符串，写入到文件中
        String html = updateCompleteHtmlPage.asXml();
        String filePath = "/home/zyh/桌面/BiShe/data/new.txt";
        BufferedWriter fwriter = null;
        try {
            fwriter = new BufferedWriter(new FileWriter(filePath));//覆盖写入，new.txt文件重复使用
            fwriter.write(html);//将xml字符串写入文件
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fwriter.flush();
                fwriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //6.关闭客户端对象
        webClient.close();
    }

    /* 4.解析最新船员页面信息，并将其加载到数据库中 主方法
     * 参数：无
     * 返回值：无 */
    @Override
    public void updateAnalysis() {
        //1.定义从哪个文件中读取数据
        String filePath = "/home/zyh/桌面/BiShe/data/new.txt";

        //2.解析文件，生成相应的Document对象
        Document document = null;
        try {
            document = Jsoup.parse(new File(filePath), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //3.根据class，使用选择器，获取所有存储船员信息的标签
        Elements elements = null;
        try {
            elements = document.select("div.w_all_wrap");//div标签下，class值为w_all_warp的信息
        } catch (Exception e) {
            e.printStackTrace();
        }

        //4.判断这个elements中是否有元素，存在则继续执行；不存在则说明是没有船员信息的空页面，则跳出程序
        if (elements.size() != 0) {
            //5.生成一个储存所有船员id的列表,用于对船员信息准确定位
            ArrayList<String> dataIds = new ArrayList<String>();
            for (Element element : elements) {
                dataIds.add(element.attr("data-id"));//获取每个船员信息的id，用于select选择器对每个船员信息进行精准定位
            }
            //6.遍历这个列表，依次将每个船员信息进行解析
            for (String dataId : dataIds) {
                //7.通过选择器将某个船员的信息全部解析出来
                //id
                int id = Integer.parseInt(dataId);
                //在数据库中查找该id，如果存在，则说明数据更新，将原数据删除，将新的数据插入
                if (seafarersDao.selectById(id) != null) {
                    seafarersDao.deleteById(id);
                }
                //name,age
                String nameAndAgeCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(1) > div:nth-child(3)";
                String nameAndAge = document.select(nameAndAgeCssQuery).text();
                String name = nameAndAge.split(", ")[0];
                String age = nameAndAge.split(", ")[1];
                //appliedFor
                String appliedForCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2) > b:nth-child(1)";
                String appliedFor = document.select(appliedForCssQuery).text();
                //alternative
                String alternativeCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(2) > b:nth-child(1)";
                String alternative = document.select(alternativeCssQuery).text();
                //availability
                String availabilityCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(3) > td:nth-child(2) > b:nth-child(1)";
                String availability = document.select(availabilityCssQuery).text();
                //salary
                String salaryCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(4) > td:nth-child(2) > b:nth-child(1)";
                String salary = document.select(salaryCssQuery).text();
                //citizenship
                String citizenshipCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(5) > td:nth-child(2)";
                String citizenship = document.select(citizenshipCssQuery).text();
                //nationality
                String nationalityCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(6) > td:nth-child(2)";
                String nationality = document.select(nationalityCssQuery).text();
                //english
                String englishCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(7) > td:nth-child(2)";
                String english = document.select(englishCssQuery).text();
                //usVisa
                String usVisaCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(8) > td:nth-child(2)";
                String usVisa = document.select(usVisaCssQuery).text();
                //schengenVisa
                String schengenVisaCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(9) > td:nth-child(2)";
                String schengenVisa = document.select(schengenVisaCssQuery).text();
                //nearestAirport
                String nearestAirportCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(10) > td:nth-child(2)";
                String nearestAirport = document.select(nearestAirportCssQuery).text();
                //vesselTypes
                String vesselTypesCssQuery = "#summary-" + dataId + " > div:nth-child(2) > p:nth-child(1) > b:nth-child(2)";
                String vesselTypes = document.select(vesselTypesCssQuery).text();
                //engineTypesAndModels
                String engineTypesAndModelsCssQuery = "#summary-" + dataId + " > div:nth-child(2) > p:nth-child(2) > b:nth-child(2)";
                String engineTypesAndModels = document.select(engineTypesAndModelsCssQuery).text();
                //lastKnownContract
                String lastKnownContractCssQuery = "#summary-" + dataId + " > div:nth-child(2) > p:nth-child(3) > b:nth-child(2)";
                String lastKnownContract = document.select(lastKnownContractCssQuery).text();
                //coc
                String cocCssQuery = "#certificates-" + dataId + " > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2) > b:nth-child(1)";
                String coc = document.select(cocCssQuery).text();
                //endorsement
                String endorsementCssQuery = "#certificates-" + dataId + " > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(2) > b:nth-child(1)";
                String endorsement = document.select(endorsementCssQuery).text();
                //document
                String documentsCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) tr";
                Elements documentsElements = document.select(documentsCssQuery);
                ArrayList<Documents> documentsArrayList = new ArrayList<Documents>();//新建一个存储证书对象的list集合
                for (int i = 1; i <= documentsElements.size(); i++) {
                    //证书名
                    String nameOfDocumentCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(1)";
                    String nameOfDocument = document.select(nameOfDocumentCssQuery).text();
                    //证书编号
                    String numberCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(2)";
                    String number = document.select(numberCssQuery).text();
                    //发行日期
                    String issueDateCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(3)";
                    String issueDate = document.select(issueDateCssQuery).text();
                    //到期时间
                    String expireDateCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(4)";
                    String expireDate = document.select(expireDateCssQuery).text();
                    //签发地点
                    String placeOfIssueCssQuery = "#certificates-" + dataId + " > div:nth-child(5) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(5)";
                    String placeOfIssue = document.select(placeOfIssueCssQuery).text();
                    //将这些信息写入对象
                    Documents documents = new Documents(nameOfDocument, number, issueDate, expireDate, placeOfIssue);
                    //将这个对象加入list集合中
                    documentsArrayList.add(documents);
                }
                String documentsJson = JSON.toJSONString(documentsArrayList, SerializerFeature.WriteMapNullValue);//将所有的证书通过fastjson转换为json对象
                //sea services
                String seaServicesCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) tr";
                Elements seaServicesElements = document.select(seaServicesCssQuery);
                ArrayList<SeaServices> seaServicesArrayList = new ArrayList<SeaServices>();//新建一个存储出海经历的list集合
                for (int i = 1; i <= seaServicesElements.size(); i++) {
                    //船名
                    String vesselNameCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(1)";
                    String vesselName = document.select(vesselNameCssQuery).text();
                    //旗帜
                    String flagCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(2)";
                    String flag = document.select(flagCssQuery).text();
                    //发动机类型
                    String engineTypeCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(3)";
                    String engineType = document.select(engineTypeCssQuery).text();
                    //职级
                    String rankCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(4)";
                    String rank = document.select(rankCssQuery).text();
                    //经理/船员代理
                    String managerCrewAgentCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(5)";
                    String managerCrewAgent = document.select(managerCrewAgentCssQuery).text();
                    //开始时间
                    String fromCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(6)";
                    String from = document.select(fromCssQuery).text();
                    //结束时间
                    String toCssQuery = "#seaservices-" + dataId + " > div:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(" + i + ") > td:nth-child(7)";
                    String to = document.select(toCssQuery).text();
                    //将这些信息写入对象
                    SeaServices seaServices = new SeaServices(vesselName, flag, engineType, rank, managerCrewAgent, from, to);
                    //将这些信息加入到list集合中
                    seaServicesArrayList.add(seaServices);
                }
                String seaServicesJson = JSON.toJSONString(seaServicesArrayList, SerializerFeature.WriteMapNullValue);//将所有的出海经历通过fastjson转换为json对象
                //lastUpdate
                String lastUpdateCssQuery = "div.w_all_wrap[data-id=" + dataId + "] > div:nth-child(1) > div:nth-child(4) > div:nth-child(2) > div:nth-child(1) > div:nth-child(1) > div:nth-child(3) > b:nth-child(1)";
                String lastUpdate = document.select(lastUpdateCssQuery).text();
                //8.创建SeafarersModel模型类对象
                Seafarers newSeafarer = new Seafarers(id, name, age, appliedFor, alternative, availability, salary, citizenship, nationality, english, usVisa, schengenVisa, nearestAirport, vesselTypes, engineTypesAndModels, lastKnownContract, coc, endorsement, documentsJson, seaServicesJson, lastUpdate);
                //9.使用数据层接口的方法将数据存入数据库
                try {
                    seafarersDao.insert(newSeafarer);//因为id的值唯一，因此当数据重复时可能会报错，因此在这里捕获异常
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //四、与表现层进行交互，向前端显示数据
    /* 1.分页条件查询
     * 参数：第currentPage页，每页有pageSize个信息，模型类对象
     * 返回值：page对象 */
    @Override
    public IPage<Seafarers> getPage(int currentPage, int pageSize, Seafarers seafarers) {
        //1.创建模糊条件查询对象
        QueryWrapper<Seafarers> qw = new QueryWrapper<Seafarers>();
        //2.添加查询条件，在这里使用船员的 姓名、任职、国籍 作为模糊查询条件
        qw.like(Strings.isNotEmpty(seafarers.getName()), "name", seafarers.getName());
        qw.like(Strings.isNotEmpty(seafarers.getAppliedFor()), "applied_for", seafarers.getAppliedFor());
        qw.like(Strings.isNotEmpty(seafarers.getCitizenship()), "citizenship", seafarers.getCitizenship());
        qw.orderByDesc("to_date(last_update,'DD.MM.YYYY')");//按最近更新时间排序，将最近更新的拍在前面
        //3.创建分页查询page对象，将当前页面和页面大小参数传入
        IPage page = new Page(currentPage, pageSize);
        //4.将分页和条件查询传入，在数据库中搜索相应的数据
        seafarersDao.selectPage(page, qw);
        return page;
    }
}

