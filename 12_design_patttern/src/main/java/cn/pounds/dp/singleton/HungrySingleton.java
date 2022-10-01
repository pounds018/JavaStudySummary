package cn.pounds.dp.singleton;

/**
 * @Author: Administrator
 * @Date: 2022/4/5 13:59
 * @Description: 饿汉式单例
 * 特点:
 * 1. 单例对象 hungrySingleton被 static 修饰, 只有类被加载的时候会创建一个HungrySingleton实例.
 * 2. static修饰的属性是 类共享属性, 所有线程共享通一个对象, 无线程安全问题
 * 3. 如果单例对象过多, 无论是否使用到这个单例对象, 在类被加载时都会创建一个实例, 造成内存占用过大
 * 4. 没有解决反射和序列化破坏单例对象的问题.
 */
public class HungrySingleton {
    private static final HungrySingleton hungrySingleton = new HungrySingleton();

    private HungrySingleton(){};

    public static HungrySingleton getInstance(){
        return hungrySingleton;
    }



}
