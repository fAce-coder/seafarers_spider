//海员信息数据库模型类,相当于数据库中的表
package com.example.seafarers_spider.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//使用lombok简化模型类开发
@Data//使用lombok完成模型类中的所有set和get等方法
@NoArgsConstructor//使用lombok完成模型类中无参构造方法
@AllArgsConstructor//使用lombok完成模型类中带参构造方法
public class Seafarers {

    //数据库字段
    //1.id,主键
    private Integer id;
    //2.姓名
    private String name;
    //3.年龄
    private String age;
    //4.任职
    private String appliedFor;
    //5.可选择
    private String alternative;
    //6.可用性（到期时间）
    private String availability;
    //7.工资（月薪）
    private String salary;
    //8.国籍
    private String citizenship;
    //9.民族
    private String nationality;
    //10.英语等级
    private String english;
    //11.美国签证
    private String usVisa;
    //12.申根签证
    private String schengenVisa;
    //13.最近的机场
    private String nearestAirport;
    //14.船舶类型
    private String vesselTypes;
    //15.引擎类型和型号
    private String engineTypesAndModels;
    //16.最后已知合同
    private String lastKnownContract;
    //17.完工证明书（Certification of Completion)
    private String coc;
    //18.背书
    private String endorsement;
    //19.证书（证书名，证书编号，发行日期，到期时间，签发地点）在数据库中是json类型
    private String documents;
    //20.出海经历（船只，旗帜，发动机类型，职级，经理/船员代理，开始时间，结束时间）在数据库中是json类型
    private String seaServices;
    //21.信息最后更新时间
    private String lastUpdate;
}
