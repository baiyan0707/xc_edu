package com.xuecheng.manage_cms.service;

import com.xuecheng.framework.domain.system.SysDictionary;
import com.xuecheng.manage_cms.dao.CmsSysDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class SysDictionaryService {

    @Autowired
    CmsSysDictionaryRepository cmsSysDictionaryRepository;

    /**
     * 获取课程等级
     * @param type
     * @return
     */
    public SysDictionary findCourseGrade(String type) {
        SysDictionary dictionary = cmsSysDictionaryRepository.findByDType(type);
        return dictionary;
    }
}
