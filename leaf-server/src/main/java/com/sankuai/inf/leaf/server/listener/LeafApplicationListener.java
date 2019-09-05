package com.sankuai.inf.leaf.server.listener;


import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.sankuai.inf.leaf.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

@Slf4j
@Component
public class LeafApplicationListener implements ApplicationListener {
    @NacosInjected
    private NamingService namingService;
    @Autowired
    private ApplicationContext applicationContext;
    @Value("${leaf.name}")
    private String leafName;
    @Value("${server.port}")
    private int port;

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if(applicationEvent instanceof ApplicationReadyEvent){
            //获取服务
            List<String> requestMappingList = new ArrayList<>();

            String[] controllerBeans = applicationContext.getBeanNamesForAnnotation(Controller.class);
            for(String name : controllerBeans){
                Object obj = applicationContext.getBean(name);
                Class clazz =  obj.getClass();
                Annotation classRequestMapping = clazz.getAnnotation(RequestMapping.class);
                String[] pre = ((RequestMapping)classRequestMapping).value();
                Method[] methods = obj.getClass().getMethods();
                for(Method method : methods){
                    RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                    if(requestMapping != null){
                        findPath(requestMappingList, requestMapping.value());
                    }
                    GetMapping getMapping = method.getAnnotation(GetMapping.class);
                    if(getMapping != null){
                        findPath(requestMappingList, getMapping.value());
                    }
                    PostMapping postMapping = method.getAnnotation(PostMapping.class);
                    if(postMapping != null){
                        findPath(requestMappingList, postMapping.value());
                    }
                }
            }
            String[] restControllerBeans = applicationContext.getBeanNamesForAnnotation(RestController.class);

            String ip = Utils.getIp();
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setServiceName(leafName);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("methods", StringUtils.join(requestMappingList,","));
            instance.setMetadata(metadata);
            //注册服务
            try {
                namingService.registerInstance(leafName,instance);
                log.info("regist server success ! serverName:{},ip:{},port:{}",leafName,ip,port);
            } catch (NacosException e) {
                e.printStackTrace();
                log.error("regist server fail");
            }
        }
    }

    private void findPath(List<String> requestMappingList, String[] values) {
        if (values.length == 0) {
            return;
        }
        for (String val : values) {
            log.info("add mapping path : {}", val);
            requestMappingList.add(val);
        }
    }
}
