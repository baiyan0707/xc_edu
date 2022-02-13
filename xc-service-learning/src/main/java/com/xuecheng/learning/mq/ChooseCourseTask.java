package com.xuecheng.learning.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.learning.config.RabbitMQConfig;
import com.xuecheng.learning.service.LearningService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Component
public class ChooseCourseTask {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    LearningService learningService;

    /**
     * 2.传递队列名称，发送到mq
     * @param xcTask
     */
    @RabbitListener(queues = {RabbitMQConfig.XC_LEARNING_ADDCHOOSECOURSE})
    public void receiveChoosecourseTask(XcTask xcTask) {
        try {
            String requestBody = xcTask.getRequestBody();
            Map map = JSON.parseObject(requestBody, Map.class);
            //获取到数据库中的数据
            //String userId, String courseId, Date startTime, Date endTime, XcTask xcTask
            String userId = (String) map.get("userId");
            String courseId = (String) map.get("courseId");
            //String的时间转换为时间格式的
            SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY‐MM‐dd HH:mm:ss");
            Date startTime = null;
            Date endTime = null;
            if (map.get("startTime") != null) {
                startTime = dateFormat.parse((String) map.get("startTime"));
            }
            if (map.get("endTime") != null) {
                endTime = dateFormat.parse((String) map.get("endTime"));
            }
            //传递所有参数，返回一个结果
            ResponseResult responseResult = learningService.addcourse(userId, courseId, startTime, endTime, xcTask);
                if (responseResult.isSuccess()) {
                // 2.1 发送信息给mq 交换机，routerKey
                rabbitTemplate.convertAndSend(RabbitMQConfig.EX_LEARNING_ADDCHOOSECOURSE,RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE_KEY,xcTask);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
