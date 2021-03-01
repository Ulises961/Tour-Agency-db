# Tour Agency 
## A PostgreSQL & JDBC dabase system


 In this project I have developed from scratch a simple application to keep track of the accounting area of a mock tour agency that delivers tours for cruise ships. The idea was to apply the concepts learnt on an introductory database course.
 The interaction with the database is done through JDBC on the command line. A menu allows the user to interact with the database via a set of standard functionalities established at desing phase and that cover the needs of the users at the time of development. As the application is done in a modular fashion needed extensions are possible.

### Executing the project
 The execution of this project assumes a PostgresSQL server running on the localhost with the agency.sql file in src/main/java/resources loaded into the PostgresSQL server and Maven as build automation system. With this pre-requisites running the Application can be executed through maven with the command:
``` mvn exec:exec -Dexec.mainClass="db.App" ``` 


