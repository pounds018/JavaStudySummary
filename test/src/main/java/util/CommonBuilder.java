package util;

import bean.Student;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CommonBuilder<T> {
    private final Supplier<T> constructor;
//    private List<Consumer<T>> setters = new ArrayList<>();
    private Consumer<T> firstSetter;

    private CommonBuilder(Supplier<T> constructor) {
        this.constructor = constructor;
    }

    public static <T> CommonBuilder<T> builder(Supplier<T> constructor) {
        return new CommonBuilder<>(constructor);
    }

    /**
     *
     * @param biConsumer --- set方法
     * @param p --- 属性
     * @return --- CommonBuilder
     * @param <P> --- 要设置的属性类型
     */
    public <P> CommonBuilder<T> with(BiConsumer<T, P> biConsumer, P p) {
        // 写法1:
        // instance -> biConsumer.accept(instance, p) 等价于new Consumer()的匿名子类
        // XxxClass::setXxx就是biConsumer, 本质是反射调用, 即instance.setXxx(p).
        // Consumer<T> tConsumer = instance -> biConsumer.accept(instance, p);
        //  setters.add(tConsumer);
        if (firstSetter == null) {
            firstSetter = instance -> biConsumer.accept(instance, p);
        } else {
            firstSetter = firstSetter.andThen(instance-> biConsumer.accept(instance, p));
        }
        return this;
    }

    public T build() {
        T instance = constructor.get();
        firstSetter.accept(instance);
//        setters.forEach(dInject -> dInject.accept(instance));
        return instance;
    }

    public static void main(String[] args) {
        BiConsumer<Student, String> setName = Student::setName;
        Supplier<Student> aNew = Student::new;

        Student student = CommonBuilder.builder(Student::new)
                .with(Student::setName, "张三")
                .with(Student::setAge, "12")
                .with(Student::setGender, "male")
                .build();
        System.out.println();
    }

}
