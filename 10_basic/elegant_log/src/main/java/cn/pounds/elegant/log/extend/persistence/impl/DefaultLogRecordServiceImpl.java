package cn.pounds.elegant.log.extend.persistence.impl;


import cn.pounds.elegant.log.bean.OpLogRecord;
import cn.pounds.elegant.log.extend.persistence.ILogRecordService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.LogRecord;

/**
 * @author: pounds
 * @desc: 默认的日志处理类
 * @date: 2022.2.20 22:14
 */
public class DefaultLogRecordServiceImpl implements ILogRecordService {
    private final static Logger logger = LoggerFactory.getLogger(DefaultLogRecordServiceImpl.class);
    @Override
    public void record(OpLogRecord logRecord) {
        logger.info("【logRecord】log={}", new Gson().toJson(logRecord));
    }
}