package cn.pounds.dp.singleton;

/**
 * @Author: Administrator
 * @Date: 2022/4/5 13:59
 * @Description: 懒汉式单例
 */
public class LazySingleton {
    private static LazySingleton instance ;

    /**
     * 私有化构造器
     */
    private LazySingleton(){};

    /**
     * 存在多线程问题, 线程1在初始化单例对象但是没有赋值给instance对象的时候, 线程2进行判断, 会造成初始化两个实例的问题
     * @return
     */
    public static LazySingleton getInstance(){
        if (null != instance){
            instance = new LazySingleton();
        }
        return instance;
    }
}
