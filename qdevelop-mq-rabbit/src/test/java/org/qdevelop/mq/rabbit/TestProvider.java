package org.qdevelop.mq.rabbit;

public class TestProvider {

	public static void main(String[] args) {
		for(int i=0;i<10000;i++){
			Object[] v = new Object[]{"test",i};
			 MQProvider.getInstance().publish("Janson", v);
			 System.out.print(".");
//			 try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
	}

}
