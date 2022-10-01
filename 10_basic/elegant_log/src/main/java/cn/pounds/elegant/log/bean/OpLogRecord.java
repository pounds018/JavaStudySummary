package cn.pounds.elegant.log.bean;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

/**
 * @author: pounds
 * @date: 2022/2/26 22:37
 * @desc:
 */
public class OpLogRecord {
	private Serializable id;
	private String tenant;
	@NotBlank(message = "bizAppName required")
	@Length(max = 200, message = "bizAppName max length is 200")
	private String bizAppName;

	@NotBlank(message = "bizAppId required")
	@Length(max = 200, message = "bizAppId max length is 200")
	private String bizAppId;

	@NotBlank(message = "bizNo required")
	@Length(max = 200, message = "bizNo max length is 200")
	private String bizNo;

	@NotBlank(message = "operator required")
	@Length(max = 63, message = "operator max length 63")
	private String operator;

	@NotBlank(message = "opAction required")
	@Length(max = 511, message = "operator max length 511")
	private String action;

	private boolean fail;
	private String opType;
	private Date createTime;
	private String detail;

	public Serializable getId() {
		return id;
	}

	public void setId(Serializable id) {
		this.id = id;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
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

	public String getBizNo() {
		return bizNo;
	}

	public void setBizNo(String bizNo) {
		this.bizNo = bizNo;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public boolean isFail() {
		return fail;
	}

	public void setFail(boolean fail) {
		this.fail = fail;
	}

	public String getOpType() {
		return opType;
	}

	public void setOpType(String opType) {
		this.opType = opType;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public static OpLogRecordBuilder builder() {
		return new OpLogRecordBuilder();
	}

	public static final class OpLogRecordBuilder {
		private Serializable id;
		private String tenant;
		private String bizAppName;
		private String bizAppId;
		private String bizNo;
		private String operator;
		private String action;
		private boolean fail;
		private String opType;
		private Date createTime;
		private String detail;

		private OpLogRecordBuilder() {
		}

		public OpLogRecordBuilder id(Serializable id) {
			this.id = id;
			return this;
		}

		public OpLogRecordBuilder tenant(String tenant) {
			this.tenant = tenant;
			return this;
		}

		public OpLogRecordBuilder bizAppName(String bizAppName) {
			this.bizAppName = bizAppName;
			return this;
		}

		public OpLogRecordBuilder bizAppId(String bizAppId) {
			this.bizAppId = bizAppId;
			return this;
		}

		public OpLogRecordBuilder bizNo(String bizNo) {
			this.bizNo = bizNo;
			return this;
		}

		public OpLogRecordBuilder operator(String operator) {
			this.operator = operator;
			return this;
		}

		public OpLogRecordBuilder action(String action) {
			this.action = action;
			return this;
		}

		public OpLogRecordBuilder fail(boolean fail) {
			this.fail = fail;
			return this;
		}

		public OpLogRecordBuilder opType(String opType) {
			this.opType = opType;
			return this;
		}

		public OpLogRecordBuilder createTime(Date createTime) {
			this.createTime = createTime;
			return this;
		}

		public OpLogRecordBuilder detail(String detail) {
			this.detail = detail;
			return this;
		}

		public OpLogRecord build() {
			OpLogRecord opLogRecord = new OpLogRecord();
			opLogRecord.setId(id);
			opLogRecord.setTenant(tenant);
			opLogRecord.setBizAppName(bizAppName);
			opLogRecord.setBizAppId(bizAppId);
			opLogRecord.setBizNo(bizNo);
			opLogRecord.setOperator(operator);
			opLogRecord.setAction(action);
			opLogRecord.setFail(fail);
			opLogRecord.setOpType(opType);
			opLogRecord.setCreateTime(createTime);
			opLogRecord.setDetail(detail);
			return opLogRecord;
		}
	}
}
