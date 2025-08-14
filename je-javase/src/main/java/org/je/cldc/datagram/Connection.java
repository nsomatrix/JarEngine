package org.je.cldc.datagram;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.UDPDatagramConnection;

import org.je.microedition.io.ConnectionImplementation;
import org.je.util.NetEventBus;
import org.je.util.net.NetConfig;

/**
 * {@link ConnectionImplementation} for the datagram protocol (UDP).
 */
public class Connection implements DatagramConnection, UDPDatagramConnection, ConnectionImplementation {

	/**
	 * The datagram protocol constant
	 */
	public final static String PROTOCOL = "datagram://";

	/**
	 * The encapsulated {@link DatagramSocket}
	 */
	private DatagramSocket socket;

	/**
	 * The connection address in the format <tt>host:port</tt>
	 */
	private String address;

	public void close() throws IOException {
		socket.close();
	}

	public int getMaximumLength() throws IOException {
		return Math.min(socket.getReceiveBufferSize(), socket.getSendBufferSize());
	}

	public int getNominalLength() throws IOException {
		return getMaximumLength();
	}

	public void send(Datagram dgram) throws IOException {
		if (NetConfig.Policy.offline) throw new IOException("No network");
		DatagramImpl di = (DatagramImpl) dgram;
		int len = di.getDatagramPacket().getLength();
		if (!NetConfig.Traffic.udpPermitAndDelay(len, null)) {
			try { NetEventBus.publish("UDP", "OUT", address, "drop:"+len); } catch (Throwable ignore) {}
			return;
		}
		socket.send(di.getDatagramPacket());
		try { NetEventBus.publish("UDP", "OUT", address, "send:"+len); } catch (Throwable ignore) {}
	}

	public void receive(Datagram dgram) throws IOException {
		DatagramImpl di = (DatagramImpl) dgram;
		socket.receive(di.getDatagramPacket());
		int len = di.getDatagramPacket().getLength();
		NetConfig.Traffic.udpPermitAndDelay(len, null);
		try { NetEventBus.publish("UDP", "IN", address, "recv:"+len); } catch (Throwable ignore) {}
	}

	public Datagram newDatagram(int size) throws IOException {
		return newDatagram(size, address);
	}

	public Datagram newDatagram(int size, String addr) throws IOException {
		if (!addr.startsWith(PROTOCOL)) {
			throw new IllegalArgumentException("Invalid Protocol " + addr);
		}
		Datagram datagram = new DatagramImpl(size);
		datagram.setAddress(addr);
		return datagram;
	}

	public Datagram newDatagram(byte[] buf, int size) throws IOException {
		return newDatagram(buf, size, address);
	}

	public Datagram newDatagram(byte[] buf, int size, String addr) throws IOException {
		if (!addr.startsWith(PROTOCOL)) {
			throw new IllegalArgumentException("Invalid Protocol " + addr);
		}
		Datagram datagram = new DatagramImpl(buf, size);
		datagram.setAddress(addr);
		return datagram;
	}

	public String getLocalAddress() throws IOException {
		InetAddress address = socket.getInetAddress();
		if (address == null) {
			/*
			 * server mode we get the localhost from InetAddress otherwise we
			 * get '0.0.0.0'
			 */
			address = InetAddress.getLocalHost();
		} else {
			/*
			 * client mode we can get the localhost from the socket here
			 */
			address = socket.getLocalAddress();
		}
		return address.getHostAddress();
	}

	public int getLocalPort() throws IOException {
		return socket.getLocalPort();
	}

	public javax.microedition.io.Connection openConnection(String name, int mode, boolean timeouts) throws IOException {
		if (!org.je.cldc.http.Connection.isAllowNetworkConnection() || NetConfig.Policy.offline) {
			throw new IOException("No network");
		}
		if (!name.startsWith(PROTOCOL)) {
			throw new IOException("Invalid Protocol " + name);
		}
		// TODO currently we ignore the mode
		address = name.substring(PROTOCOL.length());
		int port = -1;
		int index = address.indexOf(':');
		if (index == -1) {
			throw new IllegalArgumentException("Port missing");
		}
		String portToParse = address.substring(index + 1);
		if (portToParse.length() > 0) {
			port = Integer.parseInt(portToParse);
		}
	if (index == 0) {
			// server mode
			if (port == -1) {
				socket = new DatagramSocket();
			} else {
				socket = new DatagramSocket(port);
			}
		} else {
			// client mode
			if (port == -1) {
				throw new IllegalArgumentException("Port missing");
			}
			String host = address.substring(0, index);
			InetAddress ia;
			int targetPort = port;
			if (NetConfig.Policy.captivePortal) {
				ia = InetAddress.getByName("127.0.0.1");
				targetPort = NetConfig.Policy.captivePort;
			} else {
				ia = NetConfig.Dns.resolveHost(host);
			}
			socket = new DatagramSocket();
			socket.connect(new InetSocketAddress(ia, targetPort));
		}
	try { NetEventBus.publish("UDP", "OUT", address, "open"); } catch (Throwable ignore) {}
		return this;
	}
}
