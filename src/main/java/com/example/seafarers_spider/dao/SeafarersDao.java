//数据库接口，通过模型类对数据库进行操作，使用mybatis-plus简化开发
package com.example.seafarers_spider.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seafarers_spider.models.Seafarers;
import org.apache.ibatis.annotations.Mapper;

@Mapper//使数据库SQL映射被容器识别到
public interface SeafarersDao extends BaseMapper<Seafarers> {
    //使用mybatis-plus简化开发，继承自BaseMapper方法，容器类型选择Seafarers模型类
    //BaseMapper中包含了大量的内置数据库操作方法
}
