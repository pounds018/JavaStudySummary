package cn.pounds.elegant.log.config;

import cn.pounds.elegant.log.parse.core.ExpressionRootObject;
import cn.pounds.elegant.log.parse.func.ParseFunctionFactory;
import org.objectweb.asm.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

import static org.objectweb.asm.Opcodes.*;


/**
 * @author: pounds
 * @date: 2022/2/27 18:56
 * @desc:
 */
@Configuration
public class RootObjectAsmConfig {

	private static final int API_VERSION = Opcodes.ASM9;

	@Resource
	private ParseFunctionFactory parseFunctionFactory;
	@Bean
	public ExpressionRootObject expressionRootObject(){
		try {
			String name = ExpressionRootObject.class.getName();
			ClassReader classReader = new ClassReader(name);
			//COMPUTE_MAXS表示asm是否需要计算出该类的最大局部变量表和操作数栈
			ClassWriter writerVisitor = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			// 定义适配器
			MethodAddVisitor methodAddVisitor = new MethodAddVisitor(writerVisitor);
			//新增加一个方法
			MethodVisitor mw= writerVisitor.visitMethod(ACC_PUBLIC + ACC_STATIC,
					"add",
					"([Ljava/lang/String;)V",
					null,
					null);
			// pushes the 'out' field (of type PrintStream) of the System class
			mw.visitFieldInsn(GETSTATIC,
					"java/lang/System",
					"out",
					"Ljava/io/PrintStream;");
			// pushes the "Hello World!" String constant
			mw.visitLdcInsn("this is add method print!");
			// invokes the 'println' method (defined in the PrintStream class)
			mw.visitMethodInsn(INVOKEVIRTUAL,
					"java/io/PrintStream",
					"println",
					"(Ljava/lang/String;)V");
			mw.visitInsn(RETURN);
			// this code uses a maximum of two stack elements and two local
			// variables
			mw.visitMaxs(0, 0);
			mw.visitEnd();

			// 读取器先通知适配器
			classReader.accept(methodAddVisitor, API_VERSION);
			System.out.println();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	class MethodAddVisitor extends ClassVisitor implements Opcodes {

		public MethodAddVisitor(final ClassVisitor cv) {
			super(API_VERSION, cv);
		}

		@Override
		public void visit(
				int version,
				int access,
				String name,
				String signature,
				String superName,
				String[] interfaces)
		{
			if (cv != null) {
				cv.visit(version, access, name, signature, superName, interfaces);
			}
		}

		@Override
		public MethodVisitor visitMethod(
				int access,
				String name,
				String desc,
				String signature,
				String[] exceptions)
		{
			if (cv != null && "execute".equals(name)) { //当方法名为execute时，修改方法名为execute1
				return cv.visitMethod(access, name + "1", desc, signature, exceptions);
			}

			if("changeMethodContent".equals(name))  //此处的changeMethodContent即为需要修改的方法  ，修改方法內容
			{
				MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);//先得到原始的方法
				MethodVisitor newMethod = null;
				newMethod = new AsmMethodVisit(mv); //访问需要修改的方法
				return newMethod;
			}
			if (cv != null) {
				return cv.visitMethod(access, name, desc, signature, exceptions);
			}

			return null;
		}
	}

	static  class AsmMethodVisit extends MethodVisitor {

		public AsmMethodVisit(MethodVisitor mv) {
			super(API_VERSION, mv);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			super.visitMethodInsn(opcode, owner, name, desc);
		}

		@Override
		public void visitCode() {
			//此方法在访问方法的头部时被访问到，仅被访问一次
			//此处可插入新的指令
			super.visitCode();
		}

		@Override
		public void visitInsn(int opcode) {
			//此方法可以获取方法中每一条指令的操作类型，被访问多次
			//如应在方法结尾处添加新指令，则应判断：
			if(opcode == Opcodes.RETURN)
			{
				// pushes the 'out' field (of type PrintStream) of the System class
				mv.visitFieldInsn(GETSTATIC,
						"java/lang/System",
						"out",
						"Ljava/io/PrintStream;");
				// pushes the "Hello World!" String constant
				mv.visitLdcInsn("this is a modify method!");
				// invokes the 'println' method (defined in the PrintStream class)
				mv.visitMethodInsn(INVOKEVIRTUAL,
						"java/io/PrintStream",
						"println",
						"(Ljava/lang/String;)V");
//                mv.visitInsn(RETURN);
			}
			super.visitInsn(opcode);
		}
	}


}
