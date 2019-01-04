package com.ultrapower.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by dell on 2018/12/26.
 */
@RestController
public class UserController {

    @RequestMapping("/hello")
    public String showHello(){
        return "hello,SpringBoot";
    }
}
