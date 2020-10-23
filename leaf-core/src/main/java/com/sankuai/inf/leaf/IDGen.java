package com.sankuai.inf.leaf;

import com.sankuai.inf.leaf.common.Result;

/**
 * Id 生成器接口
 * @author mickle
 */
public interface IDGen {
    /**
     * 获取指定业务tag下一id
     * @param key         业务tag
     * @return            result
     */
    Result get(String key);

    /**
     * 模式初始化
     * @return            是否成功
     */
    boolean init();
}
