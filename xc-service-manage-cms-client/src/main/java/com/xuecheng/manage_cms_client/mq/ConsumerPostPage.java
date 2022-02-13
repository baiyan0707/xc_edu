package com.xuecheng.manage_cms_client.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.manage_cms_client.service.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConsumerPostPage {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerPostPage.class);

    @Autowired
    SiteService siteService;

    //监听队列
    @RabbitListener(queues = {"${xuecheng.mq.queue}"})
    public void postPage(String msg) {
        //解析消息
        Map map = JSON.parseObject(msg, Map.class);
        //得到消息中的页面id
        String pageId = (String) map.get("pageId");
        //调用方法 判断是否合法
        CmsPage cmsPage = siteService.findCmsPageById(pageId);
        if (cmsPage == null) {
            logger.error("receive postpage msg,cmsPage is null,pageId:{}", pageId);
            return;
        }
        //调用service方法将页面从GridFs中下载到服务器
        siteService.savePageToServerPath(pageId);
    }
}
