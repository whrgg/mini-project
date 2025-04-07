package cn.edu.hut.wx.controller;

import org.apache.commons.io.FilenameUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

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
        String chunkFilename = chunkNumber+"_temp";
        Path chunkPath = Paths.get(tempDir, chunkFilename);
        Files.write(chunkPath, file.getBytes(), StandardOpenOption.CREATE);

        // 如果是最后一个分片，合并文件
        if (chunkNumber == totalChunks - 1) {
            mergeFiles(identifier, file.getOriginalFilename(), totalChunks);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("chunkNumber", chunkNumber);
        return response;
    }

    private void mergeFiles(String identifier, String originalFilename, int totalChunks) throws IOException {
        // 创建合并后的文件
        Path mergedFile = Paths.get(UPLOAD_DIR, identifier.substring(0,identifier.lastIndexOf("-")));
        Files.createFile(mergedFile);

        // 合并所有分片
        for (int i = 0; i < totalChunks; i++) {
            String chunkFilename = i + "_" + "temp";
            Path chunkPath = Paths.get(UPLOAD_DIR + File.separator + identifier, chunkFilename);
            byte[] bytes = Files.readAllBytes(chunkPath);
            Files.write(mergedFile, bytes, StandardOpenOption.APPEND);

            // 删除分片文件
            Files.delete(chunkPath);
        }

        // 删除临时目录
        Files.delete(Paths.get(UPLOAD_DIR + File.separator + identifier));
    }
}
