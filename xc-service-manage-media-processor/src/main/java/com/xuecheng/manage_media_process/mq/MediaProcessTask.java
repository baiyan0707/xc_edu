package com.xuecheng.manage_media_process.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.MediaFileProcess_m3u8;
import com.xuecheng.framework.utils.HlsVideoUtil;
import com.xuecheng.framework.utils.Mp4VideoUtil;
import com.xuecheng.manage_media_process.dao.MediaFileRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@SuppressWarnings("all")
public class MediaProcessTask {

    //ffmpeg绝对路径
    @Value("${xc-service-manage-media.ffmpeg-path}")
    String ffmpeg_path;

    //上传文件根目录
    @Value("${xc-service-manage-media.video-location}")
    String serverPath;

    @Autowired
    MediaFileRepository mediaFileRepository;

    /**
     * 接受视频处理消息,进行视频处理
     * connectionFactory 在mq类声明了一个消费者Bean对象,用来处理高并发
     * @param msg
     */
    @RabbitListener(queues = "${xc-service-manage-media.mq.queue-media-video-processor}",containerFactory = "customContainerFactory")
    public void receiveMediaProcessTask(String msg) {
        //解析消息内容,得到id
        Map map = JSON.parseObject(msg, Map.class);
        String mediaId = (String) map.get("mediaId");
        //根据id获取文本信息
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        //如果没有内容直接终止
        if (!optional.isPresent()) {
            return;
        }
        MediaFile mediaFile = optional.get();
        //获取到后缀
        String fileType = mediaFile.getFileType();
        //如果非avi视频则不处理
        if (!fileType.equals("avi") || fileType == null) {
            mediaFile.setProcessStatus("303004");
            mediaFileRepository.save(mediaFile);
            return;
        } else {
            //处理中
            mediaFile.setProcessStatus("303001");
            mediaFileRepository.save(mediaFile);
        }

        //生成map4文件
        //String ffmpeg_path, String video_path, String mp4_name, String mp4folder_path
        String video_path = serverPath + mediaFile.getFilePath() + mediaFile.getFileName(); //要处理的视频的路径
        String mp4_name = mediaFile.getFileId() + ".mp4"; //视频名称
        String mp4folder_path = serverPath + mediaFile.getFilePath(); //接收目录
        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpeg_path, video_path, mp4_name, mp4folder_path);
        //接受处理结果
        String result = mp4VideoUtil.generateMp4();
        if (result == null || !result.equals("success")) {
            //操作失败
            mediaFile.setProcessStatus("303003"); //失败
            //创建一个失败记录
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            //记录失败原因
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        //转为m3u8和ts文件
        //String ffmpeg_path, String video_path, String m3u8_name,String m3u8folder_path
        //mp4文件路径
        String mp4_video_path = serverPath + mediaFile.getFilePath() + mp4_name;
        //m3u8文件名称
        String m3u8_name = mediaFile.getFileId() + ".m3u8";
        //m3u8文件所在路径
        String m3u8folder_path = serverPath + mediaFile.getFilePath() + "hls/";
        HlsVideoUtil hlsVideoUtil = new HlsVideoUtil(ffmpeg_path, mp4_video_path, m3u8_name, m3u8folder_path);
        //接受结果
        String tsResult = hlsVideoUtil.generateM3u8();
        if (tsResult == null || !tsResult.equals("success")) {
            //操作失败
            mediaFile.setProcessStatus("303003"); //失败
            //创建一个失败记录
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            //记录失败原因
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        //成功
        mediaFile.setProcessStatus("303002");
        //获取m3u8列表
        List<String> ts_list = hlsVideoUtil.get_ts_list();
        MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
        mediaFileProcess_m3u8.setTslist(ts_list);
        mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        //获取到m3u8的路径
        String m3u8_path = mediaFile.getFilePath() + "hls/" + m3u8_name;
        mediaFile.setFileUrl(m3u8_path);
        //保存
        mediaFileRepository.save(mediaFile);
    }
}
