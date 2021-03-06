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
     * ??????????????????
     * @param courseId
     * @return
     */
    public TeachplanNode findTeachplanList(String courseId) {
        return teachplanMapper.selectList(courseId);
    }

    /**
     * ??????????????????
     * @param teachplan
     * @return
     */
    @Transactional
    public ResponseResult addTeachplan(Teachplan teachplan) {
        if (teachplan == null || StringUtils.isEmpty(teachplan.getCourseid()) || StringUtils.isEmpty(teachplan.getPname())) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //???????????????
        String courseid = teachplan.getCourseid();
        //??????????????????
        String parentid = teachplan.getParentid();
        if (StringUtils.isEmpty(parentid)) {
            //????????????????????????
            parentid = this.getTeachplanRoot(courseid);
        }
        //?????????????????????
        Optional<Teachplan> optional = teachplanRepository.findById(parentid);
        Teachplan teachplan1 = optional.get();
        //??????????????????
        String parent_grade = teachplan1.getGrade();
        //?????????????????????????????????
        Teachplan newTeachplan = new Teachplan();
        //???teachplan??????????????????teachplanNew???
        BeanUtils.copyProperties(teachplan, newTeachplan);
        //????????????????????????
        newTeachplan.setParentid(parentid);
        if (parent_grade.equals("1")) {
            newTeachplan.setGrade("2");
        } else {
            newTeachplan.setGrade("3");
        }
        newTeachplan.setStatus("0");
        teachplanRepository.save(newTeachplan);
        //??????
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //????????????????????????
    private String getTeachplanRoot(String courseId) {
        Teachplan teachplan = new Teachplan();
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (!optional.isPresent()) {
            return null;
        }
        CourseBase courseBase = optional.get();
        //??????dao??????teachplan????????????????????????????????????????????????
        List<Teachplan> teachplanList = teachplanRepository.findByCourseidAndParentid(courseId, "0");
        //?????????????????????????????????
        if (teachplanList == null || teachplanList.size() == 0) {
            teachplan.setCourseid(courseId);
            teachplan.setParentid("0");
            teachplan.setGrade("1");
            teachplan.setPname(courseBase.getName());
            teachplanRepository.save(teachplan);
            return teachplan.getId();
        }
        //??????????????????id
        return teachplanList.get(0).getId();
    }

    /**
     * ????????????
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
        //??????
        PageHelper.startPage(page, size);
        //??????id
        courseListRequest.setCompanyId(companyId);
        //???????????????????????????
        Page<CourseInfo> infoPage = courseMapper.findAllCoursePage(courseListRequest);
        QueryResult<CourseInfo> queryResult = new QueryResult<>();
        List<CourseInfo> result = infoPage.getResult();
        long total = infoPage.getTotal();
        queryResult.setList(result);
        queryResult.setTotal(total);
        return new QueryResponseResult<CourseInfo>(CommonCode.SUCCESS, queryResult);
    }

    /**
     * ????????????
     * @return
     */
    public CategoryNode fndAllClassify() {
        return categoryMapper.fndAllClassify();
    }

    /**
     * ????????????
     * @param courseBase
     * @return
     */
    @Transactional
    public AddCourseResult addCourse(CourseBase courseBase) {
        courseBaseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS, courseBase);
    }

    /**
     * ??????????????????
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
     * ?????????????????????
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
     * ???????????????????????????????????????????????????
     * @param courseId
     * @param pic
     * @return
     */
    @Transactional
    public ResponseResult addCourseAndPic(String courseId, String pic) {
        CoursePic coursePic = null;
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        //?????????????????????
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
     * ??????????????????
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
     * ??????????????????
     * @param courseId
     * @return
     */
    @Transactional
    public ResponseResult deleteCoursePic(String courseId) {
        //????????????????????????,??????0????????? ????????????
        long result = coursePicRepository.deleteByCourseid(courseId);
        if (result > 0) {
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    /**
     * ??????????????????
     * @param id
     * @return
     */
    public CourseView getCoruseView(String id) {
        CourseView courseView = new CourseView();
        //????????????????????????
        Optional<CoursePic> picOptional = coursePicRepository.findById(id);
        if (picOptional.isPresent()) {
            courseView.setCoursePic(picOptional.get());
        }
        //????????????????????????
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(id);
        if (baseOptional.isPresent()) {
            courseView.setCourseBase(baseOptional.get());
        }
        //??????????????????
        Optional<CourseMarket> marketOptional = courseMarketRepository.findById(id);
        if (marketOptional.isPresent()) {
            courseView.setCourseMarket(marketOptional.get());
        }
        //??????????????????
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        courseView.setTeachplanNode(teachplanNode);
        return courseView;
    }

    //??????id??????????????????
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
     * ????????????
     * @param id
     * @return
     */
    @SuppressWarnings("all")
    public CoursePublishResult preview(String id) {
        CourseBase courseBase = this.findCourseBaseById(id);
        //????????????????????????
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);//??????id
        cmsPage.setDataUrl(publish_dataUrlPre + id);//????????????url
        cmsPage.setPageName(id + ".html");//????????????
        cmsPage.setPageAliase(courseBase.getName());//?????????????????????????????????
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);//??????????????????
        cmsPage.setPageWebPath(publish_page_webpath);//??????webpath
        cmsPage.setTemplateId(publish_templateId);//????????????id

        //????????????cms??????????????????
        CmsPageResult pageResult = cmsPageClient.save(cmsPage);
        if (!pageResult.isSuccess()) {
            return new CoursePublishResult(CommonCode.FAIL, null);
        }
        //??????id
        String pageId = pageResult.getCmsPage().getPageId();
        //??????url
        String url = previewUrl + pageId;
        return new CoursePublishResult(CommonCode.SUCCESS, url);
    }

    /**
     * ????????????
     * @param id
     * @return
     */
    @Transactional
    @SuppressWarnings("all")
    public CoursePublishResult publish(String id) {
        //?????????????????????
        CourseBase courseBase = this.findCourseBaseById(id);
        //????????????
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);//??????id
        cmsPage.setDataUrl(publish_dataUrlPre + id);//????????????url
        cmsPage.setPageName(id + ".html");//????????????
        cmsPage.setPageAliase(courseBase.getName());//?????????????????????????????????
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);//??????????????????
        cmsPage.setPageWebPath(publish_page_webpath);//??????webpath
        cmsPage.setTemplateId(publish_templateId);//????????????id
        //??????cms????????????????????????
        CmsPostPageResult postPageQuick = cmsPageClient.postPageQuick(cmsPage);
        if (!postPageQuick.isSuccess()) {
            return new CoursePublishResult(CommonCode.FAIL, null);
        }
        //????????????
        CourseBase base = this.saveCoursePubState(id);
        if (base == null) {
            return new CoursePublishResult(CommonCode.FAIL, null);
        }

        //????????????coursePub
        CoursePub coursePub = createCoursePub(id);
        //????????????????????????????????????
        CoursePub newCorsePub = saveCoursePub(id, coursePub);
        if(newCorsePub == null){
            //??????????????????
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_CREATE_INDEX_ERROR);
        }

        saveTeachplanMediaPub(id);
        //cms??????????????????????????????url
        String url = postPageQuick.getPageUrl();
        return new CoursePublishResult(CommonCode.SUCCESS, url);
    }

    //??????????????????????????????
    private void saveTeachplanMediaPub(String courseId){
        List<TeachplanMedia> mediaList = teachplanMediaRepository.findByCourseId(courseId);
        //?????????????????????????????????????????????
        teachplanMediaPubRepository.deleteByCourseId(courseId);
        //????????????,????????????
        List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();
        for (TeachplanMedia teachplanMedia : mediaList) {
            TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
            //????????????
            BeanUtils.copyProperties(teachplanMedia,teachplanMediaPub);
            teachplanMediaPubList.add(teachplanMediaPub);
        }
        teachplanMediaPubRepository.saveAll(teachplanMediaPubList);
    }

    //??????????????????
    private CourseBase saveCoursePubState(String courseId) {
        CourseBase courseBase = this.findByCourseId(courseId);
        //??????????????????????????????
        courseBase.setStatus("202002");
        courseBaseRepository.save(courseBase);
        return courseBase;
    }

    //????????????CoursePub
    private CoursePub createCoursePub(String id) {
        CoursePub coursePub = new CoursePub();
        //????????????
        Optional<CourseBase> optional = courseBaseRepository.findById(id);
        if (optional.isPresent()) {
            CourseBase courseBase = optional.get();
            BeanUtils.copyProperties(courseBase, coursePub);
        }
        //????????????
        Optional<CoursePic> picOptional = coursePicRepository.findById(id);
        if (picOptional.isPresent()) {
            CoursePic coursePic = picOptional.get();
            BeanUtils.copyProperties(coursePic, coursePub);
        }
        //??????????????????
        Optional<CourseMarket> marketOptional = courseMarketRepository.findById(id);
        if (marketOptional.isPresent()) {
            CourseMarket courseMarket = marketOptional.get();
            BeanUtils.copyProperties(courseMarket, coursePub);
        }
        //????????????
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        String jsonString = JSON.toJSONString(teachplanNode);
        coursePub.setTeachplan(jsonString);
        return coursePub;
    }

    //??????CoursePub
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
        //??????
        BeanUtils.copyProperties(coursePub, newCoursePub);

        //??????????????????
        newCoursePub.setId(id);
        //??????????????????????????????
//        newCoursePub.setTimestamp(new Date());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy???MM???dd HH:mm:ss");
        String date = simpleDateFormat.format(new Date());
        //????????????
        newCoursePub.setPubTime(date);
        coursePubRepository.save(newCoursePub);
        return newCoursePub;
    }

    /**
     * ????????????????????????????????????????????????
     * @param teachplanMedia ????????????????????????
     * @return
     */
    public ResponseResult savemedia(TeachplanMedia teachplanMedia) {
        if(teachplanMedia == null || StringUtils.isEmpty(teachplanMedia.getTeachplanId())){
            //????????????
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //?????????????????????
        String teachplanId = teachplanMedia.getTeachplanId();
        Optional<Teachplan> optional = teachplanRepository.findById(teachplanId);
        if(!optional.isPresent()){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //?????????????????????
        Teachplan teachplan = optional.get();
        String grade = teachplan.getGrade();
        if(StringUtils.isEmpty(grade) || !grade.equals("3")){
            //???????????????????????????????????????????????????
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_GRADEERROR);
        }
        //???????????????????????????????????????
        Optional<TeachplanMedia> mediaOptional = teachplanMediaRepository.findById(teachplanId);
        TeachplanMedia media = null;
        if(!mediaOptional.isPresent()){
            media = new TeachplanMedia();
        }else {
            media = mediaOptional.get();
        }
        //??????????????????
        media.setCourseId(teachplanMedia.getCourseId()); //??????id
        media.setTeachplanId(teachplanId); //????????????id
        media.setMediaUrl(teachplanMedia.getMediaUrl()); //????????????????????????
        media.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName()); //????????????????????????
        media.setMediaId(teachplanMedia.getMediaId()); //????????????id
        teachplanMediaRepository.save(media);

        return new ResponseResult(CommonCode.SUCCESS);
    }
}
