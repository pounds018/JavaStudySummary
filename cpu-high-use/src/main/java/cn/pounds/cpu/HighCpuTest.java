package cn.pounds.cpu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: pounds
 * @Date: 2022/7/23 0:08
 * @Description: 模拟cpu占用偏高的场景
 */
public class HighCpuTest {
    public static void main(String[] args) {
        List<HighCpu> cpus = new ArrayList<HighCpu>();

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("cpu-high-use-thread");
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());

        threadPoolExecutor.submit(() -> {
            int i = 0;
            while (true) {
                HighCpu highCpu = new HighCpu("cpu" + i, i);
                cpus.add(highCpu);
                System.out.println("当前cpu数量" + cpus.size());
                i++;
            }
        });
    }
}
