package db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public class Menu {

    Connection conn;
    
    
    Scanner scanner;
    public Menu(Connection conn) {
        this.scanner = new Scanner(System.in);
        this.conn = conn;

    }

    public void start() throws SQLException {
        
        
        String option1 = "Assign a tour to a Guide";
        String option2 = "Get the monthly salary of a guide ";
        String option3 = "Retrieve the most loyal client during on a chosen month of this year";
        String option4 = "Retrieve the best selling tour during on a chosen month of this year";
        String option5 = "Insert a guide into the database";
        String option6 = "Find out how many times and name of tour the guide has done in transfer";
        int option = -1;
        
    while (option != 0){

        System.out.printf("%nPlease select an option:%n%n1: %s%n2: %s%n3: %s%n4: %s%n5: %s%n6:%s%n0 to exit%n%n",option1,
                            option2,option3,option4,option5,option6);
        
      
        option = scanner.nextInt();
       
        switch (option){

            case 1: 
                new AssignAtour(conn).execute();
                    break;
            case 2: 
                new MonthlySalary(conn).execute();
                    break;
            case 3: 
                new MostLoyalClient(conn).execute();
                    break;
            case 4: 
                new MostPopularTour(conn).execute();;
                    break;
            case 5: 
                new InsertAguide(conn).execute();;
                    break;
            case 6: 
                new TourInTransfer(conn).execute();;
                    break;
            case 0: conn.close();
                scanner.close();
            break;
        }
    }
        System.out.println("Connection closed.");

      
    }

}
