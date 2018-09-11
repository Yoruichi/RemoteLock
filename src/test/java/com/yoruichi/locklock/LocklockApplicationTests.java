package com.yoruichi.locklock;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class LocklockApplicationTests {

    @Autowired
    private TestService testService;

    @Test
    public void contextLoads() {
        ExecutorService exec = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 10000; i++) {
            exec.submit(() -> testService.incrementOne());
        }

        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(10000, testService.getNum());
    }

}
