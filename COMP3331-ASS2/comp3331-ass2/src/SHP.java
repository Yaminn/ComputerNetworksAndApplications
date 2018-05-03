import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class SHP implements RoutingProtocol{

	@Override
	public ArrayList<Node> Dijkstra(Request r, ArrayList<Node> graph) {
		ArrayList<Node> visited = new ArrayList<Node>();	
		ArrayList<Node> unvisited = new ArrayList<Node>(); // to visit next
		HashMap<Node, Float> costTracker = new HashMap<Node, Float>();
		HashMap<Node, Node> pre = new HashMap<Node, Node>();
		
		Node start = r.getSource();
		Node end = r.getDest();
		
		for (Node n: graph){
			costTracker.put(n, -1f);
		}
		
		costTracker.put(start, 0f);
		unvisited.add(start);

		while (!(unvisited.isEmpty())){

			Node node = getMin(unvisited, costTracker);
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
					if (costTracker.get(n) > costTracker.get(node) + 1 || costTracker.get(n) == -1){
						float result = costTracker.get(node) + 1;
						costTracker.put(n, result);
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
	public Node getMin(ArrayList<Node> unvisited, HashMap<Node, Float> costTracker) {
		float lo = -1.00f;
		Node lowest = null;
		for (Node n: unvisited){
			if (lo == -1 || costTracker.get(n) < lo){
				lowest = n;
				lo = costTracker.get(n);
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
