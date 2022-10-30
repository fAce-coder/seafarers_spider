//表现层，与前端进行交互
package com.example.seafarers_spider.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.seafarers_spider.controller.utils.R;
import com.example.seafarers_spider.models.Seafarers;
import com.example.seafarers_spider.service.ISeafarersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController//基于Restful设计模式,并将该类加载为表现层类
@RequestMapping("/seafarers")//规定在url中输入格式
public class RSeafarersController {

    //导入业务层接口对象
    @Autowired
    private ISeafarersService iSeafarersService;

    /* 1.查询所有数据，用get请求，url：http://localhost:8080/seafarers
     * 参数：无
     * 返回值：R对象，包含标志位flag和数据data */
    @GetMapping
    public R getAll() {
        return new R(true, iSeafarersService.list());
    }

    /* 2.查询单个数据操作，用get请求,url：http://localhost:8080/seafarers/{id}
     * 参数：船员id（从查询字符串中取出）
     * 返回值：R对象，包含标志位flag和数据data */
    @GetMapping("{id}")
    public R getById(@PathVariable Integer id) {
        return new R(true, iSeafarersService.getById(id));
    }

    /* 3.分页条件查询,用get请求,url：http://localhost:8080/seafarers/{currentPage}/{pageSize}
     * 参数：第currentPage页（从查询字符串中取出），每页有pageSize个信息（从查询字符串中取出），模型类对象
     * 返回值：R对象，包含标志位flag和数据data*/
    @GetMapping("{currentPage}/{pageSize}")
    public R getPage(@PathVariable int currentPage, @PathVariable int pageSize, Seafarers seafarers) {
        IPage<Seafarers> page = iSeafarersService.getPage(currentPage, pageSize, seafarers);
        //如果当前页码值大于了总页码值,那么重新执行查询操作,使用最大页码值作为当前页码值
        if (currentPage > page.getPages()) {
            page = iSeafarersService.getPage((int) page.getPages(), pageSize, seafarers);
        }
        return new R(true, page);
    }

    /* 4.添加数据，用post请求
     * 参数：Seafarers模型类对象(通过请求体传json数据形式的参数进来)
     * 返回值：R对象，只包含标志位flag */
    @PostMapping
    public R save(@RequestBody Seafarers seafarers) {
        //返回是否添加成功
        return new R(iSeafarersService.save(seafarers));
    }

    /* 5.更新，用put请求
     * 参数：Seafarers模型类对象(通过请求体传json数据形式的参数进来)
     * 返回值：R对象，只包含标志位flag */
    @PutMapping
    public R update(@RequestBody Seafarers seafarers) {
        //返回是否更新成功
        return new R(iSeafarersService.updateById(seafarers));
    }

    /* 6.删除操作，用delete操作
     * 参数：船员id（从查询字符串中取出)
     * 返回值：R对象，只包含标志位flag */
    @DeleteMapping("{id}")
    public R delete(@PathVariable Integer id) {
        //返回是否删除成功
        return new R(iSeafarersService.removeById(id));
    }

    /* 7.展示证书信息，用get请求
     * 参数：船员id（从查询字符串中取出)
     * 返回值：R对象，包含标志位flag和数据data */
    @GetMapping("documents/{id}")
    public R showDocuments(@PathVariable Integer id) {
        return new R(true, iSeafarersService.getById(id).getDocuments());
    }

    /* 7.展示出海经历信息，用get请求
     * 参数：船员id（从查询字符串中取出)
     * 返回值：R对象，包含标志位flag和数据data */
    @GetMapping("seaServices/{id}")
    public R showSeaServices(@PathVariable Integer id) {
        return new R(true, iSeafarersService.getById(id).getSeaServices());
    }
}
