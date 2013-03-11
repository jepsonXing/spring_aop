package shoe.example.metrics;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import shoe.example.log.SystemLoggerFactory;
import shoe.example.toggles.TrackMetrics;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class MetricRecorder {
  public static final String APP_NAME_PROPERTY = "APPLICATION_NAME";
  public static final String PORT_NAME_PROPERTY = "PORT";
  private final String className;
  private final String methodName;
  private final TrackMetrics trackMetrics;
  private TimerContext context;
  private long startTime;
  private long stopTime;

  public MetricRecorder(TrackMetrics trackMetrics, String className, String methodName) {
    this.className = className;
    this.methodName = methodName;
    this.trackMetrics = trackMetrics;
  }

  public void enter() {
    CorrelationId.enter();
    startTime = System.currentTimeMillis();

    if (trackMetrics.isEnabled()) {
      MetricName name = new MetricName(group(), className, methodName);
      Timer responses = Metrics.newTimer(name, MILLISECONDS, SECONDS);
      context = responses.time();
    }

    SystemLoggerFactory.get(className).info("start : %s-%s", methodName, CorrelationId.get());
  }

  public void exit(boolean success) {
    if (context != null) {
      context.stop();
    }

    stopTime = System.currentTimeMillis();

    String result = success ? "finish" : "failure";
    SystemLoggerFactory.get(className).info("%7s: %s-%s(%dms)", result, methodName, CorrelationId.get(), duration());
    CorrelationId.exit();
  }

  private long duration() {
    return stopTime - startTime;
  }

  private String hostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "UnknownHost";
    }
  }

  private String group() {
    String app = System.getProperty(APP_NAME_PROPERTY);
    String port = System.getProperty(PORT_NAME_PROPERTY);
    return String.format("%s.%s.%s", app, hostName(), port);
  }
}