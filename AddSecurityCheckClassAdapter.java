package com.test;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;



public class AddSecurityCheckClassAdapter extends ClassAdapter{
	public AddSecurityCheckClassAdapter(ClassVisitor cv) {
		super(cv);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv = cv.visitMethod(access, name, desc, signature,exceptions);

		MethodVisitor wrappedMv = mv;
		if (mv != null) {
			//���� "operation" ����
			if (name.equals("operation")) { 
				//ʹ���Զ��� MethodVisitor��ʵ�ʸ�д��������
				wrappedMv = new AddSecurityCheckMethodAdapter(mv); 
			} 
		}
		return wrappedMv;
	}


}
