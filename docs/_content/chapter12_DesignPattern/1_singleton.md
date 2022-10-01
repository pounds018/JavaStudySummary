## 12.1.1 使用单例模式的几个原则:
    1. 私有化构造器: 防止通过构造器创建出不同的实例.
    2. 以静态方法或者枚举返回实例: 保证实例的唯一性.
    3. 确保实例只有一个,防止线程安全问题: 即保证创建实例的方法是没有线程安全问题的
    4. 确保实例无法通过反射创建对象,并保证实例在反序列化的时候返回的是同一个对象: 实际上都是反射的原因.
## 12.1.2 单例模式的几种实现方式:
1. 饿汉式单例: 

   ```java
      public class HungrySingleton {
           private static final HungrySingleton HUNGRY_SINGLETON_INSTANCE = new HungrySingleton();
       
           private HungrySingleton() {
               System.out.println("饿汉式单例,私有化构造器了");
           }
       
           public static HungrySingleton getInstance() {
               return HUNGRY_SINGLETON_INSTANCE;
           }
   
       }
   ```

   - 特点: 
     1. 通过类加载的特性,只在类加载的时候通过构造器创建一个实例对象.
     2. 所有的对象都共用一个实例,避免了线程安全问题
     3. 无论用不用实例都会被创建,`如果这样的单例多了,却又没有使用,比较浪费空间`
     4. 无法解决反射和反序列话的问题

   > 通常: 没有反射或者反序列话的需求的话,饿汉式单例是使用的比较多的一种单例模式.

   测试:

   ```java
       /**
        * 饿汉式单例测试
        */
       @Test
       public void HungrySingletonTest(){
           HungrySingleton instance1 = HungrySingleton.getInstance();
           HungrySingleton instance2 = HungrySingleton.getInstance();
           System.out.println("instance1 的地址为: " + instance1);
           System.out.println("instance2 的地址为: " + instance2);
           System.out.println("判断是否是单例: " + (instance1 == instance2));
   
           for (int i = 0; i < 10; i++) {
               EXECUTOR.submit(() -> {
                   HungrySingleton instance = HungrySingleton.getInstance();
                   System.out.println("instance 的地址为: " + instance);
               });
           }
   
           try {
               Thread.sleep(10);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }
   ```

   结果:

   ```java
   instance1 的地址为: HungrySingleton@15327b79
   instance2 的地址为: HungrySingleton@15327b79
   判断是否是单例: true
   instance 的地址为: HungrySingleton@15327b79
   instance 的地址为: HungrySingleton@15327b79
   instance 的地址为: HungrySingleton@15327b79
   instance 的地址为: HungrySingleton@15327b79
   instance 的地址为: HungrySingleton@15327b79
   instance 的地址为: HungrySingleton@15327b79
   instance 的地址为: HungrySingleton@15327b79
   instance 的地址为: HungrySingleton@15327b79
   instance 的地址为: HungrySingleton@15327b79
   instance 的地址为: HungrySingleton@15327b79
   ```

   `反射破坏演示`: 尽管单例类的构造器设置为了私有方法, 但是反射能够设置私有变量是否能够被访问, 当私有方法被设置为可以访问后, 私有性就被破坏了, 技能构通过构造器创建对象了.

   ```java
           // 反射破坏:
           HungrySingleton instance3 = HungrySingleton.getInstance();
           HungrySingleton instance4 = null;
           Class<HungrySingleton> hungrySingletonClass = HungrySingleton.class;
           try {
               Constructor<HungrySingleton> declaredConstructor = hungrySingletonClass.getDeclaredConstructor();
               declaredConstructor.setAccessible(true);
   
               instance4 = declaredConstructor.newInstance();
           } catch (Exception e) {
               e.printStackTrace();
           }
   
   instance3 的地址为: cn.pounds.dp.singleton.HungrySingleton@15327b79
   instance4 的地址为: cn.pounds.dp.singleton.HungrySingleton@16b3fc9e
   ```

   

2. 懒汉式单例: 延迟加载,在使用的时候才会去创建实例 

   ```java
   
   ```

   