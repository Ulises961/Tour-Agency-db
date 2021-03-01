package db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Time;
import java.util.Date;
import java.util.Map;

/** Utility class to read and parse the inputs of the user on the console*/
public class Utils {

  static InputStreamReader input = new InputStreamReader(System.in);
  static BufferedReader keyboard = new BufferedReader(input);

  static int parseToInt(String line) {

      int code = 0;

      try {
          code = Integer.parseInt(line);
        
        }catch (NumberFormatException e) {}
        
        return code;
    }
  
  public static int getInput(String message) {
    
    int code = -1;
    
    while (code < 0 ){
        System.out.println(message);
        String line = readString();
        code = parseToInt(line); 
      
    }
    
    return code;
  }

  public static <K,V> int findCode( Map<K,V> map,String message) throws Exception {
    int code = -1;
    while (!map.containsKey(code)) {
      code = getInput("Please insert " + message + " or 0 to return\n");

      if (code == 0)
        throw new Exception("Exiting task\n");

    }
    return code;
  }

  
  public static String readString(){
    String line= "";
    try{
      
      line = keyboard.readLine();      
      
    }catch(IOException e){
      System.err.println(e.getMessage());
    }
    return line;
  }

  public static int validateMonth(String message){
    int month = 13;

    while(month > 12)
      month = getInput(message);
    
    return month;
  }
 

}
