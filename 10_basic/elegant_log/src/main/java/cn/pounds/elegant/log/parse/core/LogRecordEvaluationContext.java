package cn.pounds.elegant.log.parse.core;

import cn.pounds.elegant.log.parse.context.LogRecordContext;
import cn.pounds.elegant.log.parse.func.ParseFunctionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: pounds
 * @date: 2022/2/20 21:28
 * @desc:
 */
public class LogRecordEvaluationContext extends MethodBasedEvaluationContext {
    private final static Logger logger = LoggerFactory.getLogger(LogRecordEvaluationContext.class);
    /**
     * 保存结果对象的变量的名称。
     */
    public static final String RESULT_VARIABLE = "_ret";
    public static final String ERROR_MSG_VARIABLE = "_errorMsg";
    /**
     *  不可方法的变量集合
     */
    private final Set<String> unavailableVariables = new HashSet<>(1);

    /**
     *
     * @param rootObject
     * @param method --- 目标方法
     * @param arguments --- 目标方法上面的参数
     * @param parameterNameDiscoverer --- 参数名称获取类
     * @param ret --- 目标方法执行结果
     * @param errorMsg --- 异常信息
     */
    public LogRecordEvaluationContext(Object rootObject,
                                      Method method,
                                      Object[] arguments,
                                      ParameterNameDiscoverer parameterNameDiscoverer,
                                      Object ret, String errorMsg,
                                      ParseFunctionFactory parseFunctionFactory) {
       //把方法的参数都放到 SpEL 解析的 RootObject 中
       super(rootObject, method, arguments, parameterNameDiscoverer);
       //把 LogRecordContext 中的变量都放到 RootObject 中
        LogRecordContext.getVariables().forEach(this::setVariable);

        // 把前置方法也设置进去
        parseFunctionFactory.getFunctionMap().forEach((key, function) -> {
            try {
                Method applyMethod = function.getClass().getDeclaredMethod("apply", Object.class);
                setVariable(key, applyMethod);
            } catch (Exception e){
                logger.error("set func variable error, this is no such method in custom parse func");
            }
        });

        //把方法的返回值和 ErrorMsg 都放到 RootObject 中
        setVariable(RESULT_VARIABLE, ret);
        setVariable(ERROR_MSG_VARIABLE, errorMsg);
    }

    /**
     *  添加不可访问的变量名称
     */
    public void addUnavailableVariable(String name) {
        this.unavailableVariables.add(name);
    }

    /**
     *  读取变量的值
     */
    @Override
    public Object lookupVariable( String name) {
        if (this.unavailableVariables.contains(name)) {
            throw new RuntimeException(String.format("Unavailable variable %s",name));
        }
        return super.lookupVariable(name);
    }
}