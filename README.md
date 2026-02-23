
# Online Voting System using Java

A desktop-based online voting system developed using Java Swing and JDBC.  
The application allows voter registration, secure login, one-time vote casting, and automatic result computation using a MySQL database.

---

## Features
- Voter registration and authentication
- One-vote-per-user enforcement
- Java Swing graphical user interface
- JDBC-based MySQL database integration
- Real-time vote counting and result display

---

## Technology Stack
- Java SE
- Swing & AWT
- JDBC
- MySQL

---

## Project Structure
Java_project/
├─ src/
│ ├─ Main.java
│ ├─ DBConnection.java
│ ├─ LoginFrame.java
│ ├─ RegisterFrame.java
│ ├─ VotingFrame.java
│ └─ AdminPanel.java
└─ lib/
└─ mysql-connector-j-8.3.0.jar

---
## How to Run
1. Install JDK 8 or higher
2. Install MySQL and create the database and tables
3. Add the MySQL JDBC JAR to the project
4. Update database credentials in `DBConnection.java`
5. Run `Main.java`
---
## Application Flow
1. Register a new voter
2. Login using registered credentials
3. Cast vote (only once)
4. View election results
---
## Notes
- Desktop application only
- Requires local MySQL server
- No external libraries used
---
## License
Academic / Educational Use
