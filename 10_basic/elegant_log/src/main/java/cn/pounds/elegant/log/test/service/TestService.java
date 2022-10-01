package cn.pounds.elegant.log.test.service;

import cn.pounds.elegant.log.aop.OpLog;
import cn.pounds.elegant.log.test.bean.RequestVo;
import org.springframework.stereotype.Service;

/**
 * @author: pounds
 * @date: 2022/2/21 0:33
 * @desc:
 */
@Service
public class TestService {

	@OpLog(successLog = "修改了订单的配送员：从“#{queryOldUser(#request.deliveryOrderNo)}”", bizAppId = "aaa", bizAppName =
			"aaa")
	public String test(RequestVo request){
		return "测试成功";
	}
}
