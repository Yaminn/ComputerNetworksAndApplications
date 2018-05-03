import java.util.ArrayList;

public class Node {
	private String name;
	private ArrayList<Link> links;
	public Node(String n) {
		name = n;
		links = new ArrayList<>();
	}
	
	public String getName() {
		return name;
	}
	
	public void createLink(Node dest, int dprop, float capacity){
		Link link = new Link(dest, dprop, capacity);
		links.add(link);
	}
	
	public ArrayList<Node> getNeighbours(){
		ArrayList<Node> results = new ArrayList<>();
		for (Link l: this.links){
			Node dest = l.getDest();
			results.add(dest);
		}
		return results;
	}
	
	public ArrayList<Link> getLinks(){
		return this.links;
	}
	
	public Link getLink(Node to){
		for(Link l : links){
			if(l.getDest().equals(to)) return l;
		}
		return null;
	}
	
	

}
