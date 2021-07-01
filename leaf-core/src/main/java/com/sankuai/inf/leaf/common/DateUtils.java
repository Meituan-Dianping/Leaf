package com.sankuai.inf.leaf.common;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/***
 *
 *
 * @author <a href="mailto:1934849492@qq.com">Darian</a> 
 * @date 2021/7/1  21:56
 */
public class DateUtils {

    public static String formatyyyyMMdd(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        return simpleDateFormat.format(date);
    }

    /**
     * 在输入日期上增加（+）或减去（-）天数
     *
     * @param date 输入日期
     * @param days 要增加或减少的天数
     */
    public static Date addDay(Date date, int days) {
        Calendar cd = Calendar.getInstance();
        cd.setTime(date);
        cd.add(Calendar.DAY_OF_MONTH, days);
        return cd.getTime();
    }
}
