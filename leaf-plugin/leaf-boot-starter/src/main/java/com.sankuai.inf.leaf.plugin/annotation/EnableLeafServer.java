package com.sankuai.inf.leaf.plugin.annotation;

import com.sankuai.inf.leaf.plugin.LeafSpringBootStarterAutoConfigure;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;
/**
 * @author zhaodong.xzd (github.com/yaccc)
 * @date 2019/10/09
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(LeafSpringBootStarterAutoConfigure.class)
@Inherited
public @interface EnableLeafServer {
}
