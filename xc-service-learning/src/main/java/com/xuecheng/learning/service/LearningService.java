package com.xuecheng.learning.service;

import com.xuecheng.framework.domain.course.response.TeachplanMediaPub;
import com.xuecheng.framework.domain.learning.XcLearningCourse;
import com.xuecheng.framework.domain.learning.request.GetMediaResult;
import com.xuecheng.framework.domain.learning.request.LearningCode;
import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.learning.client.CourseSearchClient;
import com.xuecheng.learning.dao.XcLearningCourseRepository;
import com.xuecheng.learning.dao.XcTaskHisRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;


@Service
public class LearningService {

    @Autowired
    XcLearningCourseRepository xcLearningCourseRepository;

    @Autowired
    XcTaskHisRepository xcTaskHisRepository;

    @Autowired
    CourseSearchClient courseSearchClient;

    /**
     * 请求视频路径
     * @param courseId
     * @param teachplanId
     * @return
     */
    public GetMediaResult getmedia(String courseId, String teachplanId) {
        TeachplanMediaPub mediaPub = courseSearchClient.getmedia(teachplanId);
        if (mediaPub == null || StringUtils.isEmpty(mediaPub.getMediaUrl())) {
            //获取视频地址失败
            ExceptionCast.cast(LearningCode.LEARNING_GETMEDIA_ERROR);
        }
        return new GetMediaResult(CommonCode.SUCCESS, mediaPub.getMediaUrl());
    }

    /**
     * 完成选课
     * @param userId    用户id
     * @param courseId  课程id
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param xcTask    对象内容
     * @return 是否成功
     */
    public ResponseResult addcourse(String userId, String courseId, Date startTime, Date endTime, XcTask xcTask) {
        if (StringUtils.isEmpty(courseId)) {
            ExceptionCast.cast(LearningCode.LEARNING_GETMEDIA_ERROR);
        }
        if (StringUtils.isEmpty(userId)) {
            ExceptionCast.cast(LearningCode.CHOOSECOURSE_USERISNULL);
        }
        if (xcTask == null || StringUtils.isEmpty(xcTask.getId())) {
            ExceptionCast.cast(LearningCode.CHOOSECOURSE_TASKISNULL);
        }

        XcLearningCourse xcLearningCourse = xcLearningCourseRepository.findByUserIdAndUserId(userId, courseId);
        if (xcLearningCourse != null) {
            //更新
            xcLearningCourse.setStartTime(startTime);
            xcLearningCourse.setEndTime(endTime);
            xcLearningCourse.setStatus("501001");
            xcLearningCourseRepository.save(xcLearningCourse);
        } else {
            //添加
            xcLearningCourse = new XcLearningCourse();
            xcLearningCourse.setUserId(userId);
            xcLearningCourse.setCourseId(courseId);
            xcLearningCourse.setStartTime(startTime);
            xcLearningCourse.setEndTime(endTime);
            xcLearningCourse.setStatus("501001");
            xcLearningCourseRepository.save(xcLearningCourse);
        }
        //向历史任务表添加数据
        Optional<XcTaskHis> optional = xcTaskHisRepository.findById(userId);
        if(!optional.isPresent()){
            //添加历史任务
            XcTaskHis xcTaskHis = new XcTaskHis();
            BeanUtils.copyProperties(xcTask,xcTaskHis);
            xcTaskHisRepository.save(xcTaskHis);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
