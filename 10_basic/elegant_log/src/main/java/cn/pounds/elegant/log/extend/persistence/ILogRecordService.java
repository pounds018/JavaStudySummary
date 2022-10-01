package cn.pounds.elegant.log.extend.persistence;

import cn.pounds.elegant.log.bean.OpLogRecord;

import java.util.logging.LogRecord;

/**
 * @author: pounds
 * @date: 2022/2/20 22:13
 * @desc:
 */
public interface ILogRecordService {
	/**
	 * 保存 log
	 *
	 * @param logRecord 日志实体
	 */
	void record(OpLogRecord logRecord);

}
