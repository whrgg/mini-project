package cn.edu.hut.wx.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // 设置单个文件最大大小
        factory.setMaxFileSize(DataSize.ofMegabytes(20000));
        // 设置一次请求中所有文件的总大小最大限制
        factory.setMaxRequestSize(DataSize.ofMegabytes(20000));
        return factory.createMultipartConfig();
    }
}
