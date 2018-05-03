import java.io.Serializable;

public class Packet implements Serializable{

	private static final long serialVersionUID = 1L;
	private int seq;
	private int ack;
	private boolean SYN;
	private boolean FIN;
	private boolean ACK;
	private byte[] data;
	
	public Packet(int seqNum) {
		this.seq = seqNum;
		this.SYN = false;
		this.FIN = false;
		this.ACK = false;
	}
	
	public void setSYN(boolean res){
		this.SYN = res;
	}
	
	public boolean isSYN(){
		return SYN;
	}
	
	public void setSeq(int seq){
		this.seq = seq;
	}
	
	public boolean isACK() {
		return ACK;
	}
	
	public void setACK(boolean res) {
		this.ACK = res;
	}
	
	public void setACKNum(int ack){
		this.ack = ack;
	}
	
	public int getACKNum(){
		return this.ack;
	}
	
	public void setFIN(boolean res){
		this.FIN = res;
	}
	
	public void setData(byte[] data){
		this.data = data;
	}
	
	public byte[] getData(){
		return this.data;
	}
	
	public int getDataSize(){
		if(this.data == null){
			return 0;
		}
		return data.length;
	}

	public int getSeqNum() {
		return this.seq;
	}
	
	public boolean isFIN() {
		return this.FIN;
	}

}
