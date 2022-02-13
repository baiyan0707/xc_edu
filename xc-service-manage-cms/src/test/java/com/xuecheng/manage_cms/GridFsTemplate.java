package com.xuecheng.manage_cms;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@SpringBootTest
@RunWith(SpringRunner.class)

public class GridFsTemplate{

    @Autowired
    org.springframework.data.mongodb.gridfs.GridFsTemplate gridFsTemplate;

    @Autowired
    GridFSBucket gridFSBucket;

    /**
     * 存文件
     * @throws FileNotFoundException
     */
    @Test
    public void gridFsTest() throws FileNotFoundException {
        File file = new File("e:/course.ftl");
        FileInputStream inputStream = new FileInputStream(file);
        ObjectId objectId = gridFsTemplate.store(inputStream, "course.ftl");
        System.out.println(objectId);
    }

    /**
     * 取文件
     */
    @Test
    public void GridFsTest() throws IOException {
        //根据id和value值获取到数据库中的文件
        GridFSFile fsFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is("5d317a048433091b98d8a752")));
        //打开下载流对象
        GridFSDownloadStream stream = gridFSBucket.openDownloadStream(fsFile.getObjectId());
        //创建GridFsResource对象,用于获取流对象
        GridFsResource gridFsResource = new GridFsResource(fsFile, stream);
        //调用工具类,获取流中的数据,使用utf-8编码格式
        String string = IOUtils.toString(gridFsResource.getInputStream(), "utf-8");
        System.out.println(string);
    }
}
