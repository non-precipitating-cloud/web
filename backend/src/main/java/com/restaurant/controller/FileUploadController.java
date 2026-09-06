package com.restaurant.controller;

import com.restaurant.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api")
public class FileUploadController {

    /**
     * 上传文件（头像）
     * POST /api/upload
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.fail("文件为空");
        }

        // 获取原始扩展名
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        // 生成唯一文件名
        Long userId = (Long) request.getAttribute("userId");
        String fileName = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        // 使用 user.dir 作为基础路径（运行 mvn spring-boot:run 时即 backend/ 目录）
        String basePath = System.getProperty("user.dir");
        File dir = new File(basePath, "uploads");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 保存文件
        try {
            File dest = new File(dir, fileName);
            file.transferTo(dest);
        } catch (IOException e) {
            return Result.fail("文件上传失败: " + e.getMessage());
        }

        // 返回访问路径
        String url = "/uploads/" + fileName;
        return Result.ok("上传成功", url);
    }
}
