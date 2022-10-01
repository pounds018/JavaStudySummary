package cn.pounds.netty.future;

import java.util.concurrent.Callable;

/**
 * @Author: pounds
 * @Date: 2022/7/20 22:24
 * @Description: TODO
 */
public class HotWaterJob implements Callable<Boolean> {


    @Override
    public Boolean call() throws Exception {
        try {
            System.out.println("开始烧开水了");

            Thread.sleep(3000);
        } catch (Exception e) {
            System.out.printf("烧开水的时候发生异常了, %s%n", e.getMessage());
            return false;
        }
        System.out.println("开水烧好了");
        return true;
    }
}
