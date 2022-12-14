package cn.pounds.pojo;

/**
 * @Project: 07_elasticsearch
 * @Date: 2020/10/29 14:22
 * @author: by Martin
 * @Description: 准备封装数据
 */
public class Person {
    private Integer id;
    private String address;
    private Integer age;
    private String  name;

    public Person() {
    }

    public Person(Integer id, String address, Integer age, String name) {
        this.id = id;
        this.address = address;
        this.age = age;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
