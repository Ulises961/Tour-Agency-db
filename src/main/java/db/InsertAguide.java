package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class InsertAguide {

  PreparedStatement pstmt;

  Connection conn;
  Statement stmt;

  public InsertAguide(Connection conn) {

    this.conn = conn;
    try {
      stmt = conn.createStatement();
    } catch (SQLException e) {
        System.err.println(e.getMessage());
    }
      
  }

  public void execute() {
    try {
      insertGuide();
      stmt.close();
      pstmt.close();
      System.out.println("Transaction successful\n\n");
    } catch (Exception e) {
        System.err.println(e.getLocalizedMessage()+"\nTransaction cancelled, try again.\n\n");
    }
      
  }
/** Insertion of a guide in the database, we collect all the information needed and then place make the insertion in all the corresponding tables*/
  private void insertGuide() throws Exception {
      
    System.out.println("Please insert guide's name");
    String name = Utils.readString();
    
    int tel = getTelephone();
    String base = selectBase();
    int guideId = getGuideId();

    List<Integer> lang = selectLanguages();
    String query = prepareQuery(lang);

    try{  
      pstmt = conn.prepareStatement(query);
      pstmt.setString(1,name);
      pstmt.setInt(2, guideId);
      pstmt.setInt(3, tel);
      pstmt.setString(4, base);

  /**The following lines correspond to the update of the statement with a variable number of languages */

      int j = 0;

      for(int i = 0; i < lang.size(); i++){
          pstmt.setInt(5 + j,guideId);
          pstmt.setInt(6 + j, lang.get(i));
          j += 2;
      }
            
            pstmt.executeUpdate();
  /** The mail is optional in the schema thus it is done once the guide has been inserted into the database */
            insertMail(guideId);
            
      }catch (SQLException e){
            conn.rollback();
            throw new Exception(e.getMessage());
            
          }
        }
        

   
  private boolean isMail(String mail){

    String regex = "^(.+)@(.+)$";

    Pattern pattern = Pattern.compile(regex);
    
    java.util.regex.Matcher matcher = pattern.matcher(mail);

    return matcher.matches();
  }

/**
 * Prompts the user to insert a valid telephone number and returns when the
 * validated number
 * 
 * @throws Exception
 */

private int getTelephone() throws Exception {
    int telephone = 0;

    while (!validatePhoneNumber(Utils.readString())){
      
      telephone = Utils.getInput("Please insert guide's telephone 10-11 digits, 0 to exit\n");
      
      if (telephone == 0)
        throw new Exception("Exiting task\n");
    }
    return telephone;
  }
/** Query for the bases stored in the db and place them in a map then returns the map */
  private Map<String,String> getBases() throws Exception {

    Map<String,String> bases = new HashMap<String,String>();

    String query = "SELECT * FROM Base";

    try {
        
      ResultSet table = stmt.executeQuery(query);

      while (table.next()) {

        String name  = table.getString("name");

        String code = table.getString("CAP");

        bases.put(code,name);

      }

    } catch (SQLException e) {
        throw new Exception( e.getMessage());
    }

    return bases;
    }
/** Displays on the console the available bases and prompt the user to insert a valid CAP of the chosen base. Retunrs the CAP */
  private String selectBase() throws Exception {
    
    Map<String,String> bases= getBases();
    
    System.out.println("CAP codes of bases are");
    
    bases.forEach((k,v)-> System.out.println(k+" "+v));
    
    String input = Utils.readString();
    
    while(!bases.containsKey(input)){
        System.out.println("Please insert the CAP of the base, 0 to exit");
        
        input= Utils.readString();
        
        if(input.equals("0"))
          throw new Exception("Exiting task\n");
    }
    return input;
  }
/** Query for the languages stored in the db and place them in a map then returns the map */
  private Map<Integer,String> getLanguages() throws Exception {
    Map<Integer,String> languages = new HashMap<Integer,String>();
    String query = "SELECT * FROM Language";

    try {
      ResultSet table = stmt.executeQuery(query);

      while (table.next()) {

        int code = table.getInt("language_id");
        String name = table.getString("name");
        languages.put(code,name);

      }

    } catch (SQLException e) {
      throw new Exception( e.getMessage());
    }

    return languages;
  }
  
/** Query the languages in the db, and shows them to the user. Every language selected is placed in a list and removed from the available ones */
  private List<Integer> selectLanguages() throws Exception {

    Map< Integer,String> languages = getLanguages();
    List<Integer> listOfLangs = new LinkedList<>();
    String another = "y";

    int code = 0;
    System.out.println("The guide must speak at least 3 languages");
    while(listOfLangs.size() < 3 ){
        another = "y";
        while(another.equals("y")){
          System.out.println("Choose language\n");
          languages.forEach((k, v) -> System.out.printf("%d: %s%n", k, v));
      
          code = Utils.findCode(languages, "code of the language");
              
          if (code == 0)
            throw new Exception("Exiting task\n");

          listOfLangs.add(code);
          languages.remove(code);
  
          if(listOfLangs.size() >= 3){
            System.out.println("Does the guide speak another language? Press \"y\" to insert another one");
            another = Utils.readString();

          }
      }
        
    }
    
    return listOfLangs;
  }
  
  private void insertMail(int guide_id) throws SQLException{
    String choice = "";

    do{
      System.out.printf("Do you want to add a mail?(y or n)");
      choice = Utils.readString();

      if(choice.equals("y"))
        insertMailIntoDB(guide_id);
      
    }while(!choice.equals("n"));
  }
  
  private void insertMailIntoDB(int guide_id) throws SQLException{
    String query = "INSERT INTO has_Email VALUES(?,?)";
    String mail = readMail();
    
    try {
      pstmt = conn.prepareStatement(query);
      pstmt.setInt(1, guide_id);
      pstmt.setString(2, mail);
      
      pstmt.executeUpdate();
    } catch (SQLException e) {
        throw new SQLException( e.getMessage());
    }

  }

  private String readMail(){
    String mail = " ";
    
    while(!isMail(mail)){
      System.out.printf("\nInsert your mail: ");
      mail = Utils.readString();
    }

    return mail;
  }
/** Guide id must be collected by retrieving the last row of the table and adding 1 */
  private int getGuideId() throws Exception{
    int code = 0;
    
    String query = "SELECT code FROM Guide ORDER BY code DESC LIMIT 1";

    try {
      ResultSet table = stmt.executeQuery(query);
      
      while (table.next()) 
        code = table.getInt("code");

    } catch (SQLException e) {
        throw new SQLException( e.getMessage());
    }

    return code + 1;
  }
/** A query for insertion of a tuple in the guide table and into the is_fluent_in table is prepared,
 * since the guide can speak more than 3 languages the length if this string is variable 
 * we append an insertion for every language spoken by the guide */
  private String prepareQuery( List<Integer> lang) throws Exception {
    int i = 0;
    String query = 
    "BEGIN;" + 
    "INSERT INTO GUIDE VALUES"+
    "(?,?,?,?);";
  
    while (i < lang.size()) { 
        
      query += " INSERT INTO is_fluent_in VALUES ";
      query += "(?,?);";
      i++;
    }
    
    query += 
    "END TRANSACTION;";

    return query;
  }

//-----Credits: https://www.javaprogramto.com/2020/04/java-phone-number-validation.html----//
     
  private static boolean validatePhoneNumber(String phoneNumber) {
        // validate phone numbers of format "1234567890"
        if (phoneNumber.matches("\\d{10}"))
         return true;
        // validating phone number with -, . or spaces
        else if (phoneNumber.matches("\\d{3}[-\\.\\s]\\d{3}[-\\.\\s]\\d{4}"))
         return true;
        // validating phone number with extension length from 3 to 5
        else if (phoneNumber.matches("\\d{3}-\\d{3}-\\d{4}\\s(x|(ext))\\d{3,5}"))
         return true;
        // validating phone number where area code is in braces ()
        else if (phoneNumber.matches("\\(\\d{3}\\)-\\d{3}-\\d{4}"))
         return true;
        // Validation for India numbers
        else if (phoneNumber.matches("\\d{4}[-\\.\\s]\\d{3}[-\\.\\s]\\d{3}"))
         return true;
        else if (phoneNumber.matches("\\(\\d{5}\\)-\\d{3}-\\d{3}"))
         return true;
      
        else if (phoneNumber.matches("\\(\\d{4}\\)-\\d{3}-\\d{3}"))
         return true;
        // return false if nothing matches the input
        else
         return false;
      
       
      }


}
