package com.sankuai.inf.leaf.server;

import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Status;
import com.sankuai.inf.leaf.server.exception.LeafServerException;
import com.sankuai.inf.leaf.server.exception.NoKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeafController {
    private Logger logger = LoggerFactory.getLogger(LeafController.class);
    @Autowired
    SegmentService segmentService;
    @Autowired
    SnowflakeService snowflakeService;

    @RequestMapping(value = "/api/segment/get/{key}")
    public String getSegmentID(@PathVariable("key") String key) {
        return get(key, segmentService.getId(key));
    }

    @RequestMapping(value = "/api/snowflake/get")
    public String getSnowflakeID() {
        Result result = snowflakeService.getId(null);
        if(result.getStatus() == Status.SUCCESS){
            return String.valueOf(result.getId());
        }else {
            throw new LeafServerException(result.toString());
        }
    }

    private String get(@PathVariable("key") String key, Result id) {
        Result result;
        if (key == null || key.isEmpty()) {
            throw new NoKeyException();
        }

        result = id;
        if (result.getStatus().equals(Status.EXCEPTION)) {
            throw new LeafServerException(result.toString());
        }
        return String.valueOf(result.getId());
    }
}
