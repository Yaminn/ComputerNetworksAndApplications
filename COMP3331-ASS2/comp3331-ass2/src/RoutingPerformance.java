import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Scanner;


public class RoutingPerformance {
	private static String networkScheme;
	private static String routingScheme;
	private static ArrayList<Node> graph;
	private static PriorityQueue<Request> requests;
	private static BigDecimal packetRate;
	
	public static void main(String[] args) {
		if(args.length != 5){
			System.out.println("Not enough arguments supplied; NETWORK_SCHEME, ROUTING_SCHEME, TOPOLOGY_FILE.txt, WORKLOAD_FILE.txt, PACKET_RATE");
		}
		//set network scheme - arg0
		networkScheme = args[0];
		//set routing scheme - arg1
		routingScheme = args[1];
		//create a graph i.e. list of nodes - arg2
		graph = new ArrayList<>();
		createGraph(args[2]);
		//create requests - arg3
		Comparator<Request> comparator = new RequestComparator();
		requests = new PriorityQueue<Request>(10, comparator);
		createRequests(args[3]);
		//set packet rate - arg4 
		packetRate = new BigDecimal(args[4]);
		
		float numSuccessfulPackets = 0;
		float numBlockedPackets = 0;
		ArrayList<Float> hopsInCircuit = new ArrayList<>();
		ArrayList<Float> dpropInCircuit = new ArrayList<>();
		float numRequests = requests.size();
		
		RoutingProtocol rp = null;
		if(routingScheme.equals("SDP")){
			rp = new SDP();
		} else if(routingScheme.equals("SHP")){
			rp = new SHP();
		} else {
			rp = new LLP();
		}
		
		if(networkScheme.equals("CIRCUIT")){
			//For each request, try and find a path
			while (requests.peek() != null){
				Request r = requests.poll();
				
				if(r.getAction().equals("open")){
					ArrayList<Node> pathFound = rp.Dijkstra(r, graph);
					//If there is no path (i.e. null or last node in path != destination of request) - it's blocked.
					if(pathFound == null || !pathFound.get(pathFound.size()-1).equals(r.getDest())){
						numBlockedPackets += calculateNumPackets(r);
					} else {
						hopsInCircuit.add(calculateNumHops(pathFound));
						numSuccessfulPackets += calculateNumPackets(r);
						dpropInCircuit.add(calculateCumulativePropDelay(pathFound));
						updateGraph(pathFound, "add");
						requests.add(new Request(null, null, r.getStart().add(r.getDuration()), null, "close", pathFound));	
					}
				} else {
						updateGraph(r.getPath(), "remove");
				}
			}
		} else {
			//networkscheme == PACKET
			PriorityQueue<Request> packets = new PriorityQueue<Request>(10, comparator);
			while(requests.peek() != null) {
				Request r = requests.poll();
				int numPackets = calculateNumPackets(r);
				BigDecimal newDuration = new BigDecimal(1).setScale(6).divide(packetRate, 6, BigDecimal.ROUND_HALF_UP);
				for(int i=0; i<numPackets; i++){
					BigDecimal newStart = r.getStart().add(new BigDecimal(i).setScale(6));
					packets.add(new Request(r.getDest(), r.getSource(), newStart, newDuration, "open", null));
				}
			}
			
			while(packets.peek() != null){
				Request packetRequest = packets.poll();
						
				if(packetRequest.getAction().equals("open")){
					ArrayList<Node> pathFound = rp.Dijkstra(packetRequest, graph);
					if(pathFound == null || !pathFound.get(pathFound.size()-1).equals(packetRequest.getDest())){
						//Packet blocked
						numBlockedPackets += 1;
					} else {
						//Packet path found
						hopsInCircuit.add(calculateNumHops(pathFound));
						numSuccessfulPackets += 1;
						dpropInCircuit.add(calculateCumulativePropDelay(pathFound));
						updateGraph(pathFound, "add");
						packets.add(new Request(null, null, packetRequest.getStart().add(packetRequest.getDuration()), null, "close", pathFound));	
					}
				} else {
					//close the links.
					updateGraph(packetRequest.getPath(), "remove");
				}
				numRequests = (int)(numSuccessfulPackets + numBlockedPackets);
			}
		}
		
		System.out.println("total number of virtual circuit requests: " + (int)numRequests);
		System.out.println("total number of packets: " + (int)(numSuccessfulPackets + numBlockedPackets));
		System.out.println("total number of successfully routed packets: " + (int)numSuccessfulPackets);
		System.out.println("percentage of successfully routed packets: " + new BigDecimal((numSuccessfulPackets*100/(numSuccessfulPackets + numBlockedPackets))).setScale(2, BigDecimal.ROUND_HALF_UP));
		System.out.println("total number of blocked packets: " + (int)numBlockedPackets);
		System.out.println("percentage of blocked packets: " + new BigDecimal((numBlockedPackets*100/(numSuccessfulPackets + numBlockedPackets))).setScale(2, BigDecimal.ROUND_HALF_UP));
		System.out.println("average number of hops in circuit: " +  new BigDecimal(calculateAvgNumHops(hopsInCircuit)).setScale(2, BigDecimal.ROUND_HALF_UP));
		System.out.println("average cumulative propagation delay per circuit: " + new BigDecimal(calculateAvgCumulativePropDelay(dpropInCircuit)).setScale(2, BigDecimal.ROUND_HALF_UP));
		

	}
	
	

	private static void createRequests(String file) {
		Scanner sc;
		try {
			sc = new Scanner(new FileReader(file));
			while(sc.hasNext()){
				String [] params = sc.nextLine().split(" ");
				BigDecimal time = new BigDecimal(params[0]).setScale(6, BigDecimal.ROUND_UNNECESSARY);
				Node to = getNodeWithName(params[1]);
				Node from = getNodeWithName(params[2]);
				BigDecimal duration = new BigDecimal(params[3]);
				Request req = new Request(to, from, time, duration, "open", null);
				requests.add(req);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}

	private static void createGraph(String file) {
		Scanner sc;
		try {
			sc = new Scanner(new FileReader(file));
			while(sc.hasNext()){
				String [] params = sc.nextLine().split(" ");
				boolean src = false;
				boolean dest = false;
				
				for(Node n : graph){
					if (n.getName().equals(params[0])) src = true;
					if (n.getName().equals(params[1])) dest = true;
				}
				
				if(src == false){
					graph.add(new Node(params[0]));
				}
				
				if(dest == false){
					graph.add(new Node(params[1]));
				}
				
				for(Node n : graph){
					if(n.getName().equals(params[0])){
						n.createLink(getNodeWithName(params[1]), Integer.parseInt(params[2]), Float.parseFloat(params[3]));
					} else if(n.getName().equals(params[1])){
						n.createLink(getNodeWithName(params[0]), Integer.parseInt(params[2]), Float.parseFloat(params[3]));
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	public static Node getNodeWithName(String name){
		Node result = null;
		for (Node n: graph){
			if (n.getName().equals(name)){
				result = n;
				break;
			}
		}
		return result;

	}

	private static int calculateNumPackets(Request r){
		return r.getDuration().multiply(packetRate).intValue();
	}
	
	private static float calculateNumHops(ArrayList<Node> path){
		return path.size()-1;
	}
	
	private static float calculateCumulativePropDelay(ArrayList<Node> path){
		float sum = 0;
		for(int i=0; i<path.size()-1; i++){
			Node curr = path.get(i);
			Node next = path.get(i+1);
			sum += curr.getLink(next).getDprop();
		}
		return sum;
	}
	
	private static float calculateAvgCumulativePropDelay(ArrayList<Float> delays){
		float sum = 0;
		for(float d : delays){
			sum += d;
		}
		return sum/delays.size();
	}
	
	private static float calculateAvgNumHops(ArrayList<Float> hopsInCircuit){
		float hopSum = 0;
		for(float val : hopsInCircuit){
			hopSum+=val;
		}
		return hopSum/hopsInCircuit.size();
	}
	
	/**
	 * Updates our graph by adding or subtracting load of 1 from links in a given path
	 * @param path
	 * @param option
	 */
	private static void updateGraph(ArrayList<Node> path, String option){
		for(int i=0; i+1<path.size(); i++){
			Node curr = path.get(i);
			Node next = path.get(i+1);
			if(option.equals("remove")){
				curr.getLink(next).removeLoad();
				next.getLink(curr).removeLoad();
			} else {
				curr.getLink(next).addLoad();
				next.getLink(curr).addLoad();
			}
			
		}
	}
}


