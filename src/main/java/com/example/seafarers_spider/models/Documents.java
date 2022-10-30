/* 证书对象类 */
package com.example.seafarers_spider.models;

import com.alibaba.fastjson.annotation.JSONType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data//get和set等方法
@NoArgsConstructor//无参构造方法
@AllArgsConstructor//带参构造方法
@JSONType(orders = {"nameOfDocument", "number", "issueDate", "expireDate", "placeOfIssue"})//定义fastjson的序列化顺序
public class Documents {
    //1.证书名
    private String nameOfDocument;
    //2.证书编号
    private String number;
    //3.发行日期
    private String issueDate;
    //4.到期日期
    private String expireDate;
    //5.签发地点
    private String placeOfIssue;
}
