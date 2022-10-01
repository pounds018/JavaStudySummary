package cn.pounds.elegant.log.bean;

/**
 * @author: pounds
 * @date: 2022/2/20 21:48
 * @desc:
 */
public class OpLogAnnotations {
	/**
	 * 成功模板
	 */
	private String successLog;

	/**
	 * 失败模板
	 */
	private String failLog;

	/**
	 * 操作用户
	 */
	private String operator;
	/**
	 * 业务appName
	 */
	private String bizAppName;
	/**
	 * 业务id
	 */
	private String bizAppId;

	/**
	 * 操作类型: crud
	 */
	private String opType;

	/**
	 * 详情
	 */
	private String detail;

	/**
	 * 操作记录条件
	 */
	private String condition;

	public String getSuccessLog() {
		return successLog;
	}

	public void setSuccessLog(String successLog) {
		this.successLog = successLog;
	}

	public String getFailLog() {
		return failLog;
	}

	public void setFailLog(String failLog) {
		this.failLog = failLog;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getBizAppName() {
		return bizAppName;
	}

	public void setBizAppName(String bizAppName) {
		this.bizAppName = bizAppName;
	}

	public String getBizAppId() {
		return bizAppId;
	}

	public void setBizAppId(String bizAppId) {
		this.bizAppId = bizAppId;
	}

	public String getOpType() {
		return opType;
	}

	public void setOpType(String opType) {
		this.opType = opType;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public static OpLogAnnotationsBuilder builder() {
		return new OpLogAnnotationsBuilder();
	}

	public static final class OpLogAnnotationsBuilder {
		private String successLog;
		private String failLog;
		private String operator;
		private String bizAppName;
		private String bizAppId;
		private String opType;
		private String detail;
		private String condition;

		private OpLogAnnotationsBuilder() {
		}

		public OpLogAnnotationsBuilder successLog(String successLog) {
			this.successLog = successLog;
			return this;
		}

		public OpLogAnnotationsBuilder failLog(String failLog) {
			this.failLog = failLog;
			return this;
		}

		public OpLogAnnotationsBuilder operator(String operator) {
			this.operator = operator;
			return this;
		}

		public OpLogAnnotationsBuilder bizAppName(String bizAppName) {
			this.bizAppName = bizAppName;
			return this;
		}

		public OpLogAnnotationsBuilder bizAppId(String bizAppId) {
			this.bizAppId = bizAppId;
			return this;
		}

		public OpLogAnnotationsBuilder opType(String opType) {
			this.opType = opType;
			return this;
		}

		public OpLogAnnotationsBuilder detail(String detail) {
			this.detail = detail;
			return this;
		}

		public OpLogAnnotationsBuilder condition(String condition) {
			this.condition = condition;
			return this;
		}

		public OpLogAnnotations build() {
			OpLogAnnotations opLogAnnotations = new OpLogAnnotations();
			opLogAnnotations.setSuccessLog(successLog);
			opLogAnnotations.setFailLog(failLog);
			opLogAnnotations.setOperator(operator);
			opLogAnnotations.setBizAppName(bizAppName);
			opLogAnnotations.setBizAppId(bizAppId);
			opLogAnnotations.setOpType(opType);
			opLogAnnotations.setDetail(detail);
			opLogAnnotations.setCondition(condition);
			return opLogAnnotations;
		}
	}
}
