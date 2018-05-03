import java.math.BigDecimal;
import java.util.ArrayList;

public class Request {
	private Node src;
	private Node dest;
	private BigDecimal start;
	private BigDecimal duration;
	private String action;
	private ArrayList<Node> path;
	
	public Request(Node src, Node dest, BigDecimal start, BigDecimal duration, String a, ArrayList<Node> path) {
		this.src = src;
		this.dest = dest;
		this.start = start;
		this.duration = duration;
		this.action = a;
		this.path = path;
	}
	
	public Node getSource(){
		return this.src;
	}
	
	public Node getDest(){
		return this.dest;
	}
	
	public BigDecimal getStart(){
		return this.start;
	}
	
	public BigDecimal getDuration(){
		return this.duration;
	}
	
	public String getAction(){
		return this.action;
	}
	
	public ArrayList<Node> getPath(){
		return this.path;
	}
	

}
