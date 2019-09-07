package test;

import com.sankuai.inf.leaf.LeafClient;

import java.util.concurrent.*;

public class MainTest {

    public static void main(String[] args) throws Exception {
        LeafClient leafClient = new LeafClient("127.0.0.1:8848","mall-leaf");
        Executor executor = Executors.newFixedThreadPool(10);
        CyclicBarrier cyclicBarrier = new CyclicBarrier(10);
        for(int i =0; i < 10 ; i++){
            executor.execute(()->{
                try {
                    cyclicBarrier.await();
                    while (true){
                        leafClient.request("/api/snowflake/get");
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
