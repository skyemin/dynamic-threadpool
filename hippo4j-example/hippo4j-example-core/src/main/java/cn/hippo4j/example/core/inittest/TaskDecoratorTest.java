package cn.hippo4j.example.core.inittest;

import cn.hippo4j.core.executor.DynamicThreadPoolWrapper;
import cn.hippo4j.core.executor.manage.GlobalThreadPoolManage;
import cn.hippo4j.example.core.config.ThreadPoolConfig;
import cn.hippo4j.example.core.constant.GlobalTestConstant;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * TaskDecorator test.
 *
 * @author chen.ma
 * @date 2021/11/28 13:01
 */
@Slf4j
@Component
public class TaskDecoratorTest {

    public static final String PLACEHOLDER = "site";

    /**
     * 测试动态线程池传递 {@link TaskDecorator}
     * 如果需要运行此单测, 方法上添加 @PostConstruct
     */
    //@PostConstruct
    public void taskDecoratorTest() {
        new Thread(() -> {
            MDC.put(PLACEHOLDER, "查看官网: https://www.hippox.cn");
            ThreadUtil.sleep(5000);
            DynamicThreadPoolWrapper poolWrapper = GlobalThreadPoolManage.getExecutorService(GlobalTestConstant.MESSAGE_PRODUCE);
            ThreadPoolExecutor threadPoolExecutor = poolWrapper.getExecutor();
            threadPoolExecutor.execute(() -> {
                /**
                 * 此处打印不为空, taskDecorator 即为生效.
                 * taskDecorator 配置查看 {@link ThreadPoolConfig#messageConsumeDynamicThreadPool()}
                 */
                log.info("通过 taskDecorator MDC 传递上下文 :: {}", MDC.get(PLACEHOLDER));
            });
        }).start();

    }

    public static class ContextCopyingDecorator implements TaskDecorator {

        @Override
        public Runnable decorate(Runnable runnable) {
            String placeholderVal = MDC.get(PLACEHOLDER);
            // other context...
            return () -> {
                try {
                    MDC.put(PLACEHOLDER, placeholderVal);
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }

}
