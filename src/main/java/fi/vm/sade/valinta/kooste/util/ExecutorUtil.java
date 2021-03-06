package fi.vm.sade.valinta.kooste.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

public class ExecutorUtil {

  public static ExecutorService createExecutorService(int maxSize, String threadName) {
    ThreadFactory threadFactory = new CustomizableThreadFactory(threadName + "-");
    return Executors.newFixedThreadPool(maxSize, threadFactory);
  }
}
