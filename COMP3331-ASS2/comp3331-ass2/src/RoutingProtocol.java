import java.util.ArrayList;
import java.util.HashMap;

public interface RoutingProtocol {
	public ArrayList<Node> Dijkstra(Request r, ArrayList<Node> graph);
	public Node getMin(ArrayList<Node> unvisited, HashMap<Node, Float> totalHops);
	public ArrayList<Node> getPath(Node end, HashMap<Node, Node> pre);
}
