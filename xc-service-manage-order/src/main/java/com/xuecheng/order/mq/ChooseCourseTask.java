package com.xuecheng.order.mq;

import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.order.config.RabbitMQConfig;
import com.xuecheng.order.service.TaskService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class ChooseCourseTask {

    private static final Logger logger = LoggerFactory.getLogger(ChooseCourseTask.class);

    @Autowired
    TaskService taskService;

    //测试定时发送课程添加
    @Scheduled(cron = "0/3 * * * * *")
    public void sendChoosecourseTask(){
        //获取到时间
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.set(GregorianCalendar.MINUTE,-1);
        Date time = calendar.getTime();
        List<XcTask> taskList = taskService.findTaskList(time, 10);
//        logger.info("send choose course task time:{}",time);
//        System.out.println(taskList);
        //发送选课消息
        for (XcTask xcTask : taskList) {
            //取任务
            if(taskService.updateTaskVersion(xcTask.getId(),xcTask.getVersion()) > 0){
                //1.将任务发送
                taskService.publish(xcTask.getMqExchange(),xcTask.getMqRoutingkey(),xcTask);
                logger.info("send choose course task id:{}",xcTask.getId());
            }
        }
    }

    /**
     *  3.接受选课结果响应
     * @param xcTask
     */
    @RabbitListener(queues = RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE)
    public void receiveFinishChoosecourseTask(XcTask xcTask){
        if(xcTask != null && StringUtils.isNotEmpty(xcTask.getId())){
            taskService.finishTask(xcTask.getId());
        }
    }
//    @Scheduled(fixedDelay = 5000) //上次执行完毕后5秒执行
//    @Scheduled(cron = "0/3 * * * * *") //每3秒执行一次
    public void test01(){
        logger.info("测试任务第一次开启...");
        try {
            Thread.sleep(5000); //休眠5秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("测试任务第一次结束...");
    }

//    @Scheduled(fixedDelay = 5000) //上次执行完毕后5秒执行
//    @Scheduled(cron = "0/3 * * * * *") //每3秒执行一次
    public void test02(){
        logger.info("测试任务第二次开启...");
        try {
            Thread.sleep(5000); //休眠5秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("测试任务第二次结束...");
    }
}
