package cn.pounds.cpu;

/**
 * @Author: pounds
 * @Date: 2022/7/23 0:09
 * @Description: cpu高占用演示demo的模型类
 */
public class HighCpu {
    private String name;
    private Integer age;

    public HighCpu(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
