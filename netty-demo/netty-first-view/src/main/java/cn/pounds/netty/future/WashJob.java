package cn.pounds.netty.future;

import java.util.concurrent.Callable;

/**
 * @Author: pounds
 * @Date: 2022/7/20 22:31
 * @Description:
 */
public class WashJob implements Callable<Boolean> {
    @Override
    public Boolean call() throws Exception {
        try {
            System.out.println("洗茶杯工作开始");

            Thread.sleep(3500);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        System.out.println("茶杯洗好了");
        return true;
    }
}
