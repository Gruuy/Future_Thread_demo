package com.gruuy.java8.future.service;

import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * @author: Gruuy
 * @remark:
 * @date: Create in 16:36 2019/8/28
 */
@Service
public class TestServiceImpl {
    public double getPrice(double a,double b){
        //抛异常
        if(new Random().nextInt(2)>0){
            int i=1/0;
        }
        return new Random().nextDouble()*a*b;
    }

    public double getPrice2(double a,double b){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace( );
        }
        return new Random().nextDouble()*a*b;
    }
    public double getPrice2_1(double a){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace( );
        }
        return new Random().nextDouble()*a;
    }

    /**
     * 复现join等待机制
     * @author: Gruuy
     * @date: 2019/8/29
     */
    public double getPrice3(double a){
        try {
            if(new Random().nextDouble()<0.5){
                Thread.sleep(1000);
            }else {
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace( );
        }
        return new Random().nextDouble()*a;
    }
}
