package cn.pounds.elegant.log.config;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNullApi;

/**
 * @author: pounds
 * @date: 2022/2/20 22:21
 * @desc:
 */
public class LogRecordConfigureSelector extends AdviceModeImportSelector<EnableLogRecord> {
	private static final String ASPECTJ_CONFIG_NAME = "cn.pounds.elegant.log.config.LogRecordProxyAutoConfiguration";
	/**
	 * @param adviceMode --- 需要导入类的元数据
	 * @return --- 需要导入类的全路径
	 */
	@Override
	public String[] selectImports(AdviceMode adviceMode) {
		switch(adviceMode){
			case PROXY:
				return new String[]{LogRecordProxyAutoConfiguration.class.getName()};
			case ASPECTJ:
				return new String[]{"cn.pounds.elegant.log.config.LogRecordProxyAutoConfiguration"};
			default:
				return null;
		}
	}
}
