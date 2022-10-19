package Chapter11_MultiThread;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;

/**
 * @Date 2021/3/10 22:46
 * @Author by pounds
 * @Description TODO
 */
public class CompletableFutureTest {
    /**
     * 验证默认线程池产生的线程是不是守护线程
     */
    @Test
    public void test(){

        CompletableFuture.runAsync(()->{
            System.out.println("当前线程是不是守护线程: " + Thread.currentThread().isDaemon());
        });

        // 防止test线程退出导致jvm退出,从而造成异步线程输出语句不打印
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /**
         * 当前线程是不是守护线程: true
         */
    }

    @Test
    public void testThenAcceptBoth(){
        CompletableFuture<String> domainFuture = CompletableFuture.supplyAsync(() -> {
            int a = 1;
            if (a == 1) {
                throw new RuntimeException("domain registry error");
            }
            return "name";
        }).whenComplete((res, throwable) -> {
            if (throwable == null) {
                System.out.println("domain future completed without exception");
            }
        }).exceptionally((throwable -> {
            System.out.println("domain future completed with error");
            return null;
        }));

        CompletableFuture<String> fatpodDeploy = CompletableFuture.supplyAsync(() -> {
            int a = 1;
            if (a == 1) {
                throw new RuntimeException("domain registry error");
            }
            return "11111111111111";
        }).whenComplete((res, throwable) -> {
            if (throwable == null) {
                System.out.println("fatpod future completed without exception");
            }
        }).exceptionally(throwable -> {
            System.out.println("fatpod future completed with exception");
            return "error";
        });

        domainFuture.thenAcceptBoth(fatpodDeploy, (res1, res2) -> {
            System.out.println(res1 + res2);
        });


    }
}
