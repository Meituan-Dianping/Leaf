package com.sankuai.inf.leaf.common;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 *
 *
 * @author <a href="mailto:1934849492@qq.com">Darian</a> 
 * @date 2021/7/1  22:27
 */
public class ListUtils {

    /**
     * 将数据按照固定大小进行切割
     *
     * @param stringList
     * @param maxSend
     * @return
     */
    public static List<List<String>> splitList(List<String> stringList, Integer maxSend) {
        int limit = countStep(stringList.size(), maxSend);
        return Stream.iterate(0, n -> n + 1)
                .limit(limit)
                .parallel()
                .map(a -> stringList.stream()
                        .skip(a * maxSend)
                        .limit(maxSend)
                        .parallel()
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    /**
     * 计算切分次数
     */
    private static Integer countStep(Integer size, Integer maxSend) {
        return (size + maxSend - 1) / maxSend;
    }

    public static void main(String[] args) {
        List<String> strings = Arrays.asList("a", "c", "b", "a");
        System.out.println(splitList(strings, 1));
        System.out.println();
        System.out.println(splitList(strings, 2));
        System.out.println();
        System.out.println(splitList(strings, 3));
    }

}
