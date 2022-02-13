package com.xuecheng.manage_media;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class testFile {

    @Test
    //测试文件分块方法
    public void testChunk() throws IOException {
        File sourceFile = new File("h:\\lucene.avi");
        String chunkPath = "G:\\ffmpeg\\chunk\\";
        File chunkFolder = new File(chunkPath);
        if (!chunkFolder.exists()) {
            chunkFolder.mkdirs();
        }
        //分块大小
        long chunkSize = 1024 * 1024 * 1;
        //分块数量
        long chunkNum = (long) Math.ceil(sourceFile.length() * 1.0 / chunkSize);

        //缓冲区大小
        byte[] bys = new byte[1024];
        //使用RandomAccessFile访问文件
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile, "r");
        //分块
        for (long i = 0; i < chunkNum; i++) {
            //创建分块文件
            File file = new File(chunkPath + i);
            boolean fileNewFile = file.createNewFile();
            if (fileNewFile) {
                //写数据
                RandomAccessFile raf_write = new RandomAccessFile(file, "rw");
                int len;
                while ((len = raf_read.read(bys)) != -1) {
                    raf_write.write(bys, 0, len);
                    //如果传输大于1M 则写下一块
                    if (file.length() >= chunkSize) {
                        break;
                    }
                }
                raf_write.close();
            }
        }
        raf_read.close();
    }

    @Test
    //文件合并
    public void testMerge() throws IOException {
        //块文件目录对象
        File chunkFileFolder = new File("g:\\ffmpeg\\chunk\\");
        //转为list进行排序
        File[] files = chunkFileFolder.listFiles();
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                //比较排序按照升序排序
                if (Integer.parseInt(f1.getName()) > Integer.parseInt(f2.getName())) {
                    return 1;
                }
                return -1;
            }
        });
        //合并文件要放置的位置
        File mergeFile = new File("h:\\lucene_merge.avi");
        //常见新文件
        boolean newFile = mergeFile.createNewFile();
        //创建写对象
        RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");
        for (File file : fileList) {
            //创建一个读文件的对象
            RandomAccessFile raf_read = new RandomAccessFile(file, "r");
            byte[] bys = new byte[1024];
            int len;
            while ((len = raf_read.read(bys)) != -1) {
                raf_write.write(bys,0,len);
            }
            raf_read.close();
        }
        raf_write.close();
    }
}
