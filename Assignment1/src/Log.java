import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class Log {
	private String name;
	
	public Log(String name) throws IOException {
		this.name = name;
	}
	
	public void addToLog(String line) throws Exception{
		String msg = System.lineSeparator() + line;
		File file = new File(name);
		if(!file.exists()){
			file.createNewFile();
		} 
		FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(msg);
		bw.close();
		fw.close();
	}
	

}
