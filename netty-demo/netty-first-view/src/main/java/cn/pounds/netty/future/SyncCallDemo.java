package cn.pounds.netty.future;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.Test;

import java.util.concurrent.*;

/**
 * @Author: pounds
 * @Date: 2022/7/20 22:19
 */
public class SyncCallDemo {

    private boolean cupOk = false;
    private boolean waterOk = false;

    public static final ThreadPoolExecutor POOL_EXECUTOR = new ThreadPoolExecutor(
            8,
            16,
            0,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1024),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r);
                }
            });

    /**
     * 异步阻塞调用的演示
     */
    @Test
    public void AsyncCall() throws InterruptedException {
        // 测试线程作为 这个泡茶喝demo的主线程
        Callable<Boolean> washJob = new WashJob();
        FutureTask<Boolean> washJobFutureTask = new FutureTask<>(washJob);

        Callable<Boolean> hotWaterJob = new HotWaterJob();
        FutureTask<Boolean> hotWaterFutureTask = new FutureTask<>(hotWaterJob);
        POOL_EXECUTOR.submit(washJobFutureTask);
        POOL_EXECUTOR.submit(hotWaterFutureTask);

        try {
            Boolean cupOk = washJobFutureTask.get();
            Boolean waterOk = hotWaterFutureTask.get();

            drinkTea(waterOk, cupOk);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread.sleep(1000);
    }


    /**
     * 异步回调的方法演示
     */
    @Test
    public void asyncCallBack() throws InterruptedException {
        Callable<Boolean> washJob = new WashJob();

        Callable<Boolean> hotWaterJob = new HotWaterJob();
        // 这里借用netty的回调机制演示, netty中 扩展了java的Future接口, 在Future接口中增加了一个通过listener回调的机制
        DefaultEventExecutorGroup eventExecutors = new DefaultEventExecutorGroup(2);
        Future<Boolean> submit = eventExecutors.submit(hotWaterJob);
        submit.addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("烧水完成");
                this.waterOk = true;
            } else {
                System.out.println("烧水失败");
            }
        });

        Future<Boolean> washFuture = eventExecutors.submit(washJob);
        washFuture.addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("洗杯子成功");
                this.cupOk = true;
                drinkTea(this.waterOk, this.cupOk);
            } else {
                System.out.println("洗杯子失败");
            }
        });

        Thread.sleep(5000);

    }
    public static void drinkTea(boolean waterOk, boolean cupOk) {
        if (waterOk && cupOk) {
            System.out.println(Thread.currentThread().getName());
            System.out.println("泡茶喝");
        } else if (!waterOk) {
            System.out.println("烧水失败，没有茶喝了");
        } else {
            System.out.println("杯子洗不了，没有茶喝了");
        }
    }

}
