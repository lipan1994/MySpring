package com.llicat.controller;

import com.llicat.annotation.MyAutowired;
import com.llicat.annotation.MyController;
import com.llicat.annotation.MyRequestParam;
import com.llicat.annotation.MyRequstMapping;
import com.llicat.service.IUserService;

/**
 * Copyright: Copyright (c) 2020
 *
 * @ClassName: com.llicat.controller.UserController
 * @Description: 该类的功能描述
 * @version: v1.0.0
 * @author: lipan
 * @date: 2020/10/7 22:20
 * <p>
 * Modification History:
 * Date         Author          Version            Description
 * ------------------------------------------------------------
 * 2020/10/7      lipan          v1.0.0               修改原因
 */

@MyController
@MyRequstMapping("/user")
public class UserController {

    @MyAutowired
    IUserService userService;

    @MyRequstMapping("/hello")
    public String hello(@MyRequestParam(name = "name") int [] name){
        for (Integer s : name) {
            userService.sayhaha(s);

        }
        return "";

    }

}
