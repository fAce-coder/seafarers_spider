//表现层返回结果的模型类,规范数据返回格式
package com.example.seafarers_spider.controller.utils;

import lombok.Data;


@Data
public class R {

    //1.成员变量
    private Boolean flag;//标志位，表示操作是否成功
    private Object data;//后端数据
    private String msg;//出现错误时的bug信息

    //2.构造方法，几种不同的构造方法用于不同数据的返回
    public R() {//无参构造方法
    }

    public R(Boolean flag) {//只返回标志位，用于插入等没有返回值的操作
        this.flag = flag;
    }

    public R(String msg) {//只返回错误信息，用于出现错误时的信息返回
        this.msg = msg;
    }

    public R(Boolean flag, Object data) {//返回标志位和数据，用于查询数据等带有数据返回值的操作
        this.flag = flag;
        this.data = data;
    }
}
