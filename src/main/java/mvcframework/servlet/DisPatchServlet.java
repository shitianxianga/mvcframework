package mvcframework.servlet;

import mvcframework.annotation.Autowired;
import mvcframework.annotation.Controller;
import mvcframework.annotation.RequsetMapping;
import mvcframework.annotation.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class DisPatchServlet extends HttpServlet {

    private    Properties properties=new Properties();
    private List<String> ClassNames=new ArrayList<String>();
    private HashMap<String,Object> ioc=new HashMap<String, Object>();
    private  HashMap<String, Method> handlerMapping=new HashMap<String, Method>();
    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("init------");
        //加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //根据配置文件扫描所有相关的类
        doScanner(properties.getProperty("scan-package"));
        //初始化所有相关类的实例，将其放入ioc容器中
         doInstance(ClassNames);
        //实现自动依赖注入
        doAutowried();

        //初始化HandlerMapping
        initHandlerMapping();
    }

    private void initHandlerMapping() {
        if(ioc.isEmpty())
        {
            return;
        }
        for (Map.Entry<String,Object> entry: ioc.entrySet() )
        {
            Class<?> clazz=entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(Controller.class))
            {
                continue;
            }
            String baseUrl="";
            if(clazz.isAnnotationPresent(RequsetMapping.class));
            {
                RequsetMapping requsetMapping = clazz.getAnnotation(RequsetMapping.class);

                baseUrl=requsetMapping.value();
            }
            for (Method m: clazz.getMethods()  )
            {
                if(!m.isAnnotationPresent(RequsetMapping.class))
                {
                    continue;
                }
                RequsetMapping requsetMapping=m.getAnnotation(RequsetMapping.class);
                String url=("/"+baseUrl+"/"+requsetMapping.value()).replaceAll("/+","/");
                handlerMapping.put(url,m);


            }
        }
    }

    private void doAutowried()
    {
        if(ioc.isEmpty())
        {
            return;
        }
        for (Map.Entry<String,Object> entry: ioc.entrySet() )
        {
            Field[] fields=entry.getValue().getClass().getDeclaredFields();
            for (Field field: fields ) {
                if(!field.isAnnotationPresent(Autowired.class))
                {
                    continue;
                }
                Autowired autowired=field.getAnnotation(Autowired.class);
                String beanName=autowired.value().trim();
                if("".equals(beanName))
                {
                    beanName=field.getType().getName();
                }
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(),ioc.get(beanName));

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }

            }
        }
    }

    private void doInstance(List classNames) {
        if(ClassNames.isEmpty())
        {
            return;
        }
        try {
        for (String className:ClassNames)
        {

            Class<?> clazz=Class.forName(className);
            if(clazz.isAnnotationPresent(Controller.class))
            {
                String beanName=toLow(clazz.getSimpleName());
                ioc.put(beanName,clazz.newInstance());
            }
            else if (clazz.isAnnotationPresent(Service.class))
            {
                Service service= (Service) clazz.getAnnotation(Service.class);
               String beanName=service.value();
               if("".equals(beanName.trim()))
               {
                   beanName=toLow(clazz.getSimpleName());
               }

               ioc.put(beanName,clazz.newInstance());


            Class<?>[] interfaces=  clazz.getInterfaces();
                for (Class<?> i: interfaces) {
                    if (ioc.containsKey(i.getName()))
                    {
                        throw new Exception("The Bean Name Is Exist.");
                    }
                    ioc.put(i.getName(),clazz.newInstance());
                }
            }

        }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private  String toLow(String classname)
    {
        char[] low=classname.toCharArray();
        low[0]+=32;
        return String.valueOf(low);
    }
    private void doScanner(String packagename) {
        URL url= this.getClass().getClassLoader().getResource("/"+packagename.replaceAll("\\.","/"));
        File classDir=new File(url.getFile());
        for (File file:classDir.listFiles())
        {
            if(file.isDirectory())
            {
                doScanner(packagename+"."+file.getName());
            }
            else
            {
                String className=packagename+"."+file.getName().replace(".class","");
                ClassNames.add(className);
            }
        }
    }

    private void doLoadConfig(String config)  {
        InputStream is= this.getClass().getClassLoader().getResourceAsStream(config);
        try {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null!=is)
            {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            doDispatch(req,resp);
        } catch (InvocationTargetException e) {

            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail:\n" + Arrays.toString(e.getStackTrace()));
        }
    }
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws InvocationTargetException, IllegalAccessException
    {
        String  url=req.getRequestURI();
        String context=req.getContextPath();
        url=url.replaceAll(context,"").replaceAll("/+","/");
        System.out.println(url);
        if(!handlerMapping.containsKey(url))
        {
            try {
                resp.getWriter().write("404 NOT  FOUNT");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        Method m=handlerMapping.get(url);
        String beanName=toLow(m.getDeclaringClass().getSimpleName());
        System.out.println(ioc.get(beanName).getClass().getName());
    m.invoke(ioc.get(beanName),req,resp);
    }


}
