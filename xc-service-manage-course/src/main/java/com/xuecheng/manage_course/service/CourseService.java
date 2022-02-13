package com.xuecheng.manage_course.service;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.cms.response.CoursePublishResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CategoryNode;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.domain.course.response.TeachplanMediaPub;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanRepository teachplanRepository;

    @Autowired
    CourseBaseRepository courseBaseRepository;

    @Autowired
    CourseMapper courseMapper;

    @Autowired
    CategoryMapper categoryMapper;

    @Autowired
    CoursePicRepository coursePicRepository;

    @Autowired
    CourseMarketRepository courseMarketRepository;

    @Autowired
    CmsPageClient cmsPageClient;

    @Autowired
    CoursePubRepository coursePubRepository;

    @Autowired
    TeachplanMediaRepository teachplanMediaRepository;

    @Autowired
    TeachplanMediaPubRepository teachplanMediaPubRepository;

    @Value("${course-publish.dataUrlPre}")
    private String publish_dataUrlPre;
    @Value("${course-publish.pagePhysicalPath}")
    private String publish_page_physicalpath;
    @Value("${course-publish.pageWebPath}")
    private String publish_page_webpath;
    @Value("${course-publish.siteId}")
    private String publish_siteId;
    @Value("${course-publish.templateId}")
    private String publish_templateId;
    @Value("${course-publish.previewUrl}")
    private String previewUrl;


    /**
     * 查询课程信息
     * @param courseId
     * @return
     */
    public TeachplanNode findTeachplanList(String courseId) {
        return teachplanMapper.selectList(courseId);
    }

    /**
     * 保存课程信息
     * @param teachplan
     * @return
     */
    @Transactional
    public ResponseResult addTeachplan(Teachplan teachplan) {
        if (teachplan == null || StringUtils.isEmpty(teachplan.getCourseid()) || StringUtils.isEmpty(teachplan.getPname())) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //获取到节点
        String courseid = teachplan.getCourseid();
        //获取到父节点
        String parentid = teachplan.getParentid();
        if (StringUtils.isEmpty(parentid)) {
            //获取课程的根结点
            parentid = this.getTeachplanRoot(courseid);
        }
        //查询根结点信息
        Optional<Teachplan> optional = teachplanRepository.findById(parentid);
        Teachplan teachplan1 = optional.get();
        //父结点的级别
        String parent_grade = teachplan1.getGrade();
        //创建一个新结点准备添加
        Teachplan newTeachplan = new Teachplan();
        //将teachplan的属性拷贝到teachplanNew中
        BeanUtils.copyProperties(teachplan, newTeachplan);
        //要设置必要的属性
        newTeachplan.setParentid(parentid);
        if (parent_grade.equals("1")) {
            newTeachplan.setGrade("2");
        } else {
            newTeachplan.setGrade("3");
        }
        newTeachplan.setStatus("0");
        teachplanRepository.save(newTeachplan);
        //成功
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //获取课程的根结点
    private String getTeachplanRoot(String courseId) {
        Teachplan teachplan = new Teachplan();
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (!optional.isPresent()) {
            return null;
        }
        CourseBase courseBase = optional.get();
        //调用dao查询teachplan表得到该课程的根结点（一级结点）
        List<Teachplan> teachplanList = teachplanRepository.findByCourseidAndParentid(courseId, "0");
        //新添加一个课程的根结点
        if (teachplanList == null || teachplanList.size() == 0) {
            teachplan.setCourseid(courseId);
            teachplan.setParentid("0");
            teachplan.setGrade("1");
            teachplan.setPname(courseBase.getName());
            teachplanRepository.save(teachplan);
            return teachplan.getId();
        }
        //返回根结点的id
        return teachplanList.get(0).getId();
    }

    /**
     * 课程查询
     *
     * @param companyId
     * @param page
     * @param size
     * @param courseListRequest
     * @return
     */
    public QueryResponseResult<CourseInfo> findAll(String companyId, Integer page, Integer size, CourseListRequest courseListRequest) {
        if (page == null && page == 0) {
            page = 1;
        }
        if (size == null && size == 0) {
            size = 7;
        }
        if (courseListRequest == null) {
            courseListRequest = new CourseListRequest();
        }
        //分页
        PageHelper.startPage(page, size);
        //企业id
        courseListRequest.setCompanyId(companyId);
        //获取到分页后的数据
        Page<CourseInfo> infoPage = courseMapper.findAllCoursePage(courseListRequest);
        QueryResult<CourseInfo> queryResult = new QueryResult<>();
        List<CourseInfo> result = infoPage.getResult();
        long total = infoPage.getTotal();
        queryResult.setList(result);
        queryResult.setTotal(total);
        return new QueryResponseResult<CourseInfo>(CommonCode.SUCCESS, queryResult);
    }

    /**
     * 课程级别
     * @return
     */
    public CategoryNode fndAllClassify() {
        return categoryMapper.fndAllClassify();
    }

    /**
     * 课程添加
     * @param courseBase
     * @return
     */
    @Transactional
    public AddCourseResult addCourse(CourseBase courseBase) {
        courseBaseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS, courseBase);
    }

    /**
     * 更新课程信息
     * @param courseId
     * @param courseBase
     * @return
     */
    public AddCourseResult updateCourse(String courseId, CourseBase courseBase) {
        CourseBase base = this.findByCourseId(courseId);
        if (base == null) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        base.setName(courseBase.getName());
        base.setUsers(courseBase.getUsers());
        base.setMt(courseBase.getMt());
        base.setGrade(courseBase.getGrade());
        base.setStudymodel(courseBase.getStudymodel());
        base.setTeachmode(courseBase.getTeachmode());
        base.setDescription(courseBase.getDescription());
//        base.setSt(courseBase.getSt());
        base.setStatus(courseBase.getStatus());
        base.setCompanyId(courseBase.getCompanyId());
        base.setUserId(courseBase.getUserId());
        courseBaseRepository.save(base);
        return new AddCourseResult(CommonCode.SUCCESS, base);
    }

    /**
     * 获取到课程信息
     * @param courseId
     * @return
     */
    public CourseBase findByCourseId(String courseId) {
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    /**
     * 向课程管理系统添加课程和图片的关联
     * @param courseId
     * @param pic
     * @return
     */
    @Transactional
    public ResponseResult addCourseAndPic(String courseId, String pic) {
        CoursePic coursePic = null;
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        //判断是否已存在
        if (optional.isPresent()) {
            coursePic = optional.get();
        }
        if (coursePic == null) {
            coursePic = new CoursePic();
        }
        coursePic.setPic(pic);
        coursePic.setCourseid(courseId);
        coursePicRepository.save(coursePic);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 查询课程图片
     * @param courseId
     * @return
     */
    public CoursePic findByPicId(String courseId) {
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        if (optional.isPresent()) {
            CoursePic coursePic = optional.get();
            return coursePic;
        }
        return null;
    }

    /**
     * 删除课程图片
     * @param courseId
     * @return
     */
    @Transactional
    public ResponseResult deleteCoursePic(String courseId) {
        //返回一个影响行数,大于0则成功 反之失败
        long result = coursePicRepository.deleteByCourseid(courseId);
        if (result > 0) {
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    /**
     * 课程视图查询
     * @param id
     * @return
     */
    public CourseView getCoruseView(String id) {
        CourseView courseView = new CourseView();
        //查询课程图片信息
        Optional<CoursePic> picOptional = coursePicRepository.findById(id);
        if (picOptional.isPresent()) {
            courseView.setCoursePic(picOptional.get());
        }
        //获取课程基本信息
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(id);
        if (baseOptional.isPresent()) {
            courseView.setCourseBase(baseOptional.get());
        }
        //获取营销信息
        Optional<CourseMarket> marketOptional = courseMarketRepository.findById(id);
        if (marketOptional.isPresent()) {
            courseView.setCourseMarket(marketOptional.get());
        }
        //获取课程计划
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        courseView.setTeachplanNode(teachplanNode);
        return courseView;
    }

    //根据id获取课程信息
    private CourseBase findCourseBaseById(String courseId) {
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(courseId);
        if (baseOptional.isPresent()) {
            CourseBase courseBase = baseOptional.get();
            return courseBase;
        }
        ExceptionCast.cast(CourseCode.COURSE_DENIED_DELETE);
        return null;
    }


    /**
     * 课程预览
     * @param id
     * @return
     */
    @SuppressWarnings("all")
    public CoursePublishResult preview(String id) {
        CourseBase courseBase = this.findCourseBaseById(id);
        //发布课程预览页面
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);//站点id
        cmsPage.setDataUrl(publish_dataUrlPre + id);//数据模型url
        cmsPage.setPageName(id + ".html");//页面名称
        cmsPage.setPageAliase(courseBase.getName());//页面别名，就是课程名称
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);//页面物理路径
        cmsPage.setPageWebPath(publish_page_webpath);//页面webpath
        cmsPage.setTemplateId(publish_templateId);//页面模板id

        //远程请求cms保存页面信息
        CmsPageResult pageResult = cmsPageClient.save(cmsPage);
        if (!pageResult.isSuccess()) {
            return new CoursePublishResult(CommonCode.FAIL, null);
        }
        //页面id
        String pageId = pageResult.getCmsPage().getPageId();
        //页面url
        String url = previewUrl + pageId;
        return new CoursePublishResult(CommonCode.SUCCESS, url);
    }

    /**
     * 课程发布
     * @param id
     * @return
     */
    @Transactional
    @SuppressWarnings("all")
    public CoursePublishResult publish(String id) {
        //获取到课程信息
        CourseBase courseBase = this.findCourseBaseById(id);
        //准备页面
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);//站点id
        cmsPage.setDataUrl(publish_dataUrlPre + id);//数据模型url
        cmsPage.setPageName(id + ".html");//页面名称
        cmsPage.setPageAliase(courseBase.getName());//页面别名，就是课程名称
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);//页面物理路径
        cmsPage.setPageWebPath(publish_page_webpath);//页面webpath
        cmsPage.setTemplateId(publish_templateId);//页面模板id
        //调用cms发布页面到服务器
        CmsPostPageResult postPageQuick = cmsPageClient.postPageQuick(cmsPage);
        if (!postPageQuick.isSuccess()) {
            return new CoursePublishResult(CommonCode.FAIL, null);
        }
        //修改状态
        CourseBase base = this.saveCoursePubState(id);
        if (base == null) {
            return new CoursePublishResult(CommonCode.FAIL, null);
        }

        //创建一个coursePub
        CoursePub coursePub = createCoursePub(id);
        //向数据库保存课程索引信息
        CoursePub newCorsePub = saveCoursePub(id, coursePub);
        if(newCorsePub == null){
            //创建索引失败
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_CREATE_INDEX_ERROR);
        }

        saveTeachplanMediaPub(id);
        //cms发布页面会传递回一个url
        String url = postPageQuick.getPageUrl();
        return new CoursePublishResult(CommonCode.SUCCESS, url);
    }

    //保存课程计划媒资信息
    private void saveTeachplanMediaPub(String courseId){
        List<TeachplanMedia> mediaList = teachplanMediaRepository.findByCourseId(courseId);
        //将课程计划媒资信息存储待索引表
        teachplanMediaPubRepository.deleteByCourseId(courseId);
        //创建集合,接收数据
        List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();
        for (TeachplanMedia teachplanMedia : mediaList) {
            TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
            //数据拷贝
            BeanUtils.copyProperties(teachplanMedia,teachplanMediaPub);
            teachplanMediaPubList.add(teachplanMediaPub);
        }
        teachplanMediaPubRepository.saveAll(teachplanMediaPubList);
    }

    //修改课程状态
    private CourseBase saveCoursePubState(String courseId) {
        CourseBase courseBase = this.findByCourseId(courseId);
        //更新发布状态为已发布
        courseBase.setStatus("202002");
        courseBaseRepository.save(courseBase);
        return courseBase;
    }

    //创建一个CoursePub
    private CoursePub createCoursePub(String id) {
        CoursePub coursePub = new CoursePub();
        //基础信息
        Optional<CourseBase> optional = courseBaseRepository.findById(id);
        if (optional.isPresent()) {
            CourseBase courseBase = optional.get();
            BeanUtils.copyProperties(courseBase, coursePub);
        }
        //课程图片
        Optional<CoursePic> picOptional = coursePicRepository.findById(id);
        if (picOptional.isPresent()) {
            CoursePic coursePic = picOptional.get();
            BeanUtils.copyProperties(coursePic, coursePub);
        }
        //课程营销信息
        Optional<CourseMarket> marketOptional = courseMarketRepository.findById(id);
        if (marketOptional.isPresent()) {
            CourseMarket courseMarket = marketOptional.get();
            BeanUtils.copyProperties(courseMarket, coursePub);
        }
        //课程计划
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        String jsonString = JSON.toJSONString(teachplanNode);
        coursePub.setTeachplan(jsonString);
        return coursePub;
    }

    //保存CoursePub
    private CoursePub saveCoursePub(String id, CoursePub coursePub) {
        if (StringUtils.isEmpty(id)) {
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_COURSEIDISNULL);
        }
        CoursePub newCoursePub = null;
        Optional<CoursePub> coursePubOptional = coursePubRepository.findById(id);
        if (coursePubOptional.isPresent()) {
            newCoursePub = coursePubOptional.get();
        } else {
            newCoursePub = new CoursePub();
        }
        //拷贝
        BeanUtils.copyProperties(coursePub, newCoursePub);

        //设置必须参数
        newCoursePub.setId(id);
        //更新时间戳为最新时间
//        newCoursePub.setTimestamp(new Date());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy‐MM‐dd HH:mm:ss");
        String date = simpleDateFormat.format(new Date());
        //发布时间
        newCoursePub.setPubTime(date);
        coursePubRepository.save(newCoursePub);
        return newCoursePub;
    }

    /**
     * 保存课程计划与媒资文件的关联信息
     * @param teachplanMedia 课程和媒资中间表
     * @return
     */
    public ResponseResult savemedia(TeachplanMedia teachplanMedia) {
        if(teachplanMedia == null || StringUtils.isEmpty(teachplanMedia.getTeachplanId())){
            //非法参数
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //获取到课程页面
        String teachplanId = teachplanMedia.getTeachplanId();
        Optional<Teachplan> optional = teachplanRepository.findById(teachplanId);
        if(!optional.isPresent()){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //查询到教学计划
        Teachplan teachplan = optional.get();
        String grade = teachplan.getGrade();
        if(StringUtils.isEmpty(grade) || !grade.equals("3")){
            //只允许选择第三级的课程计划关联视频
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_GRADEERROR);
        }
        //获取到课程和媒资所关联的表
        Optional<TeachplanMedia> mediaOptional = teachplanMediaRepository.findById(teachplanId);
        TeachplanMedia media = null;
        if(!mediaOptional.isPresent()){
            media = new TeachplanMedia();
        }else {
            media = mediaOptional.get();
        }
        //保存到数据库
        media.setCourseId(teachplanMedia.getCourseId()); //课程id
        media.setTeachplanId(teachplanId); //课程计划id
        media.setMediaUrl(teachplanMedia.getMediaUrl()); //媒资文件访问地址
        media.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName()); //媒资文件原始名称
        media.setMediaId(teachplanMedia.getMediaId()); //媒资文件id
        teachplanMediaRepository.save(media);

        return new ResponseResult(CommonCode.SUCCESS);
    }
}
