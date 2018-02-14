import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/*
*
*数据报格式 : type(byte) +(int)len +"hostname：ip:port";
*link "hostname:opname"
*/
public class ServerSwap implements Runnable {
	final byte Islive = 0;
	final byte IsLink = 1;
	final byte IsReady = 3;
	final byte IsWait = 4;
	private String ip = "172.16.169.2";
	private int port = 8070;
	private Selector sc;
	private DatagramChannel dc;
	long outtime = 10 * 1000;
	long selout = 0;
	byte bs[] = new byte[65550];
	ByteBuffer buf = ByteBuffer.wrap(bs);
	HashMap<Host, Long> livemap = new HashMap();
	List<String> down = new ArrayList<>();
	public ServerSwap() {
		try {
			sc = Selector.open();
			dc = DatagramChannel.open();
			dc.configureBlocking(false);
			dc.socket().bind(new InetSocketAddress(ip, port));
			dc.register(sc, SelectionKey.OP_READ);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		ServerSwap swap=new ServerSwap();
		new Thread(swap).start();
		
	}

	public void CheckLive() {
		Iterator<Map.Entry<Host, Long>> entrys = livemap.entrySet().iterator();
		while (entrys.hasNext()) {
			Entry<Host, Long> host = entrys.next();
			if (System.currentTimeMillis() - host.getValue() >= outtime) {
				livemap.remove(host.getKey());
				down.add(host.getKey().name);
			} else {
				if (down.contains(host.getKey().name))
					down.remove(host.getKey().name);
			}
		}
	}

	@Override
	public void run() {
		try {
			
			while (true) {
				System.out.println("server--run");
				sc.select();
				Set<SelectionKey> keys = sc.selectedKeys();
				for (SelectionKey key : keys) {
					if (key.isReadable()) {
						InetSocketAddress address = (InetSocketAddress) dc.receive(buf);
						buf.rewind();
						byte[] arr = buf.array();

						byte type = buf.get();

						int len = buf.getInt();

						switch (type) {
						case Islive: { // datap :type+len+hostname
							livemap.put(new Host(new String(bs, 5, len), address), System.currentTimeMillis());
							System.out.println(new String(bs, 5, len) + address + livemap.size());
						}
							break;

						case IsLink: {
							// 接受 type+len+"host:opname"
							LinkHost(buf, IsLink, len, address, dc);
							System.out.println(new String(bs, 5, len) +"link");
						}
							break;

						case IsWait: {
							// 接受 type+len+host+waitname
							LinkHost(buf, IsWait, len, address, dc);
							System.out.println(new String(buf.array(),5,len)+"wait");
							
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

	private void LinkHost(ByteBuffer buf, byte type, int len, InetSocketAddress address, DatagramChannel dc) {
		String[] name = new String(buf.array(), 5, len).split(":");

		Iterator<Map.Entry<Host, Long>> entrys = livemap.entrySet().iterator();
		while (entrys.hasNext()) {
			Entry<Host, Long> host = entrys.next();
			if (host.getKey().name.equals(name[1])) {
				ByteBuffer f = makeLinkbuf(address, type, name[0]);
				f.rewind();
				try {
					dc.send(f, host.getKey().address);
				} catch (Exception e) {

					e.printStackTrace();
				}
			}
		}
	}

	private ByteBuffer makeLinkbuf(InetSocketAddress address, byte type, String host) {
		String adr = host + ":" + address.toString().substring(1);
		byte[] adrr = adr.getBytes();
		ByteBuffer buf = ByteBuffer.allocate(50);
		buf.put(type);
		buf.putInt(adrr.length);
		buf.put(adrr);
		return buf;
	}

}

class Host {
	public Host(String name, InetSocketAddress address) {
		this.name = name;
		this.address = address;

	}

	String name;
	InetSocketAddress address;

	@Override
	public int hashCode() {

		return name.hashCode() + address.getAddress().hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		return this.hashCode() == obj.hashCode();
	}
}