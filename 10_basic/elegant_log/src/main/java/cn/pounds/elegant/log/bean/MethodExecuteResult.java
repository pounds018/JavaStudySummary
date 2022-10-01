package cn.pounds.elegant.log.bean;


/**
 * @author: pounds
 * @date: 2022/2/20 22:43
 * @desc: 切点方法执行结果
 */
public class MethodExecuteResult {
	public Throwable throwable;
	private boolean success;
	private String message;

	public MethodExecuteResult(boolean success, Throwable throwable, String message) {
		this.success = success;
		this.throwable = throwable;
		this.message = message;
	}

	public MethodExecuteResult() {
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getErrorMsg() {
		return success ? "" : message;
	}
}
