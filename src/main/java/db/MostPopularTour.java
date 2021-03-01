package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class MostPopularTour{
    Connection conn;
    PreparedStatement pstmt;


    public MostPopularTour(Connection conn) {

       this.conn = conn;
   
    }

  
    public void execute() {
        try {
            getBestSellingTour();
            pstmt.close();
          
        } catch (SQLException e) {
            System.err.println("The query has been aborted.\n"+e.getMessage()+"\n\n");
        }

    }

    private void getBestSellingTour() throws SQLException {
        
        

        String mostPopularTour = "SELECT count(D.tour) AS popularity, T.name As Tname "
                + "FROM delivers D JOIN tour T  ON D.tour = T.code " + "WHERE  EXTRACT (MONTH FROM D.date) = ? "
                + "AND EXTRACT (YEAR FROM D.date) = EXTRACT (YEAR FROM NOW()) " + " GROUP BY T.name ORDER BY (popularity) DESC LIMIT 1";
        try {
            pstmt = conn.prepareStatement(mostPopularTour);
            int month = Utils.getInput("Please insert the month");
            pstmt.setInt(1, month);

            ResultSet table = pstmt.executeQuery();
            int i = 0;

            while (table.next()) {
                System.out.printf(
                        "%n%n-------The most popular tour this month is %s - with %d bookings.-------%n%n",
                        table.getString("Tname"), table.getInt("popularity"));
                i++;
            }
            if(i == 0)
                System.out.println("\n\n-------There are no tours booked for this month-------\n\n");

        } catch (SQLException e) {
            e.printStackTrace();
            conn.rollback();
            throw new SQLException(e.getMessage());

        }
    }
}
