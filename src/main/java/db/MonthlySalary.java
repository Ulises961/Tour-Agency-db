package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class MonthlySalary {

    PreparedStatement pstmt;
    Connection conn;
    public MonthlySalary(Connection conn) {
        
            this.conn = conn;
    }


  
    public void execute() {
        try {
            calculateSalary();
            pstmt.close();

        } catch (Exception e) {
            System.err.println("The query has been aborted.\n"+e.getMessage()+"\n\n");
            
        }
    }
         
    private void calculateSalary() throws SQLException {

        String calculateSalary ="SELECT SUM(guide_honorary) as salary, Guide.name AS name "+
                                "FROM Delivers JOIN Guide ON Guide.code = Delivers.guide "+
                                "WHERE guide = ? "+
                                "AND EXTRACT (MONTH FROM date) = ? "+ 
                                "AND EXTRACT (YEAR FROM date) = EXTRACT (YEAR FROM NOW())"+
                                "GROUP BY Guide.name";
                        
        presentGuides();
        int code = Utils.getInput("Please insert the code of the guide");
        int month = Utils.getInput("Please insert the month in format MM");
    
         
        try {
            pstmt = conn.prepareStatement(calculateSalary);
            pstmt.setInt(1,code);
            pstmt.setInt(2,month);
            ResultSet  table = pstmt.executeQuery();
            int i = 0;

            while(table.next()){
                System.out.printf("%n%n------The salary of guide %d (%s) on %d is € %d.-------%n%n",code,table.getString("name"),month, table.getInt("salary"));
                i++;
            }
            if (i == 0)
            System.out.println("The guide has not worked for the company on the selected month");
           
    } catch (SQLException e) {
        conn.rollback();
        throw new SQLException(e);
    }

    }
    private void presentGuides() throws SQLException {
        System.out.println("Guides in database\n\nCode Name");
        String listOfGuides = " SELECT code, name FROM Guide";
        pstmt = conn.prepareStatement(listOfGuides);
        ResultSet table = pstmt.executeQuery();
        while (table.next()) {
            System.out.printf("%d %s%n", table.getInt("code"), table.getString("name"));
        }
        pstmt.clearParameters();
    }


  
}
