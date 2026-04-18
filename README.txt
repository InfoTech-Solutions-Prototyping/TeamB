IPOS-CA - InfoPharma Client Application
Team 14 (Valinor) - Group 5


Overview:
IPOS-CA is the Client Application (CA) component of the InfoPharma pharmacy supply chain system. It manages local stock, customer accounts, sales, supplier catalogue browsing, and purchasing unit orders.
It connects to:
  - SA via MySQL for catalogue sync and orders
  - PU via HTTP API on port 8080 and email orders
  - Gmail (IMAP) for receiving PU order emails


How to set up in intellij:
1. Open IntelliJ IDEA
2. Click "New Project" or "Open" and select this folder
3. Make sure the project SDK is set to Java 17 or higher
4. Add the required library JARs:
   a. Download the .jar files:
      SQLite JDBC (local database):
        https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar
      SLF4J (SQLite logging dependency):
        https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar
        https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar
      MySQL Connector (for SA catalogue sync):
        https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.6.0/mysql-connector-j-9.6.0.jar
      JavaMail (for reading PU order emails via Gmail):
        https://repo1.maven.org/maven2/com/sun/mail/javax.mail/1.6.2/javax.mail-1.6.2.jar
      Java Activation (required by JavaMail on JDK 17+):
        https://repo1.maven.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar
      Protocol Buffers (required by MySQL Connector):
        https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/4.31.1/protobuf-java-4.31.1.jar

   b. Create a "lib" folder in the project root
   c. Put the downloaded .jar files into the lib folder
   d. In IntelliJ: File > Project Structure > Libraries > + > Java
   e. Select a jar from the lib folder > OK
   f. do the same for the rest of the files.

5. Go to file > project structure > modules
    a. add a module pointing to src/main
    b. add another module pointing to src/test

6. go to file > project structure > modules > main > dependencies
    a. press + and select jars or directories
    b. select the path to the lib folder in the project root

7. Run the application:
   a. Open src/main/java/com/valinor/iposca/Main.java
   b. Click the green play button next to "public static void main"


SA connection:
To sync the SA catalogue and place SA orders, SA's MySQL database
must be running on the same machine.
  Host:     localhost
  Port:     3306
  Database: ipos_sa
  User:     root
  Password: configure to password set on device
If SA is not running, the catalogue sync and SA order features
will show an error but the rest of the app will still work.

PU integration:
When this app is running, PU can browse the CA catalogue at:
  http://localhost:8080/catalogue         — all items
  http://localhost:8080/catalogue/search?q=keyword
  http://localhost:8080/catalogue/item?id=ITEM_ID
PU sends orders via email to: ipos.ca.smtp@gmail.com
Use the "Order Emails" tab to fetch and import those orders.


Project Structure:
src/main/java/
  API/
    CatalogueServer.java          HTTP server exposing catalogue to PU on port 8080
    SA_CA_interface.java          Interface for SA -> CA data (catalogue sync)
    SA_CA_implementation.java     Implementation of SA -> CA interface
    CA_PU_interface.java          Interface for CA -> PU data (not used — replaced by HTTP)
    CA_PU_implementation.java     Implementation of CA -> PU interface (not used — replaced by HTTP)
  com/valinor/iposca/
    Main.java                     Entry point — starts DB, HTTP server, and sign-in window
    db/
      DatabaseManager.java        Database connection and table initialisation
    model/
      AccountHolder.java          Customer account data class
      ApplicationUser.java        Logged-in user data class
      SACatalogueItem.java        SA catalogue item data class
      Sale.java                   Sale data class
      SaleItem.java               Individual line item within a sale
      StockItem.java              Local stock item data class
    dao/
      CustomerDAO.java            Customer accounts, status updates, reminders, payments
      SalesDAO.java               Sales recording and history
      StockDAO.java               Local stock CRUD and low-stock checks
      SACatalogueDAO.java         Local SQLite cache of SA's catalogue
      SAConnectionManager.java    MySQL connection to SA's database
      PUOrderDAO.java             PU order management
      TemplateDAO.java            Merchant details and reminder templates
      ReportDAO.java              Sales, low stock, and debtor report generation
      UserDAO.java                User account management
    gui/
      SignInFrame.java            Login screen
      MainFrame.java              Main window with sidebar navigation
      StockPanel.java             Local stock management
      CustomerPanel.java          Customer account management
      SalesPanel.java             Sales processing and history
      SACataloguePanel.java       SA catalogue browser with sync
      SAOrdersPanel.java          Orders placed to SA
      PUOrderPanel.java           PU order management
      EmailPanel.java             Gmail inbox for PU order emails
      TemplatePanel.java          Pharmacy details and reminder templates
      ReportPanel.java            Reports (sales, low stock, debtors)
      UserPanel.java              Admin-only user account management
    util/
      AppTheme.java               Central theme — colours, fonts, button builders

src/test/java/com/valinor/iposca/dao/
  CustomerDAOTest.java            Tests for customer account operations
  SalesDAOTest.java               Tests for sales recording
  StockDAOTest.java               Tests for stock management
  UserDAOTest.java                Tests for user account management
  InterfaceTest.java              Tests for CA-PU and SA-CA interfaces


Default logins:
  Username: admin
  Password: admin123
  Role: Admin

Roles and access:
  Admin       — Full access including User Management and Reports
  Manager     — All tabs including Reports, excluding User Management
  Pharmacist  — Local Stock, Customers, Sales
  Test        — Read-only access for demo purposes
