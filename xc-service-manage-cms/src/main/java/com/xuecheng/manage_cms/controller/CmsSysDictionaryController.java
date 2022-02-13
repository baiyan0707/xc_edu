package com.xuecheng.manage_cms.controller;

import com.xuecheng.api.cms.CmsSysDictionaryControllerApi;
import com.xuecheng.framework.domain.system.SysDictionary;
import com.xuecheng.manage_cms.dao.CmsSysDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sys/dictionary")
public class CmsSysDictionaryController implements CmsSysDictionaryControllerApi {

    @Autowired
    CmsSysDictionaryRepository cmsSysDictionaryRepository;

    @Override
    @GetMapping("/get/{type}")
    public SysDictionary courseGrade(@PathVariable("type") String type) {
        return cmsSysDictionaryRepository.findByDType(type);
    }
}
