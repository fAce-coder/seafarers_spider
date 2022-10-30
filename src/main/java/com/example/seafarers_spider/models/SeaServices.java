/* 出海经历对象类 */
package com.example.seafarers_spider.models;

import com.alibaba.fastjson.annotation.JSONType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data//get和set等方法
@NoArgsConstructor//无参构造方法
@AllArgsConstructor//带参构造方法
@JSONType(orders = {"vesselName", "flag", "engineType", "rank", "managerCrewAgent", "from", "to"})
public class SeaServices {
    //1.船名
    private String vesselName;
    //2.旗帜
    private String flag;
    //3.发动机类型
    private String engineType;
    //4.职级
    private String rank;
    //5.经理/船员代理
    private String managerCrewAgent;
    //6.开始时间
    private String from;
    //7.结束时间
    private String to;
}
