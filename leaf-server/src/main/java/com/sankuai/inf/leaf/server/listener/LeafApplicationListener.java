package com.sankuai.inf.leaf.server.listener;


import com.sankuai.inf.leaf.server.init.InitServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class LeafApplicationListener implements ApplicationListener {
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if(applicationEvent instanceof ApplicationReadyEvent){
            //初始化服务
            initServers();
        }
    }

    private void initServers() {
        Map<String, InitServer> initServerMap =  applicationContext.getBeansOfType(InitServer.class);
        if(initServerMap != null && initServerMap.size() > 0){
            for(InitServer initServer : initServerMap.values()){
                initServer.init();
            }
        }
    }
}
