package com.sankuai.inf.leaf;/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

import com.sankuai.inf.leaf.common.Utils;
import org.junit.Test;

/**
 * @author zhaodong.xzd (github.com/yaccc)
 * @date 2019/04/27
 */
public class UtilsTest {

    @Test
    public void testIp(){
        String ip = Utils.getIp();
        System.out.println("get ip is "+ip);
        assert !"127.0.0.1".equals(ip);
    }
}
