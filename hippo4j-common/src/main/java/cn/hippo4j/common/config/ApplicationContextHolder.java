package cn.hippo4j.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Application context holder.
 *
 * @author chen.ma
 * @date 2021/6/20 18:49
 */
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext CONTEXT;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.CONTEXT = applicationContext;
    }

    /**
     * Get ioc container bean by type.
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class<T> clazz) {
        return CONTEXT.getBean(clazz);
    }

    /**
     * Get ioc container bean by name and type.
     *
     * @param name
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return CONTEXT.getBean(name, clazz);
    }

    /**
     * Get a set of ioc container beans by type.
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return CONTEXT.getBeansOfType(clazz);
    }

    /**
     * Find whether the bean has annotations.
     *
     * @param beanName
     * @param annotationType
     * @param <A>
     * @return
     */
    public static <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
        return CONTEXT.findAnnotationOnBean(beanName, annotationType);
    }

    /**
     * Get ApplicationContext.
     *
     * @return
     */
    public static ApplicationContext getInstance() {
        return CONTEXT;
    }

    public  void test1() {

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for(int i= 0 ;i<1000 ;i++){
            int randomNum = (int) (Math.random()*10+1);
            executorService.execute(new Thread(()->{
                try {
                    Thread.sleep(randomNum * 100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Logger log = LoggerFactory.getLogger(this.getClass());
                log.info("1000");
            }));
        }
        executorService.shutdown();

       /* try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }

    public static void main(String[] args) {
        ApplicationContextHolder holder = new ApplicationContextHolder();
        holder.test1();
    }
}
