//业务层接口，使用mybatis-plus的快速开发模式
package com.example.seafarers_spider.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.seafarers_spider.models.Seafarers;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public interface ISeafarersService extends IService<Seafarers> {

    //除了使用内置的方法之外，还可以在接口中自定义抽象方法

    //一、爬取数据
    //1.通过Htmlunit工具，获取列表页的HtmlPage对象
    public abstract HtmlPage htmlunitPageDown(WebClient webClient, String url);

    //2.模拟点击分类标签，获取含有10个船员信息的首页
    public abstract HtmlPage homePageClick(WebClient webClient, HtmlPage htmlPage, String dataId);

    //3.模拟点击，获取所有的船员信息的页面
    public abstract HtmlPage completePageClick(WebClient webClient, HtmlPage htmlPage);

    //4.获取页面信息，并将页面的所有信息加载到本地文件中的 主方法
    public abstract void pageDown(String url);

    //二、解析数据
    //1.解析一个文件
    public abstract void analyze(String filePath);

    //2.解析所有文件 主方法
    public abstract void analysis();

    //三、增量爬虫，定时更新数据
    //1.模拟点击搜索，获取含有10个船员信息的首页
    public abstract HtmlPage updateHomePageClick(WebClient webClient, HtmlPage htmlPage);

    //2.模拟点击，获取最新的船员信息页面
    public abstract HtmlPage updateCompletePageDown(WebClient webClient, HtmlPage htmlPage);

    //3.获取页面信息，并将最新船员信息的页面的信息加载到本地文件中 主方法
    public abstract void updatePageDown(String url);

    //4.解析最新船员页面信息，并将其加载到数据库中 主方法
    public abstract void updateAnalysis();

    //四、与表现层进行交互，向前端显示数据
    //1.分页条件查询
    public abstract IPage<Seafarers> getPage(int currentPage, int pageSize, Seafarers seafarers);
}
