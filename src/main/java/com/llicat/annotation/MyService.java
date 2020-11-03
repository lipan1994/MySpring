package com.llicat.annotation;

import java.lang.annotation.*;

/**
 * Copyright: Copyright (c) 2020
 *
 * @ClassName: com.llicat.annotation.MyService
 * @Description: 该类的功能描述
 * @version: v1.0.0
 * @author: lipan
 * @date: 2020/10/7 10:20
 * <p>
 * Modification History:
 * Date         Author          Version            Description
 * ------------------------------------------------------------
 * 2020/10/7      lipan          v1.0.0               修改原因
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyService {


    String value() default "";

}
