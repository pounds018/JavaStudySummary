package cn.pounds.elegant.log.aop;

import cn.pounds.elegant.log.parse.template.LogRecordOperationSource;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;
import org.springframework.lang.Nullable;

/**
 * @author: pounds
 * @date: 2022/2/20 22:28
 * @desc:
 */
public class BeanFactoryLogRecordAdvisor extends AbstractBeanFactoryPointcutAdvisor {
	private Pointcut pointcut;

	public BeanFactoryLogRecordAdvisor() {
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

	public void setPointcut(Pointcut pointcut){
		this.pointcut = pointcut;
	}

}
