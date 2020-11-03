package com.llicat.servlet;

import com.llicat.Handler.Handler;
import com.llicat.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Copyright: Copyright (c) 2020
 *
 * @ClassName: com.llicat.servlet.MyDispatchServlet
 * @Description: 该类的功能描述
 * @version: v1.0.0
 * @author: lipan
 * @date: 2020/10/7 9:36
 * <p>
 * Modification History:
 * Date         Author          Version            Description
 * ------------------------------------------------------------
 * 2020/10/7      lipan          v1.0.0               修改原因
 */

public class MyDispatchServlet extends HttpServlet {

    //用来解析properties配置文件
    Properties  configloader=new Properties();

    //用来存全限定类名
    List <String> classNamesList=new ArrayList<>();

    //对象容器
    Map<String,Object> iocContainer=new HashMap<>();

    //存放url与调用方法对应关系
    Map<String, Handler> handlerMapping=new HashMap<>();


    //先调用init->super.service->根据调用方式->doget、dopost

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=utf-8");
        try {
            doDispatch(request,response);
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("500 系统内部错误");
        }
    }

    //请求分发逻辑
    private void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception{
        //资源访问相对路径（不带协议、ip、端口）
        String uri = request.getRequestURI();
        //项目路径
        String contextPath = request.getContextPath();

        //去掉项目名 多/ 全部变成单/
        uri=uri.replace(contextPath,"").replaceAll("/+","/");

        if(!handlerMapping.containsKey(uri)){
            response.getWriter().write("404 not found");
            return;
        }
        //需要执行的方法
        Handler handler = handlerMapping.get(uri);
        Method method = handler.getMethod();
        //处理方法的参数
        Map<String, String[]> parameterMap = request.getParameterMap();
        Parameter[] parameters = method.getParameters();
        Object [] paramValues=new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Class<?> parameterType = parameters[i].getType();
            Parameter parameter = parameters[0];
            if(parameterType ==HttpServletRequest.class){
                paramValues[i]=request;
                continue;
            }else if(parameterType==HttpServletResponse.class){
                paramValues[i]=response;
                continue;
            }else{
                MyRequestParam parameterTypeAnnotation = parameter.getAnnotation(MyRequestParam.class);
                String paramName = parameterTypeAnnotation.name();
                //这里逻辑不严密，实际上存在参数没有加注解、或者加注解不设置name，使用形参名字对应url中的name
                if(parameterMap.containsKey(paramName)){
                      paramValues[i]=convert(parameterType,parameterMap.get(paramName));
                }
            }
        }


        Object invoke = method.invoke(handler.getController(), paramValues);
        response.getWriter().write(invoke.toString());

    }


    private  Object convert(Class<?> clz,String [] values){
        //接收到的是数组
        String value = Arrays.toString(values).replaceAll("\\[|\\]", "").replaceAll("\\s", "");
        Object ret=null;
        if(int.class==clz ||Integer.class==clz){
            Integer.parseInt(value);
        }
        if(String.class ==clz){
          ret=value;
        }

        if(Integer[].class==clz || int[].class==clz){
            String[] split = value.split(",");
            List<Integer> list=new ArrayList<>();
            for (int i = 0; i < split.length; i++) {
                list.add(Integer.parseInt(split[i]));
            }

            if(int[].class==clz){
              ret=list.stream().mapToInt(Integer::intValue).toArray();
            }else{
                ret=list.toArray(new Integer[]{});
            }
        }


        //支持数组  url?params1=1,2,3  或者 url?param1=1&param1=2
        if(String[].class ==clz){
            ret=value.split(",");
        }

        //还有 long 、byte 等一些 没有举例了。。。

        return  ret;

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    /**
     * 继承这个方法是为了初始化web.xml中配置的参数
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {

        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.扫描配置文件指定的包
        doScanner(configloader.getProperty("scannerPackage"));

        //3.实例化对象并且添加到容器中
        doInstance();

        //4，自动装配
        doAutowired();

        //5.url与method对应
        initHandlerMapping();



    }

    private void initHandlerMapping() {
        if(iocContainer.isEmpty()){
            return;
        }

        Set<Map.Entry<String, Object>> entries = iocContainer.entrySet();

        for (Map.Entry<String, Object> entry : entries) {
            Class<?> bean = entry.getValue().getClass();
            if(!bean.isAnnotationPresent(MyController.class)){
                continue;
            }

            //处理类上的requestMapping
            String url="";
            if(bean.isAnnotationPresent(MyRequstMapping.class)){
                //有些人喜欢配置  /user 这种 有些配置 user 这种 都是作为一整个controller的资源访问路径
                MyRequstMapping beanAnnotation = bean.getAnnotation(MyRequstMapping.class);
                url="/"+beanAnnotation.value()[0];
            }

            Method[] methods = bean.getDeclaredMethods();
            for (Method method : methods) {
                if(method.isAnnotationPresent(MyRequstMapping.class)){
                    MyRequstMapping methodAnnotation = method.getAnnotation(MyRequstMapping.class);
                    url+="/"+ methodAnnotation.value()[0];
                    Handler handler=new Handler();
                    handler.setMethod(method);
                    handler.setController(entry.getValue());
                    handlerMapping.put(url.replaceAll("/+","/"),handler);
                }
            }

        }

    }


    private void doAutowired() {

        if(iocContainer.isEmpty()){
            return;
        }

        //获取所有key，然后拿到属性看是否有
        Set<Map.Entry<String, Object>> entries = iocContainer.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if(field.isAnnotationPresent(MyAutowired.class)){
                    MyAutowired annotation = field.getAnnotation(MyAutowired.class);
                    boolean required = annotation.required();
                    //接口全限定类名
                    String name = field.getType().getName();
                    if(required){
                        if(iocContainer.get(name)==null){
                            throw  new RuntimeException(" can not inition bean with field"+field.getName());
                        }else{
                            field.setAccessible(true);
                            try {
                                //第一个是操作的对象，第二个是设置的值
                                field.set(entry.getValue(),iocContainer.get(name));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }


                }

            }
        }

    }

    private  String makeFirstLetterToLowerCase(String source){

        char[] chars = source.toCharArray();

        if(chars[0]>='A' && chars[0]<='Z'){
            chars[0]+=32;
        }

        return String.valueOf(chars);



    }


    private void doInstance() {

        if(classNamesList.isEmpty()){

            return;
        }


        for (String className : classNamesList) {
            try {
                Class<?> clz = Class.forName(className);
                if(clz.isAnnotationPresent(MyController.class)){
                    MyController annotation = clz.getAnnotation(MyController.class);
                    //如果自己命名了，使用自己定义的名称
                    String key="";
                    if(annotation.value()!=null && !"".equals(annotation.value())){
                        key=annotation.value();
                    }else {
                        //将类名的首字母小写作为beanId
                        key=makeFirstLetterToLowerCase(clz.getSimpleName());
                    }

                    Object o = clz.newInstance();
                    iocContainer.put(key,o);
                }else if(clz.isAnnotationPresent(MyService.class)){
                    MyService annotation = clz.getAnnotation(MyService.class);
                    //如果自己命名了，使用自己定义的名称
                    String key="";
                    if(annotation.value()!=null && !"".equals(annotation.value())){
                        key=annotation.value();
                    }else {
                        //将类名的首字母小写作为beanId
                        key=makeFirstLetterToLowerCase(clz.getSimpleName());
                    }

                    Object o = clz.newInstance();
                    iocContainer.put(key,o);

                    //仍然需要考虑Autowaired的情况，因为很多时候
                    //  类似于这种 @Autowired
                    // private BooksService booksService; 其实BooksService他是接口，如果根据接口去匹配他有哪些实现类，这不好做
                    //所以可以对这种可能会自动装配到其他bean（service自动装配到controller、dao自动装配到service）。
                    // 且用接口注入的bean应该要做多份实例，并且以接口名做为区分
                    // 当然如果使用@Qualifier 指明是哪个bean 情况会简单的多。

                    Class<?>[] interfaces = clz.getInterfaces();
                    for (Class<?> inter : interfaces) {
                        String beanId = inter.getName();
                        if(iocContainer.containsKey(beanId)){
                            throw new RuntimeException("error create bean "+beanId+" which is exists ");
                        }else{
                            iocContainer.put(beanId,o);
                        }
                    }


                }else{
                  continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }


    //目的是为了拿到全限定类名
    private void doScanner(String scannerPackage) {
        //编译后文件的class的根目录(.../target/classes/)
        String basePath=this.getClass().getClassLoader().getResource("").getPath();

        //将.替换成/  (com.llicat--->com/llicat)
        String replace = scannerPackage.replace(".", "/");
        //完整路劲
        File  file=new File(basePath+replace);

        String className="";

        //如果包名无效不处理
        if(file.isDirectory()){
            for (File listFile : file.listFiles()) {
                //还是目录则递归
                if(listFile.isDirectory()){
                    doScanner(scannerPackage+"."+listFile.getName());
                }else{
                    //如果是class文件
                   if(listFile.getName().endsWith(".class")){
                       String name = listFile.getName();
                       //之前这里没重新为name赋值
                       name=name.substring(0,name.lastIndexOf(".class")) ;
                       className=scannerPackage+"."+name;
                       classNamesList.add(className);
                   }else{
                       continue;
                   }
                }

            }
        }






    }


    //将配置加载到内存中
    private void doLoadConfig(String contextConfigLocation) {
        InputStream in=null;
        try {
            //如果是xml配置，需要使用dom4j等第三方jar包进行解析
            in = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
            configloader.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(null!=in){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }
}
