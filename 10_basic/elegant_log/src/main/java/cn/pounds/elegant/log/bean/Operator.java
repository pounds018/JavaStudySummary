package cn.pounds.elegant.log.bean;

/**
 * @author: pounds
 * @date: 2022/2/26 22:10
 * @desc:
 */
public class Operator {
	private String name;
	private String id;

	public Operator() {
	}

	public Operator(String name, String id) {
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
