package com.gruuy.java8.future.controller;

import com.gruuy.java8.future.service.TestServiceImpl;
import com.sun.deploy.util.ArrayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static java.util.stream.Collectors.toList;

/**
 * @author: Gruuy
 * @remark: 多线程  流水线demo
 * @date: Create in 15:29 2019/8/28
 */
@RestController
public class TestController {
    @Autowired
    private TestServiceImpl testService;
    
    /**
     * Future 多线程异步返回值
     * @author: Gruuy
     * @date: 2019/8/28
     */
    @RequestMapping("/test")
    public String test(){
        //定长线程池
        ExecutorService executor= Executors.newFixedThreadPool(8);
        Future<Double> future=executor.submit(()->{
            //模拟处理2s
            Thread.sleep(2000);
            Random random=new Random();
           return random.nextDouble();
        });
        try {
            //获取  1是数量  后面的是单位
            Double result=future.get(1, TimeUnit.SECONDS);
            System.out.println(result );
            return String.valueOf(result);
        } catch (InterruptedException e) {
            e.printStackTrace( );
        } catch (ExecutionException e) {
            e.printStackTrace( );
        } catch (TimeoutException e) {
            System.out.println("计算超时！");
            e.printStackTrace( );
        }
        return null;
    }
    
    /**
     * CompletableFuture 多线程异步返回
     * @author: Gruuy
     * @date: 2019/8/28
     */
    @RequestMapping("/test2")
    public String test2(){
        //定义
        CompletableFuture<Double> futurePrice=new CompletableFuture<>();
        //定长线程池
        ExecutorService executor=Executors.newFixedThreadPool(10);
        executor.execute(()->{
            try {
                //业务方法异步化
                double price=testService.getPrice(0.1,0.2);
                //模拟处理并返回
                Thread.sleep(500);
                if(new Random().nextInt(3)>0){
                    throw new Exception("模拟抛出异常");
                }
                futurePrice.complete(price);
            } catch (Exception e) {
                //要用这个抛出异步异常，否则会一直阻塞
                futurePrice.completeExceptionally(e);
            }
        });
        try {
            //模拟doElseSomething
            Thread.sleep(2000);
            return String.valueOf(futurePrice.get(1,TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace( );
        } catch (ExecutionException e) {
            //传递回来的信息包含完整类名，所以把它给砍掉
            e.printStackTrace( );
            return e.getMessage().substring(e.getMessage().lastIndexOf(':')+1);
        } catch (TimeoutException e) {
            e.printStackTrace( );
        }
        return null;
    }
    
    /**
     * 工厂方法创建CompletableFuture
     * @author: Gruuy
     * @date: 2019/8/28
     */
    @RequestMapping("/test3")
    public String test3(){
        //定长线程池
        ExecutorService executor=Executors.newFixedThreadPool(10);
        //dosomething
        try {
            //利用工厂方法自动生成带有错误处理的异步结果
            Future<Double> future=CompletableFuture.supplyAsync(()->testService.getPrice(0.1,0.2),executor);
            Thread.sleep(1000);
            return String.valueOf(future.get(1,TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace( );
        } catch (ExecutionException e) {
            //传递回来的信息包含完整类名，所以把它给砍掉
            e.printStackTrace( );
            return e.getMessage().substring(e.getMessage().lastIndexOf(':')+1);
        } catch (TimeoutException e) {
            e.printStackTrace( );
        }
        return null;
    }
    
    /**
     * 测试并行流
     * @author: Gruuy
     * @date: 2019/8/28
     */
    @RequestMapping("/test4")
    public String test4(){
        //定长线程池
        ExecutorService executor=Executors.newFixedThreadPool(10);
        //模拟执行   每次gerPrice2会阻塞1s
        List<Double> list= Arrays.asList(0.1,0.2,0.3,0.4,0.5,0.6);
        long startTime=System.currentTimeMillis();
        //同步流
        List<String> str=list.stream().map(a->String.valueOf(testService.getPrice2(a,a))).collect(toList());
        long endTime=System.currentTimeMillis();
        System.out.println("同步流："+(endTime-startTime));
        startTime=System.currentTimeMillis();
        //异步流
        List<String> str2=list.parallelStream().map(a->String.valueOf(testService.getPrice2(a,a))).collect(toList());
        endTime=System.currentTimeMillis();
        System.out.println("异步流："+(endTime-startTime));
        return null;
    }
    
    /**
     * 测试CompletableFuture结合流
     * @author: Gruuy
     * @date: 2019/8/29
     */
    @RequestMapping("/test5")
    @SuppressWarnings("all")
    public List<String> test5(){
        //定长线程池
        ExecutorService executor=Executors.newFixedThreadPool(10);
        //模拟执行   每次gerPrice2会阻塞1s
        List<Double> list= Arrays.asList(0.1,0.2,0.3,0.4,0.5,0.6);
        long startTime=System.currentTimeMillis();
        //使用CompletableFuture工厂创建  因为getPrice2会阻塞1s  这里异步执行则是返回的都是未来对象的列表
        List<CompletableFuture<String>> str=list.stream().map(a->CompletableFuture.supplyAsync(()->String.valueOf(testService.getPrice3(a)),executor)).collect(toList());
        //未来对象的列表不一定都做完了  而且返回的是List<CompletableFuture<String>>  我们需要的是List<String>  所以把它变成List<String>而且等待所有Future执行完成
        //join方法会一个一个等待其结束   有get方法的含义，但是没有错误处理
        List<String> str3=str.stream().map(CompletableFuture::join).collect(toList());
        long endTime=System.currentTimeMillis();
        System.out.println("Time:"+(endTime-startTime));
        return str3;
    }

    /**
     * 定制执行器
     * @author: Gruuy
     * @date: 2019/8/29
     */
    @RequestMapping("/test6")
    public String test6(){
        //模拟执行   每次gerPrice2会阻塞1s
        List<Double> list= Arrays.asList(0.1,0.2,0.3,0.4,0.5,0.6);
        //获取系统线程数量
        System.out.print(Runtime.getRuntime().availableProcessors());
        //并行流最多只会有系统核心数量个数的线程  超出的话他不会再多开线程给你了  例如有9个需要多线程的   时间就会X2
        //创建一个线程池，线程池的数目为100何商店数目二者中较小的一个值
        final Executor executor=Executors.newFixedThreadPool(Math.min(list.size(),100),r->{
            //设置守护进程 ---这种方式不会阻止程序的关停
            Thread t=new Thread(r);
            t.setDaemon(true);
            return t;
        });
        //提交之后主线程直接GG  业务需要阻塞1s  但是因为那个是守护线程  所以他还是运算了
        CompletableFuture<String> str=CompletableFuture.supplyAsync(()->String.valueOf(testService.getPrice3(0.41)),executor);
        try {
            //这里还没运算完  已经GG了
            return str.get(100,TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace( );
        } catch (ExecutionException e) {
            e.printStackTrace( );
            return e.getMessage();
        } catch (TimeoutException e) {
            e.printStackTrace( );
            try {
                //但是实际上他还是算了的
                Thread.sleep(1000);
                String s =str.get();
                return s;
            } catch (InterruptedException e1) {
                e1.printStackTrace( );
            } catch (ExecutionException e1) {
                e1.printStackTrace( );
            }
        }
        return null;
    }
    
    /**
     * 测试组合式异步编程
     * @author: Gruuy
     * @date: 2019/8/29
     */
    @RequestMapping("/test7")
    @SuppressWarnings("all")
    public List<Double> test7(){
        long startTime=System.currentTimeMillis();
        List<Double> list=Arrays.asList(0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9);
        //线程池
        //如果使用自定义线程池的话需要*2  因为我们第一波已经动用了9个线程  第二波运算需要再用9个  如果只用list的大小是会阻塞的
        //那样就跟并行流一毛一样了
        Executor executor=Executors.newFixedThreadPool(Math.min(list.size()*2,100));
        //getPrice2_1(getPrice2_1(x))正常耗时：1+1=2
        //list.size()=9  所以标准耗时=9*2=18
        //正常流
        //List<Double> doubleList=list.stream().map(a->testService.getPrice2_1(testService.getPrice2_1(a))).collect(Collectors.toList());
        //time=18
        //并行流
//        List<Double> doubleList=list.parallelStream().map(a->testService.getPrice2_1(testService.getPrice2_1(a))).collect(Collectors.toList());
        //time=4
        //并行流  size>8(cpu线程数)的时候一样很日狗
        //我想他只要执行2s而已
        List<CompletableFuture<Double>> list2=list.stream()
                //先运算一波
                .map(a->CompletableFuture.supplyAsync(()->testService.getPrice2_1(a),executor))
                //再用运算的结果再算一波  第一波返回的结果是CompletableFuture  所以需要get回出来  这里不带参数get表示阻塞到等到它
                .map(future->CompletableFuture.supplyAsync(()-> {
                    try {
                        return testService.getPrice2_1(future.get());
                    } catch (InterruptedException e) {
                        e.printStackTrace( );
                    } catch (ExecutionException e) {
                        e.printStackTrace( );
                    }
                    return null;
                },executor))
                //结果再转回list
                .collect(toList());
        //等待流中的所有Future执行完毕，提取各自的返回值
        List<Double> doubleList = list2.stream().map(CompletableFuture::join).collect(toList());
        //2S!
        long endTime=System.currentTimeMillis();
        System.out.println("Time:"+(endTime-startTime));
        return doubleList;
    }

    /**
     * 测试合并
     * @author: Gruuy
     * @date: 2019/8/29
     */
    @RequestMapping("/test8")
    @SuppressWarnings("all")
    public String test8() {
        long startTime=System.currentTimeMillis();
        List<Double> list=Arrays.asList(0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9);
        //线程池
        //如果使用自定义线程池的话需要*2  因为我们第一波已经动用了9个线程  第二波运算需要再用9个  如果只用list的大小是会阻塞的
        //那样就跟并行流一毛一样了
        Executor executor=Executors.newFixedThreadPool(Math.min(list.size()*2,100));
        //结果合并
        //意思是两个异步操作之后合并在一起  先开一个线程获取list[0]的值  另外一个线程获取0.66  之后利用then.Combine方法与另一个合并
        //then.Combine第二个参数是指明两个参数的合并规则  这里是相乘
        Future<Double> future=CompletableFuture.supplyAsync(()->list.get(0),executor)
                .thenCombine(CompletableFuture.supplyAsync(()->0.66,executor),(a,b)->a*b);
        long endTime=System.currentTimeMillis();
        System.out.println("Time:"+(endTime-startTime));
        try {
            return future.get().toString();
        } catch (InterruptedException e) {
            e.printStackTrace( );
        } catch (ExecutionException e) {
            e.printStackTrace( );
        }
        return null;
    }
    /**
     * 测试thenapply与thencommpose
     * @author: Gruuy
     * @date: 2019/8/29
     */
    @RequestMapping("/test9")
    @SuppressWarnings("all")
    public String test9() throws ExecutionException, InterruptedException {
        //先通过异步获取一个Integer=100  之后异步操作100*10  最后转换成string类型
        //thenApply与thenApplyAsync就是开不开线程的区别
        //你可以理解成他就是一个转换类型的  传入类型与输出类型都自己进行识别
        //但是他只负责转CompletableFuture<？>里面的类型
        CompletableFuture<String> f=CompletableFuture.supplyAsync(()->{return 100;})
                .thenApplyAsync(i->i*10)
                .thenApply(i->i.toString());
        System.out.println("f:"+f.get());
        //而thenCompose则是合并  同样只合并CompletableFuture对象
        //先获取一个CompletableFuture对象
        CompletableFuture<String> f1=CompletableFuture.supplyAsync(()->{return 100;})
                //合并
                .thenCompose(i->{
                    //在里面用另外一个CompletableFuture对象与其合并
                    return CompletableFuture.supplyAsync(()->{return (i*10)+"";});
                //多个CompletableFuture对象合并的时候thenCompose很好用
                }).thenCompose(i->{
                    return CompletableFuture.supplyAsync(()->{return i+" wdnmd";});
                });
        System.out.println("f1:"+f1.get());
        return null;
    }
    
    /**
     * 测试优先返回
     * @author: Gruuy
     * @date: 2019/8/29
     */
    @RequestMapping("/test10")
    @SuppressWarnings("all")
    public String test10(){
        long startTime=System.currentTimeMillis();
        List<Double> list=Arrays.asList(0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9);
        //线程池
        //如果使用自定义线程池的话需要*2  因为我们第一波已经动用了9个线程  第二波运算需要再用9个  如果只用list的大小是会阻塞的
        //那样就跟并行流一毛一样了
        Executor executor=Executors.newFixedThreadPool(Math.min(list.size()*2,100));
        //获取一个CompletableFuture对象数组
        CompletableFuture[] futures=list.stream()
                //构造一波CompletableFuture
                .map(a->CompletableFuture.supplyAsync(()->testService.getPrice3(a),executor))
                //在每个CompletableFuture上注册一个操作，该操作会在CompletableFuture完成后使用它的返回值。
                //使用thenAccept将结果输出，它的参数就是 CompletableFuture的返回值。
                .map(f->f.thenAccept(System.out::println))
                //返回所有对象的数组
                .toArray(size->new CompletableFuture[size]);
        //allOf方法接受一个CompletableFuture构成的数组，数组中所有的CompletableFuture对象执行完成后，
        //它返回一个COmpletableFuture<Void>对象。所以你需要等待最初Stream中的所有CompletableFuture对象执行完毕，
        //对allOf方法返回的CompletableFuture执行join操作
        //这一步其实就是  等待上面的执行完成  否则他肯定比上面快
        Void aVoid=CompletableFuture.allOf(futures).join();
        long endTime=System.currentTimeMillis();
        return null;
    }


    /**
     * 测试有一个完成就直接返回
     * @author: Gruuy
     * @date: 2019/8/29
     */
    @SuppressWarnings("all")
    @RequestMapping("/test11")
    public String test11() throws ExecutionException, InterruptedException {
        long startTime=System.currentTimeMillis();
        List<Double> list=Arrays.asList(0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9);
        //线程池
        //如果使用自定义线程池的话需要*2  因为我们第一波已经动用了9个线程  第二波运算需要再用9个  如果只用list的大小是会阻塞的
        //那样就跟并行流一毛一样了
        Executor executor=Executors.newFixedThreadPool(Math.min(100*2,100));
        List<CompletableFuture<Double>> futures = list.stream()
                //构造一波CompletableFuture
                .map(a-> CompletableFuture.supplyAsync(()->{
                    Double b = testService.getPrice3(a);
                    System.out.println(b);
                    return b;
                },executor))
                //在每个CompletableFuture上注册一个操作，该操作会在CompletableFuture完成后使用它的返回值。
                //使用thenAccept将结果输出，它的参数就是 CompletableFuture的返回值。
                //以最后一个map的返回值为基准  别在这里输出
                //.map(f->f.thenAccept(System.out::println))
                .collect(toList());
        //轮询拿最先完成的返回值
        while (true){
            for (Future<Double> future : futures) {
                if(future.isDone()){
                   return future.get().toString();
                }
            }
        }
    }

}
