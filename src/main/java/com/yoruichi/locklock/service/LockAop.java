package com.yoruichi.locklock.service;

import com.yoruichi.locklock.annotations.Synchronized;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Aspect
@Component
public class LockAop {

    private Logger logger = LoggerFactory.getLogger(LockAop.class);

    @Autowired
    private LockService lockService;

    @Pointcut("@annotation(com.yoruichi.locklock.annotations.Synchronized)")
    public void pointCut() {
    }

    @Before("pointCut()")
    public void getLock(JoinPoint jp)
            throws NoSuchMethodException, TimeoutException, ExecutionException,
            InterruptedException {
        Synchronized annotationSync =
                jp.getTarget().getClass().getMethod(jp.getSignature().getName())
                        .getAnnotation(Synchronized.class);
        String name = annotationSync.name();
        String prefixValue = annotationSync.prefixValue();
        String value = "".equals(prefixValue) ?
                Thread.currentThread().getName() :
                new StringBuilder(prefixValue).append("_").append(Thread.currentThread().getName())
                        .toString();
        long expiredTime = annotationSync.expiredTimeInMilliSeconds();
        long waitTime = annotationSync.waitTimeInMilliSeconds();
        logger.debug("Executing method:{} with @annotation name:{}, prefixValue:{} on thread {}.",
                jp.getSignature().getName(), annotationSync.name(), annotationSync.prefixValue(),
                Thread.currentThread().getName());
        lockService.getLockIfAbsent(name, value, waitTime, TimeUnit.MILLISECONDS, expiredTime,
                TimeUnit.MILLISECONDS);
        logger.debug("Thread {} got lock for name {}", value, name);
    }

    @After("pointCut()")
    public void releaseLock(JoinPoint jp) throws NoSuchMethodException {
        Synchronized annotationSync =
                jp.getTarget().getClass().getMethod(jp.getSignature().getName())
                        .getAnnotation(Synchronized.class);
        String name = annotationSync.name();
        String prefixValue = annotationSync.prefixValue();
        String value = "".equals(prefixValue) ?
                Thread.currentThread().getName() :
                new StringBuilder(prefixValue).append("_").append(Thread.currentThread().getName())
                        .toString();
        logger.debug("Executing method:{} with @annotation name:{}, prefixValue:{} on thread {}.",
                jp.getSignature().getName(), annotationSync.name(), annotationSync.prefixValue(),
                Thread.currentThread().getName());
        lockService.returnLock(name, value);
        logger.debug("Thread {} released lock for name {}", value, name);
    }
}
