package cn.pounds.elegant.log.test.bean;

/**
 * @author: pounds
 * @date: 2022/2/26 23:32
 * @desc:
 */
public class RequestVo {
	private String deliveryOrderNo;
	private String userId;
	private String condition;

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getDeliveryOrderNo() {
		return deliveryOrderNo;
	}

	public void setDeliveryOrderNo(String deliveryOrderNo) {
		this.deliveryOrderNo = deliveryOrderNo;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}
