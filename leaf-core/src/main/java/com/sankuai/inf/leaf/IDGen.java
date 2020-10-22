package com.sankuai.inf.leaf;

import com.sankuai.inf.leaf.common.Result;

/**
 * @author mickle
 */
public interface IDGen {
    Result get(String key);
    boolean init();
}
