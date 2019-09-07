package com.sankuai.inf.leaf;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
public class LeafClient {
    private OkHttpClient client = new OkHttpClient();

    private ConcurrentLinkedQueue<String> addressQueue = new ConcurrentLinkedQueue<>();

    private ReentrantLock reentrantLock = new ReentrantLock();

    public LeafClient(String serverList, String serverName) {
        try {
            NamingService namingService = NamingFactory.createNamingService(serverList);
            List<Instance> instanceList = namingService.getAllInstances(serverName);
            if(instanceList != null && instanceList.size() > 0){
                addressQueue.addAll(instanceList.stream()
                        .filter(Instance::isHealthy)
                        .map(ins -> ins.getIp() + ":" + ins.getPort())
                        .collect(Collectors.toSet()));
                log.info("pull server info from nacos, addressQueue:{}",addressQueue);
            }
            //监听服务变化
            namingService.subscribe(serverName, event -> {
                if(event instanceof NamingEvent){

                    reentrantLock.lock();

                    NamingEvent namingEvent = (NamingEvent)event;
                    List<Instance> newInstances = namingEvent.getInstances();
                    if(newInstances != null && newInstances.size() > 0){
                        addressQueue.addAll(newInstances.stream()
                                .filter(Instance::isHealthy)
                                .map(ins -> ins.getIp() + ":" + ins.getPort())
                                .filter(i -> !addressQueue.contains(i))
                                .collect(Collectors.toSet()));
                    }

                    reentrantLock.unlock();
                    log.info("server instance changed, now addressQueue:{}",addressQueue);
                    //todo email notify
                }

            });
        } catch (NacosException e) {

        }
    }
    public String request(String method){
        String res = null;

        reentrantLock.lock();

        while (!addressQueue.isEmpty()){
            String address = addressQueue.poll();
            log.info("request address is {}",address);
            String url = buildUrl(address ,method);
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                res = responseBody == null ? null : responseBody.string();
            }catch (Exception igonre){
                igonre.printStackTrace();
                log.info("address {} is not healthy, need remove it",address);
                addressQueue.remove(address);
                continue;
            }
            addressQueue.offer(address);
            break;
        }

        reentrantLock.unlock();

        return res;
    }

    private String buildUrl(String address ,String method) {
        if(StringUtils.startsWith(method,"/")){
            return "http://" + address + method;
        }else{
            return "http://" + address + "/" + method;
        }
    }

}
