//作为springMVC的异常处理器，当前后端出现联调异常时，返回异常信息
package com.example.seafarers_spider.controller.utils;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProjectExceptionAdvice {

    //1.定义一个方法,参数就是所要处理的异常种类,用这个方法来拦截所有的异常信息
    @ExceptionHandler
    public R doException(Exception ex) {
        //2.在控制台输出异常信息
        ex.printStackTrace();
        //3.返回一个R对象
        return new R("服务器故障,请稍后再试!");
    }
}
