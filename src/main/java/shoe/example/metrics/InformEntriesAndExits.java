package shoe.example.metrics;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import shoe.example.log.SystemLogger;
import shoe.example.log.SystemLoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Aspect
@Component
public class InformEntriesAndExits {
  public static final String APP_NAME_PROPERTY = "APPLICATION_NAME";
  public static final String PORT_NAME_PROPERTY = "PORT";

  private static String hostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "UnknownHost";
    }
  }

  private static String group() {
    String app = System.getProperty(APP_NAME_PROPERTY);
    String port = System.getProperty(PORT_NAME_PROPERTY);
    return String.format("%s.%s.%s", app, hostName(), port);
  }

  @Around("@target(service) && within(shoe.example..*)")
  public Object reportServiceEntry(ProceedingJoinPoint jp, Service service) throws Throwable {
    return executeShell(jp);
  }

  @Around("@target(component) && within(shoe.example..*)")
  public Object reportComponentEntry(ProceedingJoinPoint jp, Component component) throws Throwable {
    return executeShell(jp);
  }

  @Around("@target(repository) && within(shoe.example..*)")
  public Object reportResourceExecution(ProceedingJoinPoint jp, Repository repository) throws Throwable {
    return executeShell(jp);
  }

  private Object executeShell(ProceedingJoinPoint jp) throws Throwable {
    CorrelationId.enter();

    String className = jp.getSignature().getDeclaringTypeName();
    String methodName = jp.getSignature().getName();
    MetricName name = new MetricName(group(), className, methodName);

    SystemLogger targetLogger = SystemLoggerFactory.get(className);
    targetLogger.info("start : %s-%s", methodName, CorrelationId.get());

    Timer responses = Metrics.newTimer(name, MILLISECONDS, SECONDS);
    TimerContext context = responses.time();

    long start = System.currentTimeMillis();
    try {
      Object result = jp.proceed();
      targetLogger.info("finish : %s-%s(%dms)", methodName, CorrelationId.get(), System.currentTimeMillis() - start);
      return result;
    } catch (Throwable t) {
      targetLogger.info("failing: %s-%s(%dms)", methodName, CorrelationId.get(), System.currentTimeMillis() - start);
      throw t;
    } finally {
      context.stop();
      CorrelationId.exit();
    }
  }
}