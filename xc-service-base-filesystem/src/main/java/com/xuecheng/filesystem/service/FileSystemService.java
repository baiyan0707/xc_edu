package com.xuecheng.filesystem.service;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xuecheng.filesystem.dao.FileSystemRepository;
import com.xuecheng.framework.domain.filesystem.FileSystem;
import com.xuecheng.framework.domain.filesystem.response.FileSystemCode;
import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


@Service
public class FileSystemService {

    @Value("${xuecheng.fastdfs.tracker_servers}")
    String tracker_servers;
    @Value("${xuecheng.fastdfs.charset}")
    String charset;
    @Value("${xuecheng.fastdfs.network_timeout_in_seconds}")
    int network_timeout_in_seconds;
    @Value("${xuecheng.fastdfs.connect_timeout_in_seconds}")
    int connect_timeout_in_seconds;
    @Autowired
    FileSystemRepository fileSystemRepository;


    public UploadFileResult upload(MultipartFile multipartFile, String businesskey, String filetag, String metadata) {
        if (multipartFile == null) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_FILEISNULL);
        }
        //获取到文件传输后返回的id
        String fileId = fdfs_upload(multipartFile);
        if (StringUtils.isEmpty(fileId)) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_FILEISNULL);
        }
        FileSystem fileSystem = new FileSystem();
        //赋值
        fileSystem.setFileId(fileId);
        fileSystem.setFilePath(fileId);
        fileSystem.setBusinesskey(businesskey);
        fileSystem.setFiletag(filetag);
        fileSystem.setFileName(multipartFile.getOriginalFilename());
        fileSystem.setFileType(multipartFile.getContentType());
        //json 转为map数据 保存
        if (StringUtils.isEmpty(metadata)) {
            Map map = JSONObject.parseObject(metadata, Map.class);
            fileSystem.setMetadata(map);
        }
        fileSystemRepository.save(fileSystem);
        return new UploadFileResult(CommonCode.SUCCESS, fileSystem);
    }


    //初始化fdfs的环境
    private void initFdfsConfig() {
        try {
            ClientGlobal.initByTrackers(tracker_servers);
            ClientGlobal.setG_charset(charset);
            ClientGlobal.setG_network_timeout(network_timeout_in_seconds);
            ClientGlobal.setG_connect_timeout(connect_timeout_in_seconds);
        } catch (Exception e) {
            e.printStackTrace();
            //环境初始化异常
            ExceptionCast.cast(FileSystemCode.FS_INITFDFSERROR);
        }
    }

    //上传文件到fdfs，返回文件id
    private String fdfs_upload(MultipartFile file) {
        try {
            //初始化fdfs的环境
            initFdfsConfig();
            //获取到客户端 用于请求trackerServer
            TrackerClient trackerClient = new TrackerClient();
            //连接服务器,跟踪服务器
            TrackerServer trackerServer = trackerClient.getConnection();
            //获取storeStorage 服务器
            StorageServer storeStorage = trackerClient.getStoreStorage(trackerServer);
            //获取到storageClient1 服务器
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, storeStorage);
            byte[] bytes = file.getBytes();
            //上传文件
            String fileId = storageClient1.upload_file1(bytes, file.getOriginalFilename(), null);
            return fileId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
