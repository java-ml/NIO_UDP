import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

public class ClientUDP implements Runnable {
	
	String hostname = "";
	String host="";
	final byte Islive = 0;
	final byte IsLink = 1;
	final byte IsReady = 3;
	final byte IsWait = 4;
	final byte ISWrite=5;
	final byte ISFirst=6;
	byte[] array = new byte[1024];
	ByteBuffer buf = ByteBuffer.wrap(array);
	private  String ip = "172.16.169.2";
	private  int port = 8070;
	private Selector selector;
	private  DatagramChannel channel;
	private  InetSocketAddress address = new InetSocketAddress(ip, port);
	public ClientUDP(String hostname,String host,int port) {
		//System.setProperty("java.net.preferIPv4Stack", "true");
		this.hostname=hostname;
		this.host=host;
		try {
			selector = Selector.open();
			channel = DatagramChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress("172.16.169.2", port));
			channel.register(selector, SelectionKey.OP_READ);
			OnLive(channel, address);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
	}
	
	private void OnLive(DatagramChannel channel, InetSocketAddress address) {
		ByteBuffer buff = ByteBuffer.allocate(100);
		buff.put(Islive);
		buff.putInt(hostname.getBytes().length);
		buff.put(hostname.getBytes());
		buff.rewind();
		try {
			channel.connect(address);
			channel.write(buff);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		try {
		
			
			while (true) {
				selector.select();
				Set<SelectionKey> keys = selector.selectedKeys();
				System.out.println("runing---"+hostname);
				for (SelectionKey key : keys) {
					System.out.println("hive--->"+hostname);
					if (key.isReadable()) {
						System.out.println("hivedown--->"+hostname);
						
							InetSocketAddress addres = (InetSocketAddress) channel.receive(buf);
							buf.rewind();
							byte[] arr = buf.array();

							byte type = buf.get();

							int len = buf.getInt();

							switch (type) {

							case IsLink: {
								Link(IsWait,buf,len,channel);
								
							}
								break;
							case ISWrite: {
								System.out.println("wwww---"+new String(buf.array(),5,len));
							}
								break;
							case ISFirst:
							{
								
								System.out.println("ffff"+new String(buf.array(),5,len));
								String info="this is from"+hostname;
								ByteBuffer buffer=ByteBuffer.allocate(1024);
								buffer.put(ISWrite);
								buffer.putInt(info.getBytes().length);
								buffer.put(info.getBytes());
								buffer.rewind();
								channel.disconnect();
								channel.connect(addres);
								System.out.println(addres+"sending"+"<---"+channel.getLocalAddress()+hostname);
								channel.send(buffer,addres);
								//channel.disconnect();
								
								//OnLive(channel, address);
								System.out.println("sent");
								buffer.clear();
							}
								break;

							case IsWait: {
								Thread.sleep(500);
								channel.disconnect();
								String info="this is from"+hostname;
								String adr[] =new String(buf.array(), 5, len).split(":");
								ByteBuffer buffer=ByteBuffer.allocate(1024);
								buffer.put(ISWrite);
								buffer.putInt(info.getBytes().length);
								buffer.put(info.getBytes());
								buffer.rewind();
								InetSocketAddress address2=new InetSocketAddress(adr[1], Integer.valueOf(adr[2]));
								channel.connect(address2);
								System.out.println(address2+"sending"+"<---"+channel.getLocalAddress()+hostname);
								channel.send(buffer,address2);
								System.out.println("sent");
								buffer.clear();
								channel.disconnect();	
							}
								break;
							}
							buf.clear();
						
					}

				}
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
	}
	public void connectClient(){
		if(!channel.isConnected())
			try {
				channel.connect(address);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		String link=hostname+":"+host;
		ByteBuffer buffer=ByteBuffer.allocate(1024);
		buffer.put(IsLink);
		buffer.putInt(link.getBytes().length);
		buffer.put(link.getBytes());
		buffer.rewind();
		try {
			channel.write(buffer);
			channel.disconnect();
			System.out.println("conet");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void Link(byte isWait, ByteBuffer buf,int len,  DatagramChannel channel) {
		String adr[] =new String(buf.array(), 5, len).split(":");
		ByteBuffer buffer=ByteBuffer.allocate(1024);
		try {
			
			if(!channel.isConnected())OnLive(channel, address);
			buffer.put(IsWait);
			String adrr=hostname+":"+adr[0];
			System.out.println(adrr+"--wait");
			buffer.putInt(adrr.getBytes().length);
			buffer.put(adrr.getBytes());
			buffer.rewind();
			channel.write(buffer);
			buffer.clear();
			buffer.rewind();
			channel.disconnect();
			buffer.put(ISFirst);
			buffer.putInt(hostname.getBytes().length);
			buffer.put(hostname.getBytes());
			buffer.rewind();
			System.out.println("A->"+adr[0]+adr[1]+adr[2]);
			InetSocketAddress addres=new InetSocketAddress(adr[1], Integer.valueOf(adr[2]));
			channel.connect(addres);
			channel.send(buffer,addres);
			channel.disconnect();
						
		}  catch (Exception e) {
			
			e.printStackTrace();
		}
		
	}
	
}
