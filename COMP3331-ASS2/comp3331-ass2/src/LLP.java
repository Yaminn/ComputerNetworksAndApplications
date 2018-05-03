import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class LLP implements RoutingProtocol {

	@Override
	public ArrayList<Node> Dijkstra(Request r, ArrayList<Node> graph) {
		ArrayList<Node> visited = new ArrayList<Node>();
		ArrayList<Node> unvisited = new ArrayList<Node>(); // to visit next
		HashMap<Node, BigDecimal> loadTracker = new HashMap<>();
		HashMap<Node, Node> pre = new HashMap<Node, Node>();
		
		Node start = r.getSource();
		Node end = r.getDest();
		
		for (Node n: graph){
			loadTracker.put(n, new BigDecimal(-1));
		}
		
		loadTracker.put(start, new BigDecimal(0));
		unvisited.add(start);

		while (!(unvisited.isEmpty())){
			Node node = getMinValue(unvisited, loadTracker);
			visited.add(node);
			unvisited.remove(node);
			
			ArrayList<Node> neighbours = new ArrayList<Node>();
			for (Node n: node.getNeighbours()){
				if (!(visited.contains(n))){
					neighbours.add(n);
				}
			}
			
			for (Node n: neighbours) { 
				if(node.getLink(n) != null){
					if (loadTracker.get(n).compareTo(node.getLink(n).getRatio()) == -1 || loadTracker.get(n).equals(new BigDecimal(-1))){
						loadTracker.put(n, node.getLink(n).getRatio());
						pre.put(n, node);
						if (!(visited.contains(n))){
							unvisited.add(n);
						}
					}
				}
			}
			
		}
		return getPath(end, pre);
	}

	@Override
	public Node getMin(ArrayList<Node> unvisited, HashMap<Node, Float> loadTracker) {
		return null;
	}
	
	public Node getMinValue(ArrayList<Node> unvisited, HashMap<Node, BigDecimal> loadTracker) {
		BigDecimal lo = new BigDecimal(0);
		Node lowest = null;
		for (Node n: unvisited){
			if (lo.equals(new BigDecimal(0)) || loadTracker.get(n).compareTo(lo) == -1){
				lowest = n;
				lo = loadTracker.get(n);
			} 
		}
		return lowest;
	}
	

	@Override
	public ArrayList<Node> getPath(Node end, HashMap<Node, Node> pre) {
		ArrayList<Node> path = new ArrayList<Node>();
		Node node = end;
		
		if (pre.get(node) == null) return null;
		
		path.add(node);
		
		Node next = null;
		Node check = null;
		int flag = 0;
		while (pre.get(node) != null){
			check = pre.get(node);
			if (node.getLink(check).isFull()){
				flag = 1;
				break;
			}
			node = pre.get(node);
			next = node;
			path.add(next);
		}
		
		Collections.reverse(path);	
		if (flag == 1) {
			path = null;
		}
		return path;
	}

}
