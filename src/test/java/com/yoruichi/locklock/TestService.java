package com.yoruichi.locklock;

import com.yoruichi.locklock.annotations.Synchronized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    private Logger logger = LoggerFactory.getLogger(TestService.class);

    private int num;

    @Synchronized(name = "test", waitTimeInMilliSeconds = "${wait:9}")
    public void incrementOne() {
        num++;
    }

    public int getNum() {
        return num;
    }
}
