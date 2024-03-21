package com.xjbg.log.collector.starter.example;

import com.xjbg.log.collector.annotation.CollectorLog;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.starter.example.feign.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author kesc
 * @since 2023-04-14 15:46
 */
public class UserController {

    @Autowired
    UserService userService;
    @Autowired(required = false)
    private UserFeignClient userFeignClient;

    @PostMapping
    @CollectorLog.Ignore
    public boolean userInfo(@RequestBody UserInfo userInfo) {
        userService.addUser(userInfo);
        return true;
    }

    @PostMapping(value = "/user-feign")
    public boolean userInfoFeign(@RequestBody UserInfo userInfo) {
        userFeignClient.userInfo(userInfo);
        return true;
    }

    @PostMapping(value = "/log-info")
    @CollectorLog.Ignore
    public boolean userInfoFeign(@RequestBody LogInfo logInfo) {
        System.out.println(logInfo);
        return true;
    }

    @PostMapping(value = "/file")
    public boolean file(@RequestParam("file") MultipartFile file) {
        System.out.println(file.getOriginalFilename());
        return true;
    }

    @GetMapping(value = "/download")
    public void file(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getOutputStream().write("<html>1234</html>".getBytes());
    }

    @GetMapping("/hello")
    public Mono<String> hello() {
        return userService.hello(new ArrayList<>(), Mono.just("Hello World!"));
    }

}
