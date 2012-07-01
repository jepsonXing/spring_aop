package shoe.example;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import javax.ws.rs.Path;

@Aspect
@Component
public class InformEntriesAndExists {
    @Around(value = "@target(service) && @target(path)", argNames = "jp,service,path")
    public Object reportRestEndpoints(ProceedingJoinPoint jp, Service service, Path path) throws Throwable {
        return executeShell(jp, "rest endpoint");
    }

    @Around("@target(service)")
    public Object reportServiceEntry(ProceedingJoinPoint jp, Service service) throws Throwable {
        return executeShell(jp, "service");
    }

    @Around("@target(repository)")
    public Object reportResourceExecution(ProceedingJoinPoint jp, Repository repository) throws Throwable {
        return executeShell(jp, "repository");
    }

    private Object executeShell(ProceedingJoinPoint jp, String which) throws Throwable {
        System.out.printf("%s - start: %s\n", which, jp.getSignature());
        try {
            Object result = jp.proceed();
            System.out.printf("%s - finish: %s\n", which, jp.getSignature());
            return result;
        } catch (Throwable t) {
            System.out.printf("%s - failing: %s\n", which, jp.getSignature());
            throw t;
        }
    }
}