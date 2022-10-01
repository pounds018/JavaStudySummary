package cn.pounds.elegant.log.extend.operator;


import cn.pounds.elegant.log.bean.Operator;

/**
 * 获取操作人的方法
 * @author: pounds
 */
public interface IOperatorGetService {

    /**
     * 可以在里面外部的获取当前登陆的用户，比如 UserContext.getCurrentUser()
     *
     * @return 转换成Operator返回
     */
    Operator getUser();
}