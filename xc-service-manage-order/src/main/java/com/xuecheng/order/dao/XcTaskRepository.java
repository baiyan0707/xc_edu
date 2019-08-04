package com.xuecheng.order.dao;

import com.xuecheng.framework.domain.task.XcTask;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;

public interface XcTaskRepository extends JpaRepository<XcTask, String> {

    Page<XcTask> findByUpdateTimeBefore(Pageable pageable, Date updateTime);

    //更新任务处理时间
    @Modifying
    @Query("update XcTask t set t.updateTime = :updateTime where t.id = :id")
    int updateTaskTime(@Param(value = "id") String id, @Param(value = "updateTime") Date updateTime);

    //使用乐观锁
    @Modifying
    @Query("update XcTask t set t.version = :version + 1 where t.id = :id and t.version = :version")
    int updateTaskVersion(@Param(value = "id") String id, @Param(value = "version") Integer version);
}
