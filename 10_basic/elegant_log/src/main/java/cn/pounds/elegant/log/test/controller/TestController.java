package cn.pounds.elegant.log.test.controller;

import cn.pounds.elegant.log.aop.OpLog;
import cn.pounds.elegant.log.aop.OpLogs;
import cn.pounds.elegant.log.parse.context.LogRecordContext;
import cn.pounds.elegant.log.test.bean.RequestVo;
import cn.pounds.elegant.log.test.service.TestService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author: pounds
 * @date: 2022/2/21 0:33
 * @desc:
 */
@RestController
@RequestMapping(value = "/test")
public class TestController {
	@Resource
	private TestService test;

	@OpLogs(OpLogRecords = {
			@OpLog(successLog = "修改了订单的配送员：从“#{queryOldUser(#request.deliveryOrderNo)}”, " +
					"修改到“#{deveryUser(#request" +
					".userId)}”", bizAppId = "#{#request.deliveryOrderNo}", bizAppName = "pounds"),
			@OpLog(successLog = "修改了订单的配送员：从“#{queryOldUser(#request.deliveryOrderNo)}”, " +
					"修改到“#{deveryUser(#request" +
					".userId)}”", bizAppId = "#{#request.deliveryOrderNo}", bizAppName = "pounds"),
			@OpLog(successLog = "测试三元表达式, #{#request.userId == 'pounds018'? true : false}", bizAppId = "张三",
					bizAppName = "王五"),
			@OpLog(successLog = "测试condition, #{#request.userId == 'pounds018'? true : false}", bizAppId = "张三",
					bizAppName = "王五", condition = "#{T(org.apache.commons.lang3.StringUtils).isEmpty(#request" +
					".condition)}")
	})
	@RequestMapping(value = "/addr", method = RequestMethod.GET)
	public String modifyAddress(@RequestBody RequestVo request){
		// 更新派送信息 电话，收件人、地址
		// doUpdate(request);
		LogRecordContext.putVariable("#request.deliveryOrderNo", "1008611");
		System.out.println("来了");
		return test.test(request);
	}
}
