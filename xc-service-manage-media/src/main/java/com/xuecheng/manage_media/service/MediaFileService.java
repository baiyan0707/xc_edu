package com.xuecheng.manage_media.service;

import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.request.QueryMediaFileRequest;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MediaFileService {

    @Autowired
    MediaFileRepository mediaFileRepository;

    /**
     *我的媒资分页查询
     * @param page 页数
     * @param size 每页的记录数
     * @param queryMediaFileRequest 查询的参数
     * @return
     */
    public QueryResponseResult<MediaFile> findList(int page, int size, QueryMediaFileRequest queryMediaFileRequest) {
        MediaFile mediaFile = new MediaFile();
        if(queryMediaFileRequest == null){
            queryMediaFileRequest = new QueryMediaFileRequest();
        }
        //拼装条件值对象
        if(StringUtils.isNotEmpty(queryMediaFileRequest.getFileOriginalName())){
            mediaFile.setFileOriginalName(queryMediaFileRequest.getFileOriginalName());
        }
        if(StringUtils.isNotEmpty(queryMediaFileRequest.getTag())){
            mediaFile.setTag(queryMediaFileRequest.getTag());
        }
        if(StringUtils.isNotEmpty(queryMediaFileRequest.getProcessStatus())){
            mediaFile.setProcessStatus(queryMediaFileRequest.getProcessStatus());
        }
        //查询条件匹配器,使用模糊查询,状态码不设置,默认为精确查询
        ExampleMatcher matcher = ExampleMatcher.matching().withMatcher("tag",ExampleMatcher.GenericPropertyMatchers.contains())
                                 .withMatcher("fileOriginalName",ExampleMatcher.GenericPropertyMatchers.contains());

        //定义Example对象
        Example<MediaFile> example = Example.of(mediaFile,matcher);

        //分页设置
        if(page <= 0){
            page = 1;
        }
        page = page - 1;
        if(size <= 0){
            size = 7;
        }
        Pageable pageable = new PageRequest(page,size);

        //分页查询
        Page<MediaFile> files = mediaFileRepository.findAll(example, pageable);
        //总数量
        long totals = files.getTotalElements();
        List<MediaFile> content = files.getContent();
        //创建对象,传递参数
        QueryResult<MediaFile> queryResult = new QueryResult<>();
        queryResult.setList(content);
        queryResult.setTotal(totals);

        return new QueryResponseResult(CommonCode.SUCCESS,queryResult);
    }
}
