package com.clarisma.common.bytecode;
// TODO: rename package to "code"?

import static org.objectweb.asm.Opcodes.*;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class Coder
{
	protected String className;
	protected String baseClassName;
	protected ClassWriter cw; 
	protected MethodVisitor mv;
	protected MethodVisitor staticMv;
	protected int uniqueFieldCounter;
	
	protected void beginClass(String className, String baseClassName, String[] interfaces)
	{
		this.className = className;
		this.baseClassName = baseClassName;
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cw.visit(V1_5, ACC_PUBLIC, className, null, baseClassName, interfaces);
	}
	
	protected void endClass()
	{
		if(staticMv != null)
		{
			staticMv.visitInsn(RETURN);
			// force auto-calculation of maxStack and maxLocals
			staticMv.visitMaxs(0, 0);
			staticMv.visitEnd();
			staticMv = null;
		}
	}

	/**
	 * Starts the code for a public constructor and initializes the MethodVisitor.
	 *
	 * @param descriptor	the parameter signature of the constructor
	 *                      (e.g. "(Ljava/lang/String;I)V" if the constructor
	 *                      takes a String and an int)
	 */
	protected void beginConstructor(String descriptor)
	{
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
	}

	/**
	 * Invokes the base class constructor. The MethodVisitor must be initialized
	 * for the constructor of the class being generated.
	 *
	 * @param descriptor the parameter signature of the base-class constructor
	 */
	protected void callBaseClassConstructor(String descriptor)
	{
		mv.visitMethodInsn(INVOKESPECIAL, baseClassName, "<init>", descriptor, false);
	}

	
	/**
	 * Writes an instruction to push an integer constant onto
	 * the operand stack. Depending on the magnitude of the 
	 * value, writes ICONST_n, BIPUSH, SIPUSH or LDC.
	 * 
	  * @param value	the integer constant
	 */
	public void loadIntConstant(int value)
	{
		if((byte)value == value)
		{
			if(value >= -1 && value <= 5)
			{
				mv.visitInsn(ICONST_0 + value);
				return;
			}
			mv.visitIntInsn(BIPUSH, value);
			return;
		}
		if((short)value == value)
		{
			mv.visitIntInsn(SIPUSH, value);
			return;
		}
		mv.visitLdcInsn(value);
	}

	public String uniqueFieldName(String prefix)
	{
		uniqueFieldCounter++;
		return prefix + uniqueFieldCounter;
	}
	
	public MethodVisitor staticInitializer()
	{
		if(staticMv == null)
		{
			staticMv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
			staticMv.visitCode();
		}
		return staticMv;
	}
	
	protected MethodVisitor useMethodWriter(MethodVisitor newMv)
	{
		MethodVisitor oldMv = mv;
		mv = newMv;
		return oldMv;
	}
}
