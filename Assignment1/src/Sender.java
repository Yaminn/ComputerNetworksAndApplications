import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Random;

public class Sender {
	private static int receiver_port;
	private static float pdrop;
	private static int seed;
	private static int currAckNum;
	private static int currSeqNum;
	private static InetAddress receiver_host;
	private static boolean connected = false;
	private static DatagramSocket s;
	private static Log sender_log;
	private static long start_time;
	private static Random rand;
	private static int packetsDropped;
	private static int numDataSegSent;
	private static int retransmittedPackets;
	private static int fileSize;
	private static int duplicateACKReceived;
	private static int dataTransferred;

	public static void main(String[] args) throws Exception {
		if(args.length != 8){
			System.out.println("Not enough arguments supplied; 8 arguments expected (receiver_host_ip receiver_port file.txt MWS MSS timeout pdrop seed)");
			return;
		}
		
		//Assign arguments to variables
		receiver_port = Integer.parseInt(args[1]);
		receiver_host = InetAddress.getByName(args[0]); 
		
		int mws = Integer.parseInt(args[3]);
		int mss = Integer.parseInt(args[4]);
		int timeout = Integer.parseInt(args[5]);
		pdrop = Float.parseFloat(args[6]);
		seed = Integer.parseInt(args[7]);
		String fileName = args[2];
		rand = new Random(seed);
		sender_log = new Log("Sender_log.txt");

		try {
			s = new DatagramSocket();
			start_time = System.currentTimeMillis();
			s.setSoTimeout(timeout);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		try{
			//initiate handshake
			handshake();
			//Now we send the file!
			stp(fileName, mws, mss);
			//Teardown
			teardown();
			//Transfer complete, print out stats
			sender_log.addToLog("Amount of (original) Data Transferred: " + dataTransferred);
			sender_log.addToLog("Number of data segments sent (excluding retransmissions): " + numDataSegSent);
			sender_log.addToLog("Number of (all) Packets Dropped (by PLD module): " + packetsDropped);
			sender_log.addToLog("Number of Retransmitted Segments: " + retransmittedPackets);
			sender_log.addToLog("Number of Duplicate Acknowledgements Received: " + duplicateACKReceived);
		} finally {
			s.close();
		}
	}
	
	public static void handshake() throws Exception {
		//Initiate Handshake
		Random rand = new Random();
		int seq = Math.abs(rand.nextInt(1000));
		currSeqNum = seq;
		currAckNum = 0;
		Packet p = new Packet(seq);
		p.setSYN(true);
		p.setACKNum(0);
		connected = false;
		while(!connected){
			//Create datagram packet to send to socket
			DatagramPacket packet = prepareForSend(p, receiver_host, receiver_port);
			//Send SYN
			s.send(packet);
			sendToLog("snd", p);
			//Wait for SYNACK response
			DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
			s.receive(response);
			//Wait for SYNACK response
			Packet res = convertToPacket(response);
			sendToLog("rcv", res);
			//Read in the response and check it's SYNACK
			if(res.isACK() && res.isSYN()){
				//Send ACK(with no payload data)
				p.setACK(true);
				p.setSYN(false);
				p.setSeq(p.getSeqNum() + 1);
				p.setACKNum(res.getSeqNum() + 1);
				currSeqNum = p.getSeqNum();
				currAckNum = p.getACKNum();
				sendToLog("snd", p);
				s.send(prepareForSend(p, receiver_host, receiver_port));
				connected = true;
			}
			
		}
	}

	public static void stp(String fileName, int mws, int mss) throws Exception{
		LinkedList<Packet> window = new LinkedList<>();
		int numPackets = (int)Math.ceil(mws/mss);
		//Retrieve the file
		File file = new File(fileName);
		FileInputStream in = null;
		
		boolean transferComplete = false;
		fileSize = (int) file.length();
		int offset = 0;
		int dataSegSize = 0;
		int base = currSeqNum;
		int isn = currSeqNum;
		dataTransferred = 0;
		
			while(window.size() < numPackets && offset != fileSize){
				dataSegSize = 0;
				while(dataSegSize < mss && dataSegSize + offset < fileSize){
					dataSegSize += 1;
				}
				byte[] data = new byte[dataSegSize];
				try{
					in = new FileInputStream(file);
					in.skip(offset);
					int read = in.read(data, 0, dataSegSize);
					in.close();
					
					Packet snd = new Packet(currSeqNum);
					snd.setData(data);
					snd.setACKNum(currAckNum);
					window.add(snd);
					currAckNum = snd.getACKNum();
					currSeqNum += snd.getDataSize();
					offset += read;
						numDataSegSent++;
						
				} catch(FileNotFoundException e){
					System.out.println(file + " Not Found");
					return;
				}
			}
			
			
			for(Packet p : window){
				pld(p);
			}
			int duplicateAcks = 0;
			int lastAckNum = base;
			while(!transferComplete){
				if((base - isn) == fileSize){
					dataTransferred = base - isn;
					transferComplete = true;
					break;
				}
				if(duplicateAcks == 3){
					pld(window.getFirst());
				}
				try{
					DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
					s.receive(reply);
					Packet r = convertToPacket(reply);
					sendToLog("rcv", r);
					if(r.getACKNum() != lastAckNum){
						duplicateAcks = 1;
						lastAckNum = r.getACKNum();
						base = r.getACKNum();
						LinkedList<Packet> remove = new LinkedList<>();
						for(Packet q : window){
							if(q.getSeqNum() < base) remove.add(q);
						}
						window.removeAll(remove);
						if(currSeqNum - base <= mws){
							dataSegSize = 0;
							while(dataSegSize < mss && dataSegSize + offset < fileSize){
								dataSegSize += 1;
							}
							
							byte[] data = new byte[dataSegSize];
							in = new FileInputStream(file);
							in.skip(offset);
							int read = in.read(data, 0, dataSegSize);
								in.close();
							if(read != 0){
								Packet snd = new Packet(currSeqNum);
								snd.setData(data);
								snd.setACKNum(currAckNum);
								pld(snd);
								window.add(snd);
								currAckNum = snd.getACKNum();
								currSeqNum += snd.getDataSize();
								offset += read;
								numDataSegSent++;
							}
						}
					} else {
						duplicateAcks++;
						duplicateACKReceived++;
					}
					 
					
				} catch(IOException e){
					pld(window.getFirst());
					duplicateAcks = 0;
					retransmittedPackets++;
				}
					
			}
	}
	
	public static void teardown() throws Exception{
		//Transfer complete, send FIN
		Packet fin = new Packet(currSeqNum);
		fin.setFIN(true);
		fin.setACKNum(currAckNum);
		s.send(prepareForSend(fin, receiver_host, receiver_port));
		sendToLog("snd", fin);
		try {
			//Receive FINACK and send ACK
			DatagramPacket fa = new DatagramPacket(new byte[1024], 1024);
			s.receive(fa);
			Packet r = convertToPacket(fa);
			if(r.isACK() && r.isFIN()){
				sendToLog("rcv", r);
				Packet a = new Packet(r.getACKNum());
				a.setACKNum(r.getSeqNum() + 1);
				a.setACK(true);
				sendToLog("snd", a);
				s.send(prepareForSend(a, receiver_host, receiver_port));
			}
		} catch (Exception e){}
	}
	
	private static void pld(Packet p) throws Exception{
		float random = rand.nextFloat();
		if(random > pdrop){
			sendToLog("snd", p);
			s.send(prepareForSend(p, receiver_host, receiver_port));
		} else {
			sendToLog("drop", p);
			packetsDropped++;
		}
	}
	
	
	private static void sendToLog(String action, Packet p) throws Exception{
		String print = action + "\t" + (float)(System.currentTimeMillis() - start_time) + "\t";
		//Determine type of Packet
		if(p.isSYN()) print += "S";
		if(p.isFIN()) print += "F";
		if(p.isACK()) print += "A";
		if(p.getDataSize() != 0) print += "D";
		print += "\t" + p.getSeqNum() + "\t" + p.getDataSize() + "\t" + p.getACKNum();
		
		sender_log.addToLog(print);
		
	}
	
	private static DatagramPacket prepareForSend(Packet packet, InetAddress host,int port) throws Exception {
		//Create datagram packet to send to socket
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bos);
		out.writeObject(packet);
		out.close();
		DatagramPacket res = new DatagramPacket(bos.toByteArray(), bos.size(), host, port);
		return res;
	}
	
	private static Packet convertToPacket(DatagramPacket dp) throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(dp.getData());
		ObjectInputStream in = new ObjectInputStream(bis);
		in.close();
		return (Packet) in.readObject();
	}
}

