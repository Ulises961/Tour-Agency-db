package db;

import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;


public class App 
{
    public static void main( String[] args )
    {
        
          
        try {

            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost/test";
           
            // Console con = System.console();  
            // // read line from the user input  
            // System.out.print("Please insert your username: ");  
              
            // String username =con.readLine("");  
              
           
            // // read password into the char array  
            // System.out.print("Please Enter Password:");  
            // String password = String.valueOf(con.readPassword(""));  
           
            Connection conn = DriverManager.getConnection(url, "postgres", "postgres");
            conn.setAutoCommit(false);
            Menu menu = new Menu(conn);
            System.out.print("\n\n"); 
            menu.start();
            
        } catch (Exception e) {
           System.err.println("Fatal error: connection to database aborted - " + e.getMessage());
        }
       


    }
}
