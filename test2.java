
public class test2 {

	public static void main(String[] args) {
		
		ClientUDP client2=new ClientUDP("BBB","AAA",9602);
		new Thread(client2).start();
		
		client2.connectClient();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		client2.connectClient();
	}

} 
