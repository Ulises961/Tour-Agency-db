package db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AssignAtour {

  PreparedStatement pstmt;
  Statement stmt;
  Connection conn;

  public AssignAtour(Connection conn) {
    this.conn = conn;
    try {
     
      this.stmt = conn.createStatement();
    
    } catch (SQLException e) {
      e.printStackTrace();
    }

  }


  public void execute() {
    
    try {

      createTransaction();
      System.out.println("Transaction successful\n\n");
      stmt.close();
      pstmt.close();
      
    } catch (Exception e) {
      System.err.println("The query has been aborted.\n"+e.getMessage()+"\n\n");
    }
  }

  /**We collect information to insert a tuple into groups,speaks delivers and reserves, we need the tour, the guide, the languages spoken and client */
  void createTransaction() throws Exception {
  

    Tour tour = selectTour();
    Date date = Date.valueOf(getDate());
    int client = selectClient();
    int groupId = getGroupId(); 
    int numPax = getNumPax();
    int basePrice = getBasePrice(client);
    List<Integer> lang = selectLanguages();
    Guide guide = selectGuide(lang);
    int guide_honorary = calculateHonoraries(guide,tour,lang.size(),basePrice);
    String query = String.format(
    "BEGIN;" + 
    "INSERT INTO GROUPS VALUES" + 
    "(?,?,?);" + 
    "INSERT INTO SPEAKS VALUES"+
    "(?,?,?);" + 
    "INSERT INTO RESERVES VALUES" + 
    "(?,?,?,?,?,?,?);" +
    "INSERT INTO DELIVERS VALUES" + 
    "(?,?,?,?,?,?,?);");

    if (lang.size() > 1) { // Bilingual tour
      query += 
      "INSERT INTO SPEAKS VALUES" + 
      "(?,?,?);";
    }
    query += 
    "COMMIT;";
    try{ 
      pstmt = conn.prepareStatement(query);
   

      pstmt.setInt(1,groupId );
      pstmt.setDate(2, date);
      pstmt.setInt(3, numPax);

      pstmt.setInt(4, groupId);
      pstmt.setDate(5, date);
      pstmt.setInt(6, lang.get(0));

      pstmt.setInt(7, groupId);
      pstmt.setDate(8, date);
      pstmt.setInt(9, client);
      pstmt.setInt(10, tour.getCode());
      pstmt.setTime(11, tour.getStart_time());
      pstmt.setTime(12, tour.getEnd_time());
      pstmt.setInt(13, basePrice);

      pstmt.setInt(14, guide.getCode());
      pstmt.setInt(15, tour.getCode());
      pstmt.setTime(16, tour.getStart_time());
      pstmt.setTime(17, tour.getEnd_time());
      pstmt.setInt(18, groupId);
      pstmt.setDate(19, date);
      pstmt.setInt(20, guide_honorary);

      if (lang.size() > 1) {
        pstmt.setInt(21, groupId);
        pstmt.setDate(22, date);
        pstmt.setInt(23, lang.get(1));
      }
      
      pstmt.execute();
    
    }catch (SQLException e){
      conn.rollback();
      System.err.println(e.getLocalizedMessage()+"\nTransaction cancelled, try again.");
     
    }
  }
/** We query for all the clients in the db and  insert them in a map, we return the map */
  private Map<Integer, String> getClients() {

    Map<Integer, String> clients = new TreeMap<Integer, String>();
    String query = "SELECT * FROM Client";

    try {
      ResultSet table = stmt.executeQuery(query);

      while (table.next()) {

        int code = table.getInt("tax_code");
        String name = table.getString("name");

        clients.put(code, name);
      }

    } catch (SQLException e) {
      e.getMessage();
    }

    return clients;
  }
/** Prints on the console the clients available  and prompts the user to choose a valid Tax code, returns the chosen code */
  private int selectClient() throws Exception {
    Map<Integer, String> clients = getClients();
    System.out.println("Clients available: ");
    clients.forEach((k, v) -> System.out.printf("%d: %s%n", k, v));
    
    int client = Utils.findCode(clients, "code");

    return client;
  }

  private LocalDate getDate() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    System.out.println("Insert a date in the format DD-MM-YYYY");
    String date = "";
    LocalDate parsedDate = null;
     
    do {
      date = Utils.readString();
      try {
        parsedDate = LocalDate.parse(date, formatter);
        if(parsedDate.getYear() > 2025 || parsedDate.getYear() < LocalDate.now().getYear())
          throw new DateTimeException(date);
       
      } catch (DateTimeException e) {
        System.out.println("Please insert a valid date");
        date = "";
      }
    }while (date.equals(""));

    return parsedDate;
  }
/** We need to show the available languages, thus we query the db for the information and store it in a map for later use. Information is stored for the duration of the transaction */
  private Map<Integer,String> getLanguages() throws Exception {
    Map<Integer,String> languages = new TreeMap<Integer,String>();
    String query = "SELECT * FROM Language";

    try {
      ResultSet table = stmt.executeQuery(query);

      while (table.next()) {

        int code = table.getInt("language_id");
        String name = table.getString("name");
        languages.put(code,name);

      }

    } catch (SQLException e) {
      throw new Exception(e.getMessage());
    }

    return languages;
  }

/** Prints on the console the languages available and prompts the user to choose a valid code, 
 * we select the language and remove it from the map where it is stored 
 * returns the list of languages spoken by the group */
  private List<Integer> selectLanguages() throws Exception { 
    Map< Integer,String> languages = getLanguages();
    List<Integer> languagesOfGroup = new LinkedList<>();
    String another = "y";

    while (another.equals("y")) {
      System.out.println("Choose language");
      languages.forEach((k, v) -> System.out.printf("%d: %s%n", k, v));
      int language = Utils.findCode(languages, "the code");
      languagesOfGroup.add(language);
      languages.remove(language);
      if (languagesOfGroup.size() == 2)
        return languagesOfGroup;

      System.out.println("Does the group speak another language? Press \"y\" to insert another one");
      another = Utils.readString();
    }

    return languagesOfGroup;
  }
/** We create Tour objects and insert it in a map so that, later on, we can use the information of the chosen tour for several check constraint before the insertion into the db */
  private Map<Integer, Tour> getTours() throws SQLException {
    String query = "SELECT * FROM Tour;";
    Map<Integer, Tour> tours = new TreeMap<Integer, Tour>();

    try {
      ResultSet table = stmt.executeQuery(query);

      while (table.next()) {
        int code = table.getInt("code");
        Tour tour = new Tour();
        tour.setCode(code);
        tour.setName(table.getString("name"));
        tour.setStart_time(table.getTime("start_time"));
        tour.setEnd_time(table.getTime("end_time"));
        tour.setCity(table.getString("city"));
        tours.put(code, tour);
      }

    } catch (SQLException e) {
      throw new SQLException("Error in getTours: "+ e.getMessage());
    }

    return tours;
  }
/** Prints on the console the tours available  and prompts the user to choose a valid code of a tour, returns the chosen code */
  private Tour selectTour() throws Exception {
    
    Map<Integer, Tour> tours = getTours();
    
    Tour selection = null;

    System.out.println(" Available Tours:");
    
    tours.forEach((k, v) -> System.out.printf("Code %d: %s%n", k, v));
    
    int code = Utils.findCode(tours, "the code" );
    
    selection = tours.get(code);
          
    return selection;
  }
/** We create Guide objects and insert them in a map so that, later on, we can use the information of the chosen guide for several check constraint before the insertion into the db */
  private Map<Integer, Guide> getGuides(List<Integer> language) throws Exception{

    Map<Integer, Guide> guides = new TreeMap<Integer, Guide>();
      
    PreparedStatement prstmt = null;

    if(language.size() == 2) // If the tour is bilingual we query for a guide that speaks those languages
      prstmt = twoLanguagesQuery(language);
      
    else if(language.size() == 1) // else we query for a guide that speaks the required language
      prstmt = oneLanguagesQuery(language);

    ResultSet table = prstmt.executeQuery();

    while (table.next()) { // We create guide objects and store them in a map, so that when the guide is chosen we can use its information to check pertinent constraints 

      Guide guide = new Guide();
      int code = table.getInt("code");
      guide.setCode(code);
      guide.setTelephone(table.getInt("telephone"));
      guide.setName( table.getString("name"));
      guide.setBase(table.getString("base"));
      guides.put(code, guide);
    }
    
    if (guides.size() == 0){ // If the map storing the guides is empty there was no matching candidate for the language combination.
      throw new SQLException(
        "No guides available for the language combination of the group.\n"+
        "Please split the group and try again. Aborting Transaction"
      );        
    }
    prstmt.close();
    
    return guides;

  }

  private PreparedStatement twoLanguagesQuery(List<Integer> language) throws Exception {

    PreparedStatement prstmt = null;
    
    try {
      prstmt = conn.prepareStatement("select * from guide, is_fluent_in L1, is_fluent_in L2 "
          + "where code = L1.guide AND L1.language = ? AND code = L2.guide AND L2.language = ?;");

      prstmt.setInt(1, language.get(0));
      
      prstmt.setInt(2, language.get(1));
    
    } catch (SQLException e) {
      throw new SQLException("Error in twoLanguagesQuery: " + e.getMessage());
    }
    
    return prstmt;
  }
  private PreparedStatement oneLanguagesQuery(List<Integer> language) throws Exception {

    PreparedStatement prstmt = null;
    
    try {
      prstmt = conn.prepareStatement("select * from guide, is_fluent_in L1 "
          + "where code = L1.guide AND L1.language = ?;");

      prstmt.setInt(1, language.get(0));
   
    } catch (SQLException e) {
      throw new SQLException("Error in oneLanguageQuery: " + e.getMessage());
    }
    
    return prstmt;
  }
/** Prints  on the console the guides that speak those languages and prompts the user to choose a valid id of a guide, returns the chosen id */
  private Guide selectGuide(List<Integer> language) throws Exception {

    Map<Integer, Guide> guides = getGuides(language);
    System.out.println("Guides available: ");
    guides.forEach((k, v) -> System.out.printf("%d: %s%n", k, v.getName()));
    
    Integer guide = Utils.findCode(guides, "a code");
    Guide candidate = guides.get(guide);
    return candidate;
  }
/** We collect the information of the last group inserted into the db and increment that number by one*/
  private int getGroupId() throws Exception{

    int code = 0;
    
    String query = "SELECT group_id FROM Groups ORDER BY group_id DESC LIMIT 1";

    try {
      ResultSet table = stmt.executeQuery(query);
      
      while (table.next()) 
        code = table.getInt("group_id");

    } catch (SQLException e) {
      throw new SQLException( e.getMessage());
    }

    return code + 1;
  }
/** Base price is calculated by adding to the client a 30% surcharge if it is a cruiseship */
  private int getBasePrice(int client_id) throws Exception{

    String query = "SELECT * from Cruise_ship where Cruise_ship.tax_code = " + client_id;
    int basePrice = 100;
    try {
      ResultSet table = stmt.executeQuery(query);

      if(table.next())
        basePrice += 50;
     
    } catch (SQLException e) {
      throw new SQLException( e.getMessage());
    }
  
    return basePrice;
  }

  private int getNumPax(){
    int numPax;
    do {
      numPax = Utils.getInput("Insert number of passengers (Min group 15- Max group 50):");
    } while (numPax < 15 || numPax > 50);
    
    return numPax;
  }

  
  private int calculateHonoraries(Guide guide, Tour tour,int languages,int basePrice){
    
    int guideHonorary= basePrice - (int)(basePrice * 0.40); /// guide gets 60% from the price paid by the client
    
    if(!guide.getBase().equals(tour.getCity()))
        guideHonorary += (int)basePrice * 0.1; // guide gets 10% extra if does a  tour away from home
    
    if(languages > 1)
        guideHonorary += (int)basePrice * 0.05; // guide gets 5% extra if does a bilingual tour
    
        return guideHonorary;
  }

  
}