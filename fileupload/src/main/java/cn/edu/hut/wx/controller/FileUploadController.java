package cn.edu.hut.wx.controller;

import cn.hutool.core.date.StopWatch;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/file")
public class FileUploadController {
    private static final String UPLOAD_DIR = "F:\\javaex\\mini-project\\fileupload\\file\\";

    @CrossOrigin(origins = "*")
    @PostMapping("/fileupload")
    public Map<String, Object> uploadChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("chunkNumber") int chunkNumber,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("identifier") String identifier) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 确保上传目录存在
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 为每个图片创建临时目录
        String tempDir = UPLOAD_DIR + File.separator + identifier;
        Files.createDirectories(Paths.get(tempDir));

        String extension = FilenameUtils.getExtension(file.getName());

        // 保存分片文件
        String chunkFilename = chunkNumber + "_temp";
        Path chunkPath = Paths.get(tempDir, chunkFilename);
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(chunkPath.toFile(), "rw")) {
            randomAccessFile.write(file.getBytes());
        }

        // 更新conf文件记录当前切片位置
        updateConfFile(identifier, chunkNumber, totalChunks);

        // 如果是最后一个分片，合并文件
        if (chunkNumber == totalChunks - 1) {
            mergeFiles(identifier, file.getOriginalFilename(), totalChunks);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("chunkNumber", chunkNumber);
        stopWatch.stop();
        System.out.println("controller用时为:" + stopWatch.getTotalTimeMillis());
        return response;
    }

    // 更新conf文件记录当前切片位置
    private void updateConfFile(String identifier, int chunkNumber, int totalChunks) throws IOException {
        String tempDir = UPLOAD_DIR + File.separator + identifier;
        Path confPath = Paths.get(tempDir, "upload.conf");
        List<String> lines = new ArrayList<>();
        lines.add("totalChunks=" + totalChunks);
        lines.add("lastChunk=" + chunkNumber);
        Files.write(confPath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // 合并文件
    private void mergeFiles(String identifier, String originalFilename, int totalChunks) throws IOException {
        // 创建合并后的文件
        Path mergedFile = Paths.get(UPLOAD_DIR, identifier.substring(0, identifier.lastIndexOf("-")));
        try {
            Files.createFile(mergedFile);
        } catch (IOException e) {
            // 如果文件已存在，可选择继续操作或抛出异常
            System.err.println("合并文件已存在，继续操作。");
        }
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(mergedFile.toFile(), "rw")) {
            // 合并所有分片
            for (int i = 0; i < totalChunks; i++) {
                String chunkFilename = i + "_" + "temp";
                Path chunkPath = Paths.get(UPLOAD_DIR + File.separator + identifier, chunkFilename);
                byte[] bytes = Files.readAllBytes(chunkPath);
                randomAccessFile.seek(randomAccessFile.length());
                randomAccessFile.write(bytes);
                // 删除分片文件
                Files.delete(chunkPath);
            }
            Files.delete(Paths.get(UPLOAD_DIR + File.separator + identifier,"upload.conf"));
        }
        // 删除临时目录
        Files.delete(Paths.get(UPLOAD_DIR + File.separator + identifier));
    }

    // 检查上传状态
    @GetMapping("/upload-status")
    @CrossOrigin(origins = "*")
    public Map<String, Object> checkUploadStatus(@RequestParam String identifier) {
        Map<String, Object> response = new HashMap<>();
        String tempDir = UPLOAD_DIR + File.separator + identifier;
        Path confPath = Paths.get(tempDir, "upload.conf");
        try {
            if (Files.exists(confPath)) {
                List<String> lines = Files.readAllLines(confPath);
                int totalChunks = 0;
                int lastChunk = -1;
                for (String line : lines) {
                    if (line.startsWith("totalChunks=")) {
                        totalChunks = Integer.parseInt(line.substring("totalChunks=".length()));
                    } else if (line.startsWith("lastChunk=")) {
                        lastChunk = Integer.parseInt(line.substring("lastChunk=".length()));
                    }
                }
                List<Integer> uploadedChunks = new ArrayList<>();
                for (int i = 0; i <= lastChunk; i++) {
                    uploadedChunks.add(i);
                }
                response.put("uploadedChunks", uploadedChunks);
                response.put("totalChunks", totalChunks);
            } else {
                response.put("uploadedChunks", new ArrayList<>());
                response.put("totalChunks", 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            response.put("uploadedChunks", new ArrayList<>());
            response.put("totalChunks", 0);
        }
        System.out.println("当前文件数："+response.get("totalChunks"));
        return response;
    }
}