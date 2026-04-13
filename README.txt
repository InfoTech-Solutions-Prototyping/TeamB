===================================================
IPOS-CA - InfoPharma Client Application
Team 14 (Valinor) - Group 5
===================================================

HOW TO SET UP IN INTELLIJ IDEA:
---------------------------------

1. Open IntelliJ IDEA
2. Click "New Project" or "Open" and select this folder
3. Make sure the project SDK is set to Java 17 or higher

4. IMPORTANT - Add the SQLite JDBC driver:
   a. Download the SQLite JDBC jar and other 2 files:
      https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar
      https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar
      https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar

   b. Create a "lib" folder in the project root
   c. Put the downloaded .jar files into the lib folder
   d. In IntelliJ: File > Project Structure > Libraries > + > Java
   e. Select the sqlite-jdbc jar from the lib folder > OK
   f. do the same for the rest of the files.

5. Set the source root:
   a. Right-click on src/main/java > Mark Directory as > Sources Root

6. Run the application:
   a. Open src/main/java/com/valinor/iposca/Main.java
   b. Click the green play button next to "public static void main"

The database file (ipos_ca.db) will be created automatically
in the project root folder when you first run the app.

PROJECT STRUCTURE:
---------------------------------
com.valinor.iposca/
  Main.java              - Application entry point
  db/
    DatabaseManager.java - Database connection and table setup
  model/
    StockItem.java       - Stock item data class
  dao/
    StockDAO.java        - Stock database operations
  gui/
    MainFrame.java       - Main application window with tabs
    StockPanel.java      - Stock management screen

sql/
  schema.sql             - Full database schema (for reference)

DEFAULT LOGIN:
  Username: admin
  Password: admin123
  Role: Admin

