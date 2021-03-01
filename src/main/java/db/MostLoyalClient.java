package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



public class MostLoyalClient {

    PreparedStatement pstmt;
    Connection conn;
  
    public MostLoyalClient(Connection conn) {
        
            this.conn = conn;
    }

 
	public void execute() {
        try {
            findClient();
            pstmt.close();
           
        } catch (SQLException e) {
           System.err.println("The query has been aborted.\n"+e.getMessage()+"\n\n");
        }
        
    }
  
    private void findClient() throws SQLException {

        String mostLoyalClient ="SELECT COUNT(C.tax_code) AS loyalty, C.name "+
        "FROM Reserves R JOIN Client C ON R.client = C.tax_code "+
        "WHERE  EXTRACT (MONTH FROM R.date) = ? "+
        "AND EXTRACT (YEAR FROM R.date) = EXTRACT (YEAR FROM NOW()) "+
        "GROUP BY (C.name) " + 
        "ORDER BY loyalty DESC LIMIT 1";
    
         
        try {
            pstmt = conn.prepareStatement(mostLoyalClient);
            System.out.println("Retrieving the most loyal client\n");
            int month = Utils.validateMonth("Please insert a month in the format MM");
            
            pstmt.setInt(1, month);
           
            ResultSet  table =  pstmt.executeQuery();
            int i = 0;

            while(table.next()){
                System.out.printf("%n%n------ The most loyal client up to the moment is %s with %d bookings ------%n%n",
                                table.getString("name"), table.getInt("loyalty"));
                i++;
            }
            if(i == 0)
                System.out.println("\n\n------ All clients have the same ranking ------\n\n");

    } catch (SQLException e) {
        conn.rollback();
        throw new SQLException(e.getMessage()); return month;
       
    }

    }
}
