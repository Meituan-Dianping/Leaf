package com.darian.test.dailysegment;

import com.sankuai.inf.leaf.plugin.annotation.EnableLeafServer;
import com.sankuai.inf.leaf.service.DailySegmentService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/***
 *
 *
 * @author <a href="mailto:1934849492@qq.com">Darian</a> 
 * @date 2021/7/2  13:46
 */
@SpringBootApplication
@EnableLeafServer
public class TestDailySegmentApplication {

    @Resource
    private DailySegmentService dailySegmentService;

    public static void main(String[] args) {
        SpringApplication.run(TestDailySegmentApplication.class, args);
    }

    @PostConstruct
    public void init() {
        dailySegmentService.getId("1");
    }
}
