package cn.hippo4j.example.core.inittest;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.*;

import static cn.hippo4j.common.constant.Constants.EXECUTE_TIMEOUT_TRACE;

/**
 * Test run time metrics.
 *
 * @author chen.ma
 * @date 2021/8/15 21:00
 */
@Slf4j
@Component
public class RunStateHandlerTest {

    @Resource
    private ThreadPoolExecutor messageConsumeDynamicThreadPool;

    @Resource
    private ThreadPoolExecutor messageProduceDynamicThreadPool;

    @Resource
    private ThreadPoolExecutor skyeThreadPool;

    @PostConstruct
    @SuppressWarnings("all")
    public void runStateHandlerTest() {
        log.info("Test thread pool runtime state interface...");

       /* // 启动动态线程池模拟运行任务
        runTask(messageConsumeDynamicThreadPool);

        // 启动动态线程池模拟运行任务
        runTask(messageProduceDynamicThreadPool);*/

        runTask(skyeThreadPool);
    }

    private static void runTask(ExecutorService executorService) {

        /*while(true) {
            executorService.execute(() -> {
                try {
                    Thread.sleep(3000);
                    System.out.println(Thread.currentThread().getName()+"开始执行...");
                } catch (InterruptedException e) {
                    // ignore
                }
            });

        }*/

        // 模拟任务运行
        Thread thread = new Thread(() -> {
            /**
             * 当线程池任务执行超时, 向 MDC 放入 Trace 标识, 报警时打印出来.
             */
            //MDC.put(EXECUTE_TIMEOUT_TRACE, "test");
            System.out.println("开始执行");
            ThreadUtil.sleep(1000);
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                try {
                    executorService.execute(() -> {
                        try {
                            Thread.sleep(5000);
                            //System.out.println( MDC.get(EXECUTE_TIMEOUT_TRACE));
                            System.out.println(Thread.currentThread().getName() + "开始执行...");
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    });
                } catch (Exception ex) {
                    // ignore
                }
                ThreadUtil.sleep(500);
            }

        });
        thread.setName("Test-Thread");
        thread.start();
    }

    public static void main(String[] args) {
       /* ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 0,
                TimeUnit.MICROSECONDS,
                new LinkedBlockingDeque<Runnable>(2),

                new ThreadPoolExecutor.AbortPolicy());
                 executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
                */
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 0,
                TimeUnit.MICROSECONDS,
                new LinkedBlockingDeque<Runnable>(2),
                new ThreadPoolExecutor.AbortPolicy());

        try {
            for (int i = 0; i < 6; i++) {
                System.out.println("添加第"+i+"个任务");
                executor.execute(new MyThread("线程"+i));
                Iterator iterator = executor.getQueue().iterator();
                while (iterator.hasNext()){
                    MyThread thread = (MyThread) iterator.next();
                    System.out.println("列表："+thread.name);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("12131331");
        }
    }
    static class MyThread implements Runnable {
        String name;
        public MyThread(String name) {
            this.name = name;
        }
        @Override
        public void run() {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("线程:"+Thread.currentThread().getName() +" 执行:"+name +"  run");
        }
    }
}