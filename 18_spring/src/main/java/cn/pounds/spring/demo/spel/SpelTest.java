package cn.pounds.spring.demo.spel;

import org.junit.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @author: pounds
 * @date: 2022/2/23 21:01
 * @desc:
 */
public class SpelTest {

	/**
	 * 简单的分析某个字符串, 将其封装成expression对象
	 * ''包裹起来表示一个整体
	 */
	@Test
	public void test() {
		SpelExpressionParser parser = new SpelExpressionParser();
		Expression helloSpel = parser.parseExpression("'HELLO SPEL'");
		// value == HELLO SPEL
		String value = (String) helloSpel.getValue();
		System.out.println(value);
	}

	/**
	 * 演示spel可以使用一些字符串函数
	 */
	@Test
	public void test2() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression("'Hello World'.concat('!')");
		String message = (String) exp.getValue();
		// message == Hello World!
		System.out.println(message);

		// invokes 'getBytes()'
		exp = parser.parseExpression("'Hello World'.bytes");
		byte[] bytes = (byte[]) exp.getValue();

		// 链式调用
		exp = parser.parseExpression("'Hello World'.bytes.length");
		int length = (Integer) exp.getValue();
	}

	/**
	 * 演示spel中可以使用string的构造函数\
	 * public <T> T getValue(Class<T> desiredResultType)
	 * getValue(target.class), 表示将结果封装成target类型, 可能会出现类型转换异常
	 */
	@Test
	public void test3() {
		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression("new String('hello world').toUpperCase()");
		String message = exp.getValue(String.class);
		System.out.println(message);
	}

	/**
	 * spel更为常见的使用场景是根据一个表达式, 从某个对象中取出对应的值, 写入到表达式中进行处理
	 * 本质上是反射去取值, 并且必须要是public修饰
	 */
	@Test
	public void test4() {
		// Create and set a calendar
		GregorianCalendar c = new GregorianCalendar();
		c.set(1856, 7, 9);

		// The constructor arguments are name, birthday, and nationality.
		Inventor tesla = new Inventor("Nikola Tesla", c.getTime(), "Serbian");

		ExpressionParser parser = new SpelExpressionParser();

		Expression exp = parser.parseExpression("name");
		// 从tesla对象中取出 name属性
		String name = (String) exp.getValue(tesla);
		// name == "Nikola Tesla"

		exp = parser.parseExpression("name == 'Nikola Tesla'");
		// 从tesla对象中取出 name属性,然后判断是否与Nikola Tesla相等
		boolean result = exp.getValue(tesla, Boolean.class);
		// result == true
	}

	/**
	 * spel中的泛型, spel会根据泛型自动将表达式中的类型转换
	 */
	@Test
	public void test5() {
		SpelExpressionParser parser = new SpelExpressionParser();
		class Simple {
			public List<Boolean> booleanList = new ArrayList<Boolean>();
		}

		Simple simple = new Simple();
		simple.booleanList.add(true);

		EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();

		// "false" is passed in here as a String. SpEL and the conversion service
		// will recognize that it needs to be a Boolean and convert it accordingly.
		parser.parseExpression("booleanList[0]").setValue(context, simple, "false");

		// b is false
		Boolean b = simple.booleanList.get(0);
		System.out.println(b);
	}

	/**
	 * spel能够自动创建元素
	 */
	@Test
	public void test6() {
		class Demo {
			public List<String> list;
		}

		// Turn on:
		// - auto null reference initialization
		// - auto collection growing
		SpelParserConfiguration config = new SpelParserConfiguration(true, true);

		ExpressionParser parser = new SpelExpressionParser(config);

		Expression expression = parser.parseExpression("list[3]");

		Demo demo = new Demo();

		Object o = expression.getValue(demo);
		System.out.println(o);

		// demo.list will now be a real collection of 4 entries
		// Each entry is a new empty String
	}

	/**
	 * spel表达式中调用的方法只能是static修饰的方法
	 *
	 * @throws NoSuchMethodException --- 无方法异常
	 */
	@Test
	public void testStandardEvaluationContext() throws NoSuchMethodException {
		String content = "修改了订单的配送员：从“{queryOldUser{#request.deliveryOrderNo()}}”, 修改到“{deveryUser{#request.userId}}”";
		ExpressionParser spelExpressionParser = new SpelExpressionParser();
		SpelParserConfiguration config = new SpelParserConfiguration(true,true);
		class Request {
			public String name;
			public String deliveryOrderNo;
			public String userId;

			public Request(String name, String deliveryOrderNo, String userId) {
				this.name = name;
				this.deliveryOrderNo = deliveryOrderNo;
				this.userId = userId;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getName() {
				return name;
			}

			public String deliveryOrderNo(){
				return deliveryOrderNo;
			}

			public void setDeliveryOrderNo(String deliveryOrderNo){
				this.deliveryOrderNo = deliveryOrderNo;
			}
		}

		Method queryOldUser = SpelTest.class.getDeclaredMethod("queryOldUser", String.class);
		Method deveryUser = SpelTest.class.getDeclaredMethod("deveryUser", String.class);
		Request request = new Request("王二麻子", "15", "900");

		StandardEvaluationContext context = new StandardEvaluationContext();
		context.registerFunction("queryOldUser", queryOldUser);
		context.registerFunction("deveryUser", deveryUser);
		context.setVariable("request", request);

		String value = spelExpressionParser.parseExpression(content).getValue(context, String.class);
		System.out.println(value);
	}

	public static String queryOldUser(String deliveryOrderNo) {
		return "张三" + deliveryOrderNo;
	}

	public static String deveryUser(String userId) {
		return "王五" + userId;
	}

	@Test
	public void testFunctionExpression() throws SecurityException, NoSuchMethodException {
		//定义2个函数,registerFunction和setVariable都可以，不过从语义上面来看用registerFunction更恰当
		StandardEvaluationContext context = new StandardEvaluationContext();
		Method parseInt = Integer.class.getDeclaredMethod("parseInt", String.class);
		context.registerFunction("parseInt1", parseInt);
		context.setVariable("parseInt2", parseInt);

		ExpressionParser parser = new SpelExpressionParser();
		System.out.println(parser.parseExpression("#parseInt1('3')").getValue(context, int.class));
		System.out.println(parser.parseExpression("#parseInt2('3')").getValue(context, int.class));

		String expression1 = "#parseInt1('3') == #parseInt2('3')";
		boolean result1 = Boolean.TRUE.equals(parser.parseExpression(expression1).getValue(context, boolean.class));
		System.out.println(result1);
	}

	@Test
	public void testTemplateExpression() throws SecurityException, NoSuchMethodException {
		String template = "#{parseInt('2')}";
		//定义2个函数,registerFunction和setVariable都可以，不过从语义上面来看用registerFunction更恰当
		StandardEvaluationContext context = new StandardEvaluationContext();
		Method parseInt = Integer.class.getDeclaredMethod("parseInt", String.class);
		context.setVariable("parseInt", parseInt);

		ExpressionParser parser = new SpelExpressionParser();
		System.out.println(parser.parseExpression(template, new TemplateParserContext("#{","}")).getValue(context));
	}

}
