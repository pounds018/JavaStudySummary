package Chapter11_MultiThread;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: pounds
 * @date: 2021/11/11 23:59
 * @desc:
 */
public class ThreadLocalTest {
    /**
     * 创建一个初始值为 10 的threadlocal对象.
     */
    private static final ThreadLocal<Integer> THREAD_LOCAL = ThreadLocal.withInitial(()->10);
    private static final ThreadPoolExecutor EXECUTORS = new ThreadPoolExecutor(
            3,
            3,
            0,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(15),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
//    {
//        @Override
//        protected void afterExecute(Runnable r, Throwable t) {
//            // 线程执行完毕后清理各自的threadlocal存储的数据
//            THREAD_LOCAL.remove();
//        }
//    };


    @Test
    public void test(){
        // 存储各个线程对threadlocal值进行+10之后的结果
        Integer[] integers = new Integer[4];
        // 最大线程数量设置为3 , 开启4个线程一定会复用线程, 可以观察是否能够拿到 threadlocal里面没有清除的value
        for (int i = 0; i < 4; i++) {
            AtomicInteger atomicInteger = new AtomicInteger(i);
            EXECUTORS.execute(()->{
                try {
                    // 先睡2 * i秒,让各个线程串行
                    Thread.sleep(2000L * atomicInteger.get());
                    Integer threadlocalValue = THREAD_LOCAL.get();
                    System.out.println("threadlocal初始值为:  " + threadlocalValue);

                    // 做一个加操作
                    THREAD_LOCAL.set(threadlocalValue + 10);
                    integers[atomicInteger.get()] = THREAD_LOCAL.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Arrays.toString(integers));
    }
}
