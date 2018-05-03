import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Random;

public class Receiver {
	private static long start_time;
	private static LinkedList<Packet> buffer;
	private static LinkedList<Packet> received;
	private static Log Receiver_log;
	private static File out_file;
	private static int dataReceived;
	private static int duplicateSegments;
	
	public static void main(String[] args) throws Exception {
		if(args.length != 2){
			System.out.println("Not enough arguments supplied; 2 arguments expected (receiver_port file.txt)");
	        return;
		}
		
		int port = Integer.parseInt(args[0]);
		String file = args[1];
		Receiver_log = new Log("Receiver_log.txt");
		out_file = new File(file);
		DatagramSocket s = new DatagramSocket(port);
		start_time = System.currentTimeMillis();
		buffer = new LinkedList<Packet>();
		received = new LinkedList<>();
		int expectedSeqNum = 0;
		
		while(true){
			//Receive packet from Sender
			DatagramPacket rec = new DatagramPacket(new byte[1024], 1024);
			s.receive(rec);
			Packet p = convertToPacket(rec);
			//if it's a SYN
			if(p.isSYN()){
				//Send SYNACK
				Random rand = new Random();
				int seq = Math.abs(rand.nextInt(1000));
				Packet synack = new Packet(seq);
				synack.setACK(true);
				synack.setSYN(true);
				synack.setACKNum(p.getSeqNum() + 1);
				
				rec = prepareForSend(synack,rec.getAddress(), rec.getPort());
				s.send(rec);
				sendToLog("snd", synack);
				
				try {
					DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
					s.receive(reply);
					Packet r = convertToPacket(reply);
					sendToLog("rcv", r);
					
					if(r.isACK()){
						expectedSeqNum = r.getSeqNum();
						//Handshake complete.
						//prepare to receive file.
						out_file.createNewFile();
					}
				} finally {
					
				}
			} else if(p.isFIN()){
				sendToLog("rcv", p);
				Packet fa = new Packet(p.getACKNum());
				fa.setACK(true);
				fa.setFIN(true);
				fa.setACKNum(p.getSeqNum() + 1);
				s.send(prepareForSend(fa, rec.getAddress(), rec.getPort()));
				sendToLog("snd", fa);
				
				try {
					DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
					s.receive(reply);
					Packet r = convertToPacket(reply);
					if(r.isACK()){
						sendToLog("rcv", r);
						for(Packet p1 : received){
							//Reliable transfer done - write to file
							writeFile(new String(p1.getData()));
						}
						break;
					}
				} catch (IOException e) { 
					e.printStackTrace();
				}
			
			} else if (p.getDataSize() != 0) {
				//get data out of packet
				sendToLog("rcv", p);
				Packet ack = new Packet(p.getACKNum());
				if(!received.isEmpty() && received.contains(p)){
					duplicateSegments++;
				} 
				
				if(p.getSeqNum()<=expectedSeqNum){
					if(received.isEmpty() || (received.getLast().getSeqNum() < p.getSeqNum())){
					//Check buffer for packets
						dataReceived += p.getDataSize();
						expectedSeqNum = p.getSeqNum() + p.getDataSize();
						received.add(p);
						LinkedList<Packet> remove = new LinkedList<>();
						if(!buffer.isEmpty()){
							for(int i=0; i < buffer.size(); i++){
								Packet q = buffer.get(i);
								if(q.getSeqNum() == expectedSeqNum){
									//Write to file
									received.add(q);
									//Update expectedSeqNum
									expectedSeqNum = (q.getSeqNum() + q.getDataSize());
									remove.add(q);
								}
							}
						} 
						buffer.removeAll(remove);
						ack.setACKNum(expectedSeqNum);
					}
				} else {
					buffer.add(p);
					dataReceived += p.getDataSize();
					ack.setACKNum(expectedSeqNum);
				}
				ack.setACK(true);
				s.send(prepareForSend(ack, rec.getAddress(), rec.getPort()));
				sendToLog("snd", ack);
				
			}
		}
		//close connection
		s.close();
		Receiver_log.addToLog("Amount of (original) Data Received (in bytes): " + dataReceived);
		Receiver_log.addToLog("Number of (original) Data Segments Received: " + received.size());
		Receiver_log.addToLog("Number of Duplicate Segments Received: " + duplicateSegments);
	}
	
	private static void writeFile(String line) throws IOException{
		String msg = line;
		if(!out_file.exists()){
			out_file.createNewFile();
		} 
		FileWriter fw = new FileWriter(out_file.getAbsoluteFile(), true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(msg);
		bw.close();
		fw.close();
	}
	
	private static void sendToLog(String action, Packet p) throws Exception{
		String print = action + "\t" + (float)(System.currentTimeMillis() - start_time) + "\t";
		//Determine type of Packet
		if(p.isSYN()) print += "S";
		if(p.isFIN()) print += "F";
		if(p.isACK()) print += "A";
		if(p.getDataSize() != 0) print += "D";
		print += "\t" + p.getSeqNum() + "\t" + p.getDataSize() + "\t" + p.getACKNum();
		//Send to log
		Receiver_log.addToLog(print);
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
