package tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang3.text.WordUtils;

public class TextFileWrapper {
	private static final String CONFPATH = "c:\\temporary\\";
	private static final String TEMP_FILE = "vitek.txt";
	
	
	public TextFileWrapper(String fullFileName){
		try (BufferedReader br = new BufferedReader( new FileReader(fullFileName))){
			String line = br.readLine();
			String singleLine = new String();
			
			while (line != null) {
				System.out.println("line : "+line);
				singleLine = singleLine +" "+line;
				line = br.readLine();
			}
			 
			System.out.println("single : "+singleLine);
			
			String wrappedText = WordUtils.wrap(singleLine, 42, ""+(char) 0x0A, true);
			
			System.out.println("wrap : \n"+wrappedText);
			
		} catch (IOException e) {
			System.out.println("error reading header text file");
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		new TextFileWrapper(CONFPATH+TEMP_FILE);
		
		
	}

}
