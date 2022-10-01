package cn.pounds.elegant.log.config;

import cn.pounds.elegant.log.aop.BeanFactoryLogRecordAdvisor;
import cn.pounds.elegant.log.aop.LogRecordInterceptor;
import cn.pounds.elegant.log.aop.OpLogRecordPointCut;
import cn.pounds.elegant.log.extend.custom.IParseFunction;
import cn.pounds.elegant.log.extend.custom.impl.DefaultParseFunctionServiceImpl;
import cn.pounds.elegant.log.parse.core.LogFunctionParser;
import cn.pounds.elegant.log.parse.func.*;
import cn.pounds.elegant.log.parse.func.impl.DefaultDiffItemsToLogContentService;
import cn.pounds.elegant.log.parse.func.impl.DefaultFunctionServiceImpl;
import cn.pounds.elegant.log.extend.operator.impl.DefaultOperatorGetServiceImpl;
import cn.pounds.elegant.log.extend.operator.IOperatorGetService;
import cn.pounds.elegant.log.parse.func.impl.DiffParseFunction;
import cn.pounds.elegant.log.parse.template.LogRecordOperationSource;
import cn.pounds.elegant.log.extend.persistence.impl.DefaultLogRecordServiceImpl;
import cn.pounds.elegant.log.extend.persistence.ILogRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.List;

/**
 * @author: pounds
 * @date: 2022/2/20 22:16
 * @desc:
 */
@Configuration
@EnableConfigurationProperties(LogRecordProperties.class)
public class LogRecordProxyAutoConfiguration implements ImportAware {
	private final static Logger logger = LoggerFactory.getLogger(LogRecordProxyAutoConfiguration.class);

	private AnnotationAttributes enableLogRecord;

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public LogRecordOperationSource logRecordOperationSource() {
		return new LogRecordOperationSource();
	}

	@Bean
	public ParseFunctionFactory parseFunctionFactory(@Autowired List<IParseFunction> parseFunctions) {
		return new ParseFunctionFactory(parseFunctions);
	}

	@Bean
	public LogFunctionParser logFunctionParser(IFunctionService functionService) {
		return new LogFunctionParser(functionService);
	}

	@Bean
	public DiffParseFunction diffParseFunction(IDiffItemsToLogContentService diffItemsToLogContentService) {
		DiffParseFunction diffParseFunction = new DiffParseFunction();
		diffParseFunction.setDiffItemsToLogContentService(diffItemsToLogContentService);
		return diffParseFunction;
	}
	/**
	 * @return 切点
	 */
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public OpLogRecordPointCut opLogRecordPointCut(){
		OpLogRecordPointCut opLogRecordPointCut = new OpLogRecordPointCut();
		opLogRecordPointCut.setLogRecordOperationSource(logRecordOperationSource());
		return opLogRecordPointCut;
	}

	/**
	 * 增强方法, 即advice
	 * @param functionService
	 * @return
	 */
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public LogRecordInterceptor logRecordInterceptor(IFunctionService functionService, DiffParseFunction diffParseFunction) {
		LogRecordInterceptor interceptor = new LogRecordInterceptor();
		interceptor.setLogRecordOperationSource(logRecordOperationSource());
		interceptor.setTenant(enableLogRecord.getString("tenant"));
		interceptor.setLogFunctionParser(logFunctionParser(functionService));
		interceptor.setDiffParseFunction(diffParseFunction);
		return interceptor;
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryLogRecordAdvisor logRecordAdvisor(IFunctionService functionService, DiffParseFunction diffParseFunction) {
		BeanFactoryLogRecordAdvisor advisor =
				new BeanFactoryLogRecordAdvisor();
		advisor.setPointcut(opLogRecordPointCut());
		advisor.setAdvice(logRecordInterceptor(functionService, diffParseFunction));
		return advisor;
	}

	@Bean
	@ConditionalOnMissingBean(IFunctionService.class)
	public IFunctionService functionService(ParseFunctionFactory parseFunctionFactory) {
		return new DefaultFunctionServiceImpl(parseFunctionFactory);
	}

	// ----------------------------------- 对外扩展接口 -------------------------------------------- \\
	@Bean
	@ConditionalOnMissingBean(IParseFunction.class)
	public DefaultParseFunctionServiceImpl parseFunction() {
		return new DefaultParseFunctionServiceImpl();
	}

	@Bean
	@ConditionalOnMissingBean(IOperatorGetService.class)
	@Role(BeanDefinition.ROLE_APPLICATION)
	public IOperatorGetService operatorGetService() {
		return new DefaultOperatorGetServiceImpl();
	}

	@Bean
	@ConditionalOnMissingBean(ILogRecordService.class)
	@Role(BeanDefinition.ROLE_APPLICATION)
	public ILogRecordService recordService() {
		return new DefaultLogRecordServiceImpl();
	}

	@Bean
	@ConditionalOnMissingBean(IDiffItemsToLogContentService.class)
	@Role(BeanDefinition.ROLE_APPLICATION)
	public IDiffItemsToLogContentService diffItemsToLogContentService(IFunctionService functionService, LogRecordProperties logRecordProperties) {
		return new DefaultDiffItemsToLogContentService(functionService, logRecordProperties);
	}


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableLogRecord = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableLogRecord.class.getName(), false));
		if (this.enableLogRecord == null) {
			logger.info("@enableLogRecord is not present on importing class");
		}
	}
}
