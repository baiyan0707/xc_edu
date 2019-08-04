package com.xuecheng.api.course;

import com.xuecheng.framework.domain.cms.response.CoursePublishResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CategoryNode;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PathVariable;

@Api(value = "课程管理接口", description = "课程管理接口，提供课程的管理、查询接口")
public interface CourseControllerApi {

    @ApiOperation("课程计划查询")
    TeachplanNode findTeachplanList(String courseId);

    @ApiOperation("课程计划添加")
    ResponseResult addTeachplan(Teachplan teachplan);

    @ApiOperation("我的课程查询")
    QueryResponseResult<CourseInfo> findAllCourse(Integer page, Integer size, CourseListRequest courseListRequest);

    @ApiOperation("课程级别")
    CategoryNode fndAllClassify();

    @ApiOperation("添加课程")
    AddCourseResult addCourse(CourseBase courseBase);

    @ApiOperation("根据页面id获得页面信息")
    CourseBase getByCourseId(String courseId) throws RuntimeException;

    @ApiOperation("修改课程")
    AddCourseResult updateCourse(String courseId,CourseBase courseBase);

    @ApiOperation("根据id查询营销信息")
    CourseMarket getCourseMarketById(String courseId);

    @ApiOperation("更新页面营销信息")
    CourseMarket updateCourseMarket(String id,CourseMarket courseMarket);

    @ApiOperation("添加课程图片")
    ResponseResult addCoursePic(String courseId,String pic);

    @ApiOperation("查询课程图片")
    CoursePic findByPicId(String courseId);

    @ApiOperation("删除课程图片")
    ResponseResult deletePic(String courseId);

    @ApiOperation("课程视图查询")
    CourseView courseview(String id);

    @ApiOperation("预览课程")
    CoursePublishResult preview(String id);

    @ApiOperation("发布课程")
    CoursePublishResult publish(String id);

    @ApiOperation("保存课程计划与媒资文件关联")
    ResponseResult savemedia(TeachplanMedia teachplanMedia);
}
