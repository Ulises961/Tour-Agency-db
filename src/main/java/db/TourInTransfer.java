package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TourInTransfer {

    PreparedStatement pstmt;
    Connection conn;
    public TourInTransfer(Connection conn) {
       
        this.conn = conn;
    
    }



    public void execute() {
        try {
            getToursInTransfer();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("The query has been aborted.\n"+e.getMessage()+"\n\n");
        }

    }

    private void getToursInTransfer() throws SQLException {

        String toursIntransfer ="SELECT COUNT(*) AS times,T.name AS Tname "+
                                "FROM Guide G, Delivers D, Tour T "+
                                "WHERE G.code = ? "+
                                "AND G.code = D.guide AND D.tour = T.code " +
                                "AND G.base <> T.city " +
                                "GROUP BY (T.name)";

        try {
            pstmt = conn.prepareStatement(toursIntransfer);
            int code = Utils.getInput("Please insert the guide's code");
            pstmt.setInt(1, code);

            ResultSet table = pstmt.executeQuery();
            int i = 0;
            System.out.printf("%n%n------The guide with code %d has ",code);
            while (table.next()) {
                System.out.printf("done %d times %s", table.getInt("times"), table.getString("Tname"));
                i++;
            }
            if ( i == 0)
                System.out.print("not done tours in transfer");
            System.out.printf("-------%n%n");

        } catch (SQLException e) {
            conn.rollback();
            throw new SQLException(e.getMessage());

        }
    }

}
