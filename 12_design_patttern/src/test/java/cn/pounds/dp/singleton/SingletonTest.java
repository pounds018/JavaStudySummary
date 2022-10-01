package cn.pounds.dp.singleton;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: pounds
 * @Date: 2022/4/5 14:45
 * @Description: TODO
 */
public class SingletonTest {
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            10,
            10,
            0,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16), new ThreadFactory() {
                private AtomicInteger index = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "SingleTonThread" + index.getAndIncrement());
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 饿汉式单例测试
     */
    @Test
    public void HungrySingletonTest(){
        HungrySingleton instance1 = HungrySingleton.getInstance();
        HungrySingleton instance2 = HungrySingleton.getInstance();
        System.out.println("instance1 的地址为: " + instance1);
        System.out.println("instance2 的地址为: " + instance2);
        System.out.println("判断是否是单例: " + (instance1 == instance2));

        for (int i = 0; i < 10; i++) {
            EXECUTOR.submit(() -> {
                HungrySingleton instance = HungrySingleton.getInstance();
                System.out.println("instance 的地址为: " + instance);
            });
        }

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 反射破坏:
        HungrySingleton instance3 = HungrySingleton.getInstance();
        HungrySingleton instance4 = null;
        Class<HungrySingleton> hungrySingletonClass = HungrySingleton.class;
        try {
            Constructor<HungrySingleton> declaredConstructor = hungrySingletonClass.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);

            instance4 = declaredConstructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("instance3 的地址为: " + instance3);
        System.out.println("instance4 的地址为: " + instance4);

    }
}
