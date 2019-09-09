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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
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


    private Method getMethodByName(Class clazz, String name) {
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(name)) {
                return methods[i];
            }
        }
        return null;
    }

    private String getName(Synchronized annotationSync, Class clazz, JoinPoint jp) {
        String name = annotationSync.name();
        String generateNameMethodName = annotationSync.generateNameMethod();
        try {
            Method generateNameMethod = getMethodByName(clazz, generateNameMethodName);
            if (Objects.isNull(generateNameMethod)) {
                logger.warn("No such method named {}.", generateNameMethodName);
            } else {
                generateNameMethod.setAccessible(true);
                name = (String) generateNameMethod.invoke(jp.getTarget(), jp.getArgs());
            }
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
            logger.warn("Failed to invoke the method {} with args {}.", generateNameMethodName, Arrays.toString(jp.getArgs()));
        } catch (Exception e) {
            logger.error("Failed to invoke the method {} with args {}.", generateNameMethodName, Arrays.toString(jp.getArgs()));
        }
        return name;
    }

    @Before("pointCut()")
    public void getLock(JoinPoint jp) throws NoSuchMethodException, TimeoutException, ExecutionException, InterruptedException {
        Class<? extends Object> clazz = jp.getTarget().getClass();
        Method pointCutMethod = getMethodByName(clazz, jp.getSignature().getName());
        if (Objects.isNull(pointCutMethod)) {
            logger.error("Failed to get lock.");
            throw new NoSuchMethodException(jp.getSignature().getName());
        }
        Synchronized annotationSync = pointCutMethod.getAnnotation(Synchronized.class);
        if (Objects.isNull(annotationSync)) {
            logger.error("Failed to get lock.");
            throw new NoSuchMethodException(jp.getSignature().getName());
        }

        String name = getName(annotationSync, clazz, jp);
        String prefixValue = annotationSync.prefixValue();
        String value = "".equals(prefixValue) ?
                Thread.currentThread().getName() : new StringBuilder(prefixValue).append("_").append(Thread.currentThread().getName()).toString();
        long expiredTime = annotationSync.expiredTimeInMilliSeconds();
        long waitTime = annotationSync.waitTimeInMilliSeconds();
        logger.debug("Executing method:{} with @annotation name:{}, prefixValue:{} on thread {}.", jp.getSignature().getName(), annotationSync.name(),
                annotationSync.prefixValue(), Thread.currentThread().getName());
        if (!lockService.getLockIfAbsent(name, value, waitTime, TimeUnit.MILLISECONDS, expiredTime, TimeUnit.MILLISECONDS)) {
            throw new InterruptedException();
        }
        logger.debug("Thread {} got lock for name {}", value, name);
    }

    @After("pointCut()")
    public void releaseLock(JoinPoint jp) throws NoSuchMethodException {
        Class<? extends Object> clazz = jp.getTarget().getClass();
        Method pointCutMethod = getMethodByName(clazz, jp.getSignature().getName());
        if (Objects.isNull(pointCutMethod)) {
            logger.error("Failed to get lock.");
            throw new NoSuchMethodException(jp.getSignature().getName());
        }
        Synchronized annotationSync = pointCutMethod.getAnnotation(Synchronized.class);
        if (Objects.isNull(annotationSync)) {
            logger.error("Failed to get lock.");
            throw new NoSuchMethodException(jp.getSignature().getName());
        }

        String name = getName(annotationSync, clazz, jp);
        String prefixValue = annotationSync.prefixValue();
        String value = "".equals(prefixValue) ?
                Thread.currentThread().getName() : new StringBuilder(prefixValue).append("_").append(Thread.currentThread().getName()).toString();
        logger.debug("Executing method:{} with @annotation name:{}, prefixValue:{} on thread {}.", jp.getSignature().getName(), annotationSync.name(),
                annotationSync.prefixValue(), Thread.currentThread().getName());
        lockService.returnLock(name, value);
        logger.debug("Thread {} released lock for name {}", value, name);
    }
}
