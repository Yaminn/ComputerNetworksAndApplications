import java.util.Comparator;

public class RequestComparator implements Comparator<Request>{
	@Override
    public int compare(Request x, Request y){
        return x.getStart().compareTo(y.getStart());
    }
}
