
public class test {

	public static void main(String[] args) {
		
			ClientUDP client=new ClientUDP("AAA","BBB",9600);
			new Thread(client).start();

	}

}
