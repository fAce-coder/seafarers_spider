//拦截器，用于分页查询
package com.example.seafarers_spider.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MPConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        //创建一个拦截器的壳的对象,拦截器壳中可以放具体的拦截器
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        //添加内部的具体的拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        //将拦截器壳对象return出去即可
        return interceptor;
    }
}
