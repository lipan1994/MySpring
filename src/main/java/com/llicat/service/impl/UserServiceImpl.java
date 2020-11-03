package com.llicat.service.impl;

import com.llicat.annotation.MyService;
import com.llicat.service.IUserService;

/**
 * Copyright: Copyright (c) 2020
 *
 * @ClassName: com.llicat.service.impl.UserServiceImpl
 * @Description: 该类的功能描述
 * @version: v1.0.0
 * @author: lipan
 * @date: 2020/10/7 22:19
 * <p>
 * Modification History:
 * Date         Author          Version            Description
 * ------------------------------------------------------------
 * 2020/10/7      lipan          v1.0.0               修改原因
 */
@MyService
public class UserServiceImpl implements IUserService {

    @Override
   public void sayhaha(Integer name){
        System.out.println(name+" 哈哈");
    }
}
