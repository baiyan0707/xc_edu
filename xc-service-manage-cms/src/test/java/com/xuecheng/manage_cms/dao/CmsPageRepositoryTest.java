package com.xuecheng.manage_cms.dao;


import com.xuecheng.framework.domain.cms.CmsPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

@SpringBootTest
@RunWith(SpringRunner.class)
public class CmsPageRepositoryTest {

    @Autowired
    CmsPageRepository cmsPageRepository;

    /**
     * 分页测试
     */
    @Test
    public void findAllTest() {
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<CmsPage> pages = cmsPageRepository.findAll(pageable);
        System.out.println(pages);
    }

    /**
     * 查询所有
     */
    @Test
    public void findAll() {
        List<CmsPage> all = cmsPageRepository.findAll();
        System.out.println(all);
    }

    /**
     * 修改
     */
    @Test
    public void update() {
        //查询数据 返回一个optional容器对象
        Optional<CmsPage> optional = cmsPageRepository.findById("5ad94b9168db5243ec846e8e");
        //判断是否为空
        if (optional.isPresent()) {
            //获取到具体值
            CmsPage cmsPage = optional.get();
            //修改别名
            cmsPage.setPageAliase("");
            cmsPageRepository.save(cmsPage);
        }
    }

    /**
     * 条件查询
     */
    @Test
    public void findAllByExample(){
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        //条件值对象
        CmsPage cmsPage = new CmsPage();
        //要查询的id
        cmsPage.setTemplateId("5aec5dd70e661808240ab7a6");
        //条件匹配器
        ExampleMatcher matcher = ExampleMatcher.matching();
        //定义Example
        Example<CmsPage> example = Example.of(cmsPage,matcher);
        Page<CmsPage> pages = cmsPageRepository.findAll(example,pageable);
        List<CmsPage> content = pages.getContent();
        System.out.println(content);
    }
}
