package com.sankuai.inf.leaf.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    /**
     * 获取本地ip地址
     *
     * @param prefer 期望获取到的ip格式(注意顺序)
     * @return
     */
    @SuppressWarnings("all")
    public static String getIp(Pattern... preferPatterns) {
        List<InetAddress> addressList = new ArrayList<>();
        try {
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                for (Enumeration<InetAddress> inetAddrs = ifaces.nextElement().getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (inetAddr.isSiteLocalAddress() && !inetAddr.isLoopbackAddress()) {
                        addressList.add(inetAddr);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Utils get IP warn", e);
        }
        if (addressList.isEmpty()) {
            return _getIp();
        }
        if (preferPatterns.length == 0) {
            return addressList.get(0).getHostAddress();
        }
        for (Pattern pattern : preferPatterns) {
            for (InetAddress address : addressList) {
                if (pattern.matcher(address.getHostAddress()).matches()) {
                    return address.getHostAddress();
                }
            }
        }
        return "";
    }


    private static String _getIp() {
        String ip;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ip = addr.getHostAddress();
        } catch (Exception ex) {
            ip = "";
            logger.warn("Utils get IP warn", ex);
        }
        return ip;
    }

}
