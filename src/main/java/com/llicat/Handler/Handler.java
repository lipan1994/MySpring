package com.llicat.Handler;

import java.lang.reflect.Method;

/**
 * Copyright: Copyright (c) 2020
 *
 * @ClassName: com.llicat.Handler.Handler
 * @Description: 用来映射
 * @version: v1.0.0
 * @author: lipan
 * @date: 2020/10/7 22:13
 * <p>
 * Modification History:
 * Date         Author          Version            Description
 * ------------------------------------------------------------
 * 2020/10/7      lipan          v1.0.0               修改原因
 */

public class Handler {


   private Method method;

   private Object controller;



    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }
}
