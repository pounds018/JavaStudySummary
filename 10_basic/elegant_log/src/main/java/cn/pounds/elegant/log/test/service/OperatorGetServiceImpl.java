package cn.pounds.elegant.log.test.service;

import cn.pounds.elegant.log.bean.Operator;
import cn.pounds.elegant.log.extend.operator.IOperatorGetService;
import org.springframework.stereotype.Service;

/**
 * @author: pounds
 * @date: 2022/2/26 23:51
 * @desc:
 */
@Service
public class OperatorGetServiceImpl implements IOperatorGetService {
	@Override
	public Operator getUser() {
		Operator operator = new Operator();
		operator.setId("1008611");
		operator.setName("张三");
		return operator;
	}
}
