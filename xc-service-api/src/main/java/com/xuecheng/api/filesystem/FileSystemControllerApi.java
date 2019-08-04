package com.xuecheng.api.filesystem;

import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;

@Api(value = "图片管理接口", description = "图片管理接口，提供图片的管理、查询接口")
public interface FileSystemControllerApi {


    /**
     * multipartFile 文件
     * businesskey 业务key
     * filetag 业务标签
     * metadata 文件元信息 采用json格式 转为Map 集合
     */
    @ApiOperation("图片上传")
    UploadFileResult upload(MultipartFile multipartFile, String businesskey, String filetag, String metadata);
}
