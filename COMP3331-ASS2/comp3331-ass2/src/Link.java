import java.math.BigDecimal;

public class Link {
	private Node dest;
	private int dprop;
	private float capacity;  
	private float currLoad;

	public Link(Node d, int prop, float c) {
		dest = d;
		dprop = prop;
		capacity = c;
		currLoad = 0;
	}
	
	public Node getDest(){
		return dest;
	}
	
	public int getDprop(){
		return dprop;
	}

	public float getCapacity(){
		return capacity;
	}

	public float getCurrLoad(){
		return currLoad;
	}
	
	public void removeLoad(){
		this.currLoad = this.currLoad - 1;
	}
	
	public void addLoad(){
		this.currLoad = this.currLoad + 1;
	}

	public boolean isFull(){
		if(currLoad >= capacity){
			return true;
		}
		return false;
	}
	
	public BigDecimal getRatio(){
		return new BigDecimal(this.currLoad/this.capacity).setScale(2, BigDecimal.ROUND_HALF_UP);
	}
}
