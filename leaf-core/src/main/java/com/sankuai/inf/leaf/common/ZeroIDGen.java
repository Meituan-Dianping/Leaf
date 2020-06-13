package com.sankuai.inf.leaf.common;

import com.sankuai.inf.leaf.IDGen;

public class ZeroIDGen implements IDGen {
    @Override
    public Result get(String key) {
        return new Result(0, Status.SUCCESS);
    }

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public Result getBatch(String key, Long size) {
        return null;
    }
}
