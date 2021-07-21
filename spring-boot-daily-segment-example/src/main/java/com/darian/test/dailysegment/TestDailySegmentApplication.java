package com.darian.test.dailysegment;

import com.sankuai.inf.leaf.common.DateUtils;
import com.sankuai.inf.leaf.plugin.annotation.EnableLeafServer;
import com.sankuai.inf.leaf.service.DailySegmentService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Date;

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
        System.out.println(dailySegmentService.getId("TEST" + "_" + DateUtils.formatyyyyMMdd(new Date())));
        System.out.println(dailySegmentService.getId("TEST" + "_" + DateUtils.formatyyyyMMdd(DateUtils.addDay(new Date(), 1))));
        System.out.println(dailySegmentService.getId("TEST" + "_" + DateUtils.formatyyyyMMdd(DateUtils.addDay(new Date(), 2))));
        System.out.println(dailySegmentService.getId("CASE" + "_" + DateUtils.formatyyyyMMdd(new Date())));
        System.out.println(dailySegmentService.getId("LEAF" + "_" + DateUtils.formatyyyyMMdd(new Date())));
    }
}
