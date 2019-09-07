package com.sankuai.inf.leaf.server.init;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.sankuai.inf.leaf.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class RegistNacosInitServer implements InitServer{
    @NacosInjected
    private NamingService namingService;
    @Autowired
    private WebApplicationContext webApplicationContext;
    @Value("${nacos.enable}")
    private boolean enableNacos;
    @Value("${leaf.name}")
    private String leafName;
    @Value("${server.port}")
    private int port;

    @Override
    public void init(){
        if(!enableNacos){
            log.info("nacos enable is false , can not regist server to nacos !");
            return;
        }
        //获取服务
        Set<String> requestMappingList = new HashSet<>();

        RequestMappingHandlerMapping requestMappingHandlerMapping = webApplicationContext.getBean(RequestMappingHandlerMapping.class);

        Map<RequestMappingInfo, HandlerMethod> handlerMethodMap = requestMappingHandlerMapping.getHandlerMethods();


        for(RequestMappingInfo requestMappingInfo : handlerMethodMap.keySet()){
            Set<String> patternSet = requestMappingInfo.getPatternsCondition().getPatterns();
            requestMappingList.addAll(patternSet);
        }

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
            log.info("regist server to nacos success ! serverName:{},ip:{},port:{}",leafName,ip,port);
        } catch (NacosException e) {
            log.error("regist server to nacos fail",e);
        }
    }
}
