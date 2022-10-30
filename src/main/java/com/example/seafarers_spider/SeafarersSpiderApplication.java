package com.example.seafarers_spider;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.seafarers_spider.dao.SeafarersDao;
import com.example.seafarers_spider.models.Seafarers;
import com.example.seafarers_spider.service.ISeafarersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class SeafarersSpiderApplication {

    //导入数据层接口和业务层接口
    @Autowired
    private SeafarersDao itSeafarersDao;
    @Autowired
    private ISeafarersService itISeafarersService;
    //因为静态方法中只能调用静态成员变量，但静态成员变量无法通过@Autowired导入,因此需要以下操作
    private static SeafarersDao seafarersDao;
    private static ISeafarersService iSeafarersService;//重新创建一个同对象的static变量
    //写一个@PostConstruct注解的方法，在这个方法里，初始化刚才创建的static变量，将之前注入的对象赋值给它
    @PostConstruct
    public void init(){
        seafarersDao = itSeafarersDao;
        iSeafarersService = itISeafarersService;
    }

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(SeafarersSpiderApplication.class, args);

        //1.全量式查询，如果系统是第一次启动，即数据库中一条数据都没有，则进行全量式复制
        QueryWrapper<Seafarers> qw = new QueryWrapper<Seafarers>();
        if (seafarersDao.selectCount(qw) == 0){//数据库中没有信息，则进行一次全量式爬虫
            System.out.println("已经开始全量式爬虫");
            iSeafarersService.pageDown("https://www.balticshipping.com/seafarers");
            iSeafarersService.analysis();
        }else {
            //2.增量式爬虫，只要系统启动，就每隔一段时间进行一次网页增量式爬取和解析，保证数据库数据是最新的
            int i = 1;//==========================================
            while (true){
                System.out.println("已经开始第" + i + "次增量式爬虫");//=========================
                i++;//===================================
                //1.增量式爬取，将页面新增的，数据库中没有的数据抓取到本地文件中
                iSeafarersService.updatePageDown("https://www.balticshipping.com/seafarers");
                //2.从本地文件中解析数据，并将数据写入数据库中
                iSeafarersService.updateAnalysis();
                //3.爬取解析结束后，进行下一次循环之前，进行休眠，实现每隔一段时间进行一次增量式抓取，可以设置为1天或者8小时等
                TimeUnit.HOURS.sleep(8);
            }
        }

    }

}
