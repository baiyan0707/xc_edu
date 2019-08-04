package com.xuecheng.order.service;


import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.order.dao.XcTaskHisRepository;
import com.xuecheng.order.dao.XcTaskRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    XcTaskRepository xcTaskRepository;

    @Autowired
    RabbitTemplate template;

    @Autowired
    XcTaskHisRepository xcTaskHisRepository;

    /**
     * 取出前n条任务,取出指定时间之前处理的任务
     * @param updateTime
     * @param size
     * @return
     */
    public List<XcTask> findTaskList(Date updateTime, int size) {
        //设置分页
        Pageable pageable = new PageRequest(0, size);

        Page<XcTask> taskPage = xcTaskRepository.findByUpdateTimeBefore(pageable, updateTime);
        return taskPage.getContent();
    }

    /**
     * 1.1 查询任务
     * @param ex 交换机名称
     * @param routerKey routerKey名称
     * @param xcTask  数据库实体对象
     */
    public void publish(String ex, String routerKey, XcTask xcTask) {
        Optional<XcTask> optional = xcTaskRepository.findById(xcTask.getId());
        if (optional.isPresent()) {
            //向mq发送队列消息
            template.convertAndSend(ex, routerKey, xcTask);
            XcTask task = optional.get();
            //更新任务时间为当前时间
            task.setUpdateTime(new Date());
            xcTaskRepository.save(task);
        }
    }

    /**
     * 获取任务
     * @param id
     * @param version
     * @return
     */
    @Transactional
    public int updateTaskVersion(String id,Integer version){
        int count = xcTaskRepository.updateTaskVersion(id, version);
        return count;
    }

    /**
     *  3.1 删除任务
     */
    @Transactional
    public void finishTask(String taskId){
        Optional<XcTask> optional = xcTaskRepository.findById(taskId);
        if(optional.isPresent()){
            //当前任务
            XcTask xcTask = optional.get();
            xcTask.setDeleteTime(new Date());
            //历史任务
            XcTaskHis xcTaskHis = new XcTaskHis();
            BeanUtils.copyProperties(xcTask,xcTaskHis);
            xcTaskHisRepository.save(xcTaskHis);
            xcTaskRepository.delete(xcTask);
        }
    }
}
