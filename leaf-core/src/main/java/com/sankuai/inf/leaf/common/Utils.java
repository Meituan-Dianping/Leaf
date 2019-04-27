package com.sankuai.inf.leaf.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author:zhaodong.xzd
 * 19-04-27
 */
public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static String getIp() throws RuntimeException {
        Set<String> localIps = getLocalIps();
        for (String next : localIps) {
            if (isValidIp(next)) {
                return next;
            }
        }
        logger.error("can not find available IP, find ips are {}", localIps);
        throw new RuntimeException("can not find available IP");
    }

    private static final String ANYHOST = "0.0.0.0";

    private static final String LOCALHOST = "127.0.0.1";

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    static boolean isValidIp(InetAddress address) {
        if (address == null || address.isLoopbackAddress())
            return false;
        String name = address.getHostAddress();
        return isValidIp(name);
    }

    static boolean isValidIp(String ip) {
        return (ip != null
                && !ANYHOST.equals(ip)
                && !LOCALHOST.equals(ip)
                && IP_PATTERN.matcher(ip).matches());
    }

    static Set<String> getLocalIps() {
        Set<String> result = new HashSet<>();
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidIp(localAddress)) {
                result.add(localAddress.getHostAddress().trim());
            }
        } catch (Throwable e) {
            logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        if (addresses != null) {
                            while (addresses.hasMoreElements()) {
                                try {
                                    InetAddress address = addresses.nextElement();
                                    if (isValidIp(address)) {
                                        result.add(address.getHostAddress().trim());
                                    }
                                } catch (Throwable e) {
                                    logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
        }
        return result;
    }
}
