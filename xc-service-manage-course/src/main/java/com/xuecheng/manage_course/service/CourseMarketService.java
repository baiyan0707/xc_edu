package com.xuecheng.manage_course.service;

import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.course.CourseMarket;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.manage_course.dao.CourseMarketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CourseMarketService {

    @Autowired
    CourseMarketRepository courseMarketRepository;

    @Transactional
    public CourseMarket updateCourseMarket(String id, CourseMarket courseMarket) {
        CourseMarket market = this.findByMarketId(id);
        if (courseMarket == null) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        //保存信息
        market.setId(courseMarket.getId());
        market.setCharge(courseMarket.getCharge());
        market.setValid(courseMarket.getValid());
        market.setStartTime(courseMarket.getStartTime());
        market.setEndTime(courseMarket.getEndTime());
        market.setPrice(courseMarket.getPrice());
        market.setQq(courseMarket.getQq());
        market.setPrice_old(courseMarket.getPrice_old());
        courseMarketRepository.save(market);
        return market;
    }

    public CourseMarket findByMarketId(String courseId) {
        //根据id获取到页面信息
        Optional<CourseMarket> optional = courseMarketRepository.findById(courseId);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }
}
