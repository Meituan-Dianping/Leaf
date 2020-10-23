package com.sankuai.inf.leaf.common;

import com.sankuai.inf.leaf.IDGen;

/**
 * 数据库号段模式默认默认实现
 * 当配置文件中 leaf.segment.enable 设为false时采用该模式
 *
 * @author mickle
 */
public class ZeroIDGen implements IDGen {

    @Override
    public Result get(String key) {
        return new Result(0, Status.SUCCESS);
    }

    @Override
    public boolean init() {
        return true;
    }
}
