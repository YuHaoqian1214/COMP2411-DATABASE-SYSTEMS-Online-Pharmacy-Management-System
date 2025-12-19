import java.io.*;
import java.sql.*;
import oracle.jdbc.driver.*;
import oracle.sql.*;
import java.util.*;

public class OPMSDemo {
    private static Scanner scanner = new Scanner(System.in);
    private static String currentRole; // Stores the current logged-in user's role
    private static String currentUsername; // Stores the current logged-in username for reference
    private static String currentSSN; // For customer role
    private static int currentDoctorId = -1; // For doctor role
    private static int currentEmployeeId = -1; // For employee role

    // Role-function permissions map: key is role, value is list of allowed function numbers
    private static final Map<String, List<Integer>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        // Initialize permissions
        ROLE_PERMISSIONS.put("customer", Arrays.asList(2, 3, 0)); // Customer: view prescriptions, place order, exit (removed register after login/registration)
        ROLE_PERMISSIONS.put("doctor", Arrays.asList(4, 0)); // Doctor: issue prescription, exit
        ROLE_PERMISSIONS.put("employee", Arrays.asList(5, 0)); // Employee: process order, exit
        ROLE_PERMISSIONS.put("admin", Arrays.asList(6, 7, 8, 9, 10, 11, 12, 0)); // Admin: monthly revenue, view expired stock, annual revenue, top sold drugs, top customers, top doctors, low stock, exit
    }

    public static void main(String[] args) throws SQLException, IOException, InterruptedException {
        // Clear screen
        clearScreen();
        Console console = System.console();
        View.displayPrompt("your username");    // Your Oracle ID, e.g. "24124366d"
        currentUsername = console.readLine();
        View.displayPrompt("your password");    // Password of your Oracle Account
        char[] password = console.readPassword();
        String pwd = String.valueOf(password);

        // Register Oracle driver and connect to database
        DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
        OracleConnection conn =
                (OracleConnection)DriverManager.getConnection(
                        "jdbc:oracle:thin:@studora.comp.polyu.edu.hk:1521:dbms", currentUsername, pwd);
        clearScreen();

        boolean running = true;
        while(running) {
            // Role selection and validation
            boolean validated = false;
            while(!validated) {
                View.displayMessage("Select your role:");
                View.displayMessage("1. Customer");
                View.displayMessage("2. Doctor");
                View.displayMessage("3. Employee");
                View.displayMessage("4. Admin");
                View.displayMessage("0. Exit");
                String roleChoice = readEntry("Enter role number: ");
                int roleNum;
                try {
                    roleNum = Integer.parseInt(roleChoice);
                }
                catch(NumberFormatException e) {
                    View.displayBadInput("valid number", roleChoice);
                    continue;
                }

                try {
                    switch(roleNum) {
                        case 1: // Customer
                            View.displayMessage("Are you a new customer or existing?");
                            View.displayMessage("1. New (Register)");
                            View.displayMessage("2. Existing (Login)");
                            String customerChoice = readEntry("Enter choice: ");
                            int customerType;
                            try {
                                customerType = Integer.parseInt(customerChoice);
                            }
                            catch(NumberFormatException e) {
                                View.displayBadInput("valid number", customerChoice);
                                continue;
                            }
                            if(customerType == 1) {
                                // Register new customer
                                String registeredSSN = registerCustomer(conn);
                                if(registeredSSN != null) {
                                    currentSSN = registeredSSN;
                                    currentRole = "customer";
                                    validated = true;
                                    View.displayMessage("Registration and login successful.");
                                }
                            }
                            else if(customerType == 2) {
                                // Login existing
                                String ssn = readEntry("Enter SSN: ");
                                String custPwd = readEntry("Enter Password: ");
                                String custSql = "SELECT 1 FROM Customer WHERE SSN = ? AND Password = ?";
                                try(PreparedStatement pstmt = conn.prepareStatement(custSql)) {
                                    pstmt.setString(1, ssn);
                                    pstmt.setString(2, custPwd);
                                    ResultSet rs = pstmt.executeQuery();
                                    if(rs.next()) {
                                        currentRole = "customer";
                                        currentSSN = ssn;
                                        validated = true;
                                        View.displayMessage("Customer role validated successfully.");
                                    }
                                    else {
                                        View.displayError("Invalid SSN or Password.");
                                    }
                                }
                            }
                            else {
                                View.displayError("Invalid choice.");
                            }
                            break;
                        case 2: // Doctor
                            int docId = Integer.parseInt(readEntry("Enter Doctor ID: "));
                            String docPhone = readEntry("Enter Phone: ");
                            String docSql = "SELECT 1 FROM Doctor WHERE Doctor_ID = ? AND Phone = ?";
                            try(PreparedStatement pstmt = conn.prepareStatement(docSql)) {
                                pstmt.setInt(1, docId);
                                pstmt.setString(2, docPhone);
                                ResultSet rs = pstmt.executeQuery();
                                if(rs.next()) {
                                    currentRole = "doctor";
                                    currentDoctorId = docId;
                                    validated = true;
                                    View.displayMessage("Doctor role validated successfully.");
                                }
                                else {
                                    View.displayError("Invalid Doctor ID or Phone.");
                                }
                            }
                            break;
                        case 3: // Employee
                            int empId = Integer.parseInt(readEntry("Enter Employee ID: "));
                            String empPhone = readEntry("Enter Phone: ");
                            String empSql = "SELECT 1 FROM Employee WHERE Employee_ID = ? AND Phone = ?";
                            try(PreparedStatement pstmt = conn.prepareStatement(empSql)) {
                                pstmt.setInt(1, empId);
                                pstmt.setString(2, empPhone);
                                ResultSet rs = pstmt.executeQuery();
                                if(rs.next()) {
                                    currentRole = "employee";
                                    currentEmployeeId = empId;
                                    validated = true;
                                    View.displayMessage("Employee role validated successfully.");
                                }
                                else {
                                    View.displayError("Invalid Employee ID or Phone.");
                                }
                            }
                            break;
                        case 4: // Admin (no validation, for demo purposes)
                            currentRole = "admin";
                            validated = true;
                            View.displayMessage("Admin role selected (no validation).");
                            break;
                        case 0: // Exit the program
                            running = false;
                            validated = true;
                            break;
                        default:
                            View.displayError("Invalid role choice.");
                    }
                }
                catch(SQLException e) {
                    View.displayError("Validation failed: " + e.getMessage());
                }
                catch(NumberFormatException e) {
                    View.displayError("Invalid ID input.");
                }
            }

            if(!running) {
                break;
            }

            View.displayMessage("\nPress Enter to continue...");
            System.in.read();
            clearScreen();

            // Main loop
            int choice = 0;
            while(choice != -1) {
                displayMenu();
                String input = readEntry("Enter your choice: ");
                try {
                    choice = Integer.parseInt(input);
                }
                catch(NumberFormatException e) {
                    View.displayBadInput("valid number", input);
                    continue;
                }

                // Permission check
                List<Integer> allowed = ROLE_PERMISSIONS.get(currentRole);
                if(allowed == null || !allowed.contains(choice)) {
                    View.displayError("Insufficient permissions! This function is only open to " + getRoleDesc(currentRole) + ".");
                    View.displayMessage("\nPress Enter to continue...");
                    System.in.read();
                    clearScreen();
                    continue;
                }

                switch(choice) {
                    case 1:
                        registerCustomer(conn);
                        break;
                    case 2:
                        viewPrescriptions(conn);
                        break;
                    case 3:
                        placeOrder(conn);
                        break;
                    case 4:
                        issuePrescription(conn);
                        break;
                    case 5:
                        processOrder(conn);
                        break;
                    case 6:
                        monthlyRevenueReport(conn);
                        break;
                    case 7:
                        viewExpiredStock(conn);
                        break;
                    case 8:
                        annualRevenueReport(conn);
                        break;
                    case 9:
                        topSoldDrugsReport(conn);
                        break;
                    case 10:
                        topCustomersReport(conn);
                        break;
                    case 11:
                        topDoctorsReport(conn);
                        break;
                    case 12:
                        lowStockReport(conn);
                        break;
                    case 0:
                        choice = -1;
                        break;
                    default:
                        View.displayError("Invalid choice. Please try again.");
                }
                if(choice != -1) {
                    View.displayMessage("\nPress Enter to continue...");
                    System.in.read();
                }
                clearScreen();
            }
        }

        // Exit
        conn.close();
        View.displayExit();
        System.in.read();
        clearScreen();
    }

    private static void displayMenu() {
        View.displayMessage("--- Online Pharmacy Management System (OPMS) Demo ---");
        List<Integer> allowedFunctions = ROLE_PERMISSIONS.get(currentRole);

        if(allowedFunctions.contains(1)) {
            View.displayMessage("1. Register Customer");
        }
        if(allowedFunctions.contains(2)) {
            View.displayMessage("2. View Prescriptions (for a customer)");
        }
        if(allowedFunctions.contains(3)) {
            View.displayMessage("3. Place Order");
        }
        if(allowedFunctions.contains(4)) {
            View.displayMessage("4. Issue Prescription (as doctor)");
        }
        if(allowedFunctions.contains(5)) {
            View.displayMessage("5. Process Order (as employee)");
        }
        if(allowedFunctions.contains(6)) {
            View.displayMessage("6. Monthly Revenue Report");
        }
        if(allowedFunctions.contains(7)) {
            View.displayMessage("7. View Expired Stock");
        }
        if(allowedFunctions.contains(8)) {
            View.displayMessage("8. Annual Revenue Report");
        }
        if(allowedFunctions.contains(9)) {
            View.displayMessage("9. Top 5 Most Sold Drugs Report");
        }
        if(allowedFunctions.contains(10)) {
            View.displayMessage("10. Top 5 Customers by Spending Report");
        }
        if(allowedFunctions.contains(11)) {
            View.displayMessage("11. Top 5 Doctors by Prescriptions Report");
        }
        if(allowedFunctions.contains(12)) {
            View.displayMessage("12. Low Stock Medicines Report");
        }
        if(allowedFunctions.contains(0)) {
            View.displayMessage("0. Exit");
        }
    }

    private static String registerCustomer(Connection conn) {
        View.displayMessage("\n--- Register Customer ---");
        String ssn = readEntry("SSN: ");
        String firstName = readEntry("First Name: ");
        String lastName = readEntry("Last Name: ");
        String gender = readEntry("Gender (male/female): ");
        String dob = readEntry("Date of Birth (YYYY-MM-DD): ");
        String phone = readEntry("Phone: ");
        String password = readEntry("Password: ");
        String address = readEntry("Address: ");

        String sql = "INSERT INTO Customer (SSN, First_Name, Last_Name, Gender, Date_of_Birth, Phone, Password, Address) VALUES (?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?)";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ssn);
            pstmt.setString(2, firstName);
            pstmt.setString(3, lastName);
            pstmt.setString(4, gender);
            pstmt.setString(5, dob);
            pstmt.setString(6, phone);
            pstmt.setString(7, password);
            pstmt.setString(8, address);
            pstmt.executeUpdate();
            View.displayMessage("Customer registered successfully.");
            return ssn;
        }
        catch(SQLException e) {
            View.displayError("Registration failed: " + e.getMessage());
            return null;
        }
    }

    private static void viewPrescriptions(Connection conn) throws SQLException {
        // For customer, use currentSSN; otherwise prompt
        String ssn;
        if("customer".equals(currentRole)) {
            ssn = currentSSN;
            View.displayMessage("Viewing prescriptions for SSN: " + ssn);
        }
        else {
            ssn = readEntry("Enter Customer SSN: ");
        }
        String sql = "SELECT p.Prescription_ID, p.Prescribed_Date, p.Note, d.First_Name || ' ' || d.Last_Name AS Doctor_Name " +
                "FROM Prescription p JOIN Doctor d ON p.Doctor_ID = d.Doctor_ID WHERE p.Customer_SSN = ?";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ssn);
            ResultSet rs = pstmt.executeQuery();
            View.displayMessage("\nPrescriptions:");
            ArrayList<String[]> rows = new ArrayList<>();
            while(rs.next()) {
                String dateStr = (rs.getDate(2) != null) ? rs.getDate(2).toString() : "N/A";
                String noteStr = rs.getString(3) != null ? rs.getString(3) : "N/A";
                String doctorName = rs.getString(4) != null ? rs.getString(4) : "N/A";
                rows.add(new String[]{
                        String.valueOf(rs.getInt(1)),
                        dateStr,
                        noteStr,
                        doctorName
                });
            }
            if(!rows.isEmpty()) {
                View.displayTable(new String[]{"ID", "Date", "Note", "Doctor"}, rows);
            }
            else {
                View.displayMessage("No prescriptions found.");
            }
        }
    }

    private static void placeOrder(Connection conn) throws SQLException {
        View.displayMessage("\n--- Place Order ---");
        String ssn;
        if("customer".equals(currentRole)) {
            ssn = currentSSN;
        }
        else {
            ssn = readEntry("Customer SSN: ");
        }
        int prescriptionId = Integer.parseInt(readEntry("Prescription ID: "));
        int employeeId = Integer.parseInt(readEntry("Employee ID: "));
        String orderDate = readEntry("Order Date (YYYY-MM-DD): ");
        String type = readEntry("Type (urgent/normal): ");
        String status = "incomplete";
        double totalAmount = 0.0;

        int orderId;
        try(Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT NVL(MAX(ORDER_ID), 0) + 1 FROM \"Order\"");
            if(rs.next()) {
                orderId = rs.getInt(1);
            }
            else {
                throw new SQLException("Unable to generate order ID");
            }
        }

        String insertOrder = "INSERT INTO \"Order\" (ORDER_ID, Order_Date, Total_Amount, Type, Status, Customer_SSN, Employee_ID, Prescription_ID) " +
                "VALUES (?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?, ?, ?, ?)";
        try(PreparedStatement pstmt = conn.prepareStatement(insertOrder)) {
            pstmt.setInt(1, orderId);
            pstmt.setString(2, orderDate);
            pstmt.setDouble(3, totalAmount);
            pstmt.setString(4, type);
            pstmt.setString(5, status);
            pstmt.setString(6, ssn);
            pstmt.setInt(7, employeeId);
            pstmt.setInt(8, prescriptionId);
            pstmt.executeUpdate();

            // Add drugs loop (simplified, add at least one for demo)
            String drugName = readEntry("Drug Name: ");
            int pharmacyId = Integer.parseInt(readEntry("Pharmacy ID: "));
            String batchNumber = readEntry("Batch Number: ");
            int quantity = Integer.parseInt(readEntry("Quantity: "));
            double price = Double.parseDouble(readEntry("Price: "));

            // Check stock (simplified)
            String checkStock = "SELECT Stock_Quantity FROM Medicine WHERE Drug_Name = ? AND Pharmacy_ID = ? AND Batch_Number = ?";
            try(PreparedStatement checkPstmt = conn.prepareStatement(checkStock)) {
                checkPstmt.setString(1, drugName);
                checkPstmt.setInt(2, pharmacyId);
                checkPstmt.setString(3, batchNumber);
                ResultSet rs = checkPstmt.executeQuery();
                if(rs.next() && rs.getInt(1) >= quantity) {
                    // Insert ordered drug
                    String insertOrdered = "INSERT INTO Ordered_Drugs (Order_ID, Drug_Name, Pharmacy_ID, Batch_Number, Ordered_Quantity, Price) VALUES (?, ?, ?, ?, ?, ?)";
                    try(PreparedStatement orderedPstmt = conn.prepareStatement(insertOrdered)) {
                        orderedPstmt.setInt(1, orderId);
                        orderedPstmt.setString(2, drugName);
                        orderedPstmt.setInt(3, pharmacyId);
                        orderedPstmt.setString(4, batchNumber);
                        orderedPstmt.setInt(5, quantity);
                        orderedPstmt.setDouble(6, price);
                        orderedPstmt.executeUpdate();
                    }
                    // Update stock
                    String updateStock = "UPDATE Medicine SET Stock_Quantity = Stock_Quantity - ? WHERE Drug_Name = ? AND Pharmacy_ID = ? AND Batch_Number = ?";
                    try(PreparedStatement updatePstmt = conn.prepareStatement(updateStock)) {
                        updatePstmt.setInt(1, quantity);
                        updatePstmt.setString(2, drugName);
                        updatePstmt.setInt(3, pharmacyId);
                        updatePstmt.setString(4, batchNumber);
                        updatePstmt.executeUpdate();
                    }
                    totalAmount += price * quantity;
                }
                else {
                    View.displayError("Insufficient stock.");
                }
            }
            // Update total
            String updateTotal = "UPDATE \"Order\" SET Total_Amount = ? WHERE Order_ID = ?";
            try(PreparedStatement totalPstmt = conn.prepareStatement(updateTotal)) {
                totalPstmt.setDouble(1, totalAmount);
                totalPstmt.setInt(2, orderId);
                totalPstmt.executeUpdate();
            }
            View.displayMessage("Order placed successfully.");
        }
    }

    private static void issuePrescription(Connection conn) throws SQLException {
        if(currentDoctorId == -1) {
            View.displayError("Doctor ID not set.");
            return;
        }
        View.displayMessage("\n--- Issue Prescription ---");
        String ssn = readEntry("Customer SSN: ");
        String prescribedDate = readEntry("Prescribed Date (YYYY-MM-DD): ");
        String note = readEntry("Note: ");

        int prescriptionId;
        try(Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT NVL(MAX(PRESCRIPTION_ID), 0) + 1 FROM Prescription");
            if(rs.next()) {
                prescriptionId = rs.getInt(1);
            }
            else {
                throw new SQLException("Unable to generate prescription ID");
            }
        }

        String insertPres = "INSERT INTO Prescription (PRESCRIPTION_ID, Prescribed_Date, Note, Customer_SSN, Doctor_ID) VALUES (?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?)";
        try(PreparedStatement pstmt = conn.prepareStatement(insertPres)) {
            pstmt.setInt(1, prescriptionId);
            pstmt.setString(2, prescribedDate);
            pstmt.setString(3, note);
            pstmt.setString(4, ssn);
            pstmt.setInt(5, currentDoctorId);
            pstmt.executeUpdate();

            // Add drugs (simplified, add at least one)
            String drugName = readEntry("Drug Name: ");
            int quantity = Integer.parseInt(readEntry("Quantity: "));
            int refillLimit = Integer.parseInt(readEntry("Refill Limit: "));

            String insertDrug = "INSERT INTO Prescribed_Drugs (Prescription_ID, Drug_Name, Prescribed_Quantity, Refill_Limit) VALUES (?, ?, ?, ?)";
            try(PreparedStatement drugPstmt = conn.prepareStatement(insertDrug)) {
                drugPstmt.setInt(1, prescriptionId);
                drugPstmt.setString(2, drugName);
                drugPstmt.setInt(3, quantity);
                drugPstmt.setInt(4, refillLimit);
                drugPstmt.executeUpdate();
            }
            View.displayMessage("Prescription issued successfully.");
        }
    }

    private static void processOrder(Connection conn) throws SQLException {
        if(currentEmployeeId == -1) {
            View.displayError("Employee ID not set.");
            return;
        }
        View.displayMessage("\n--- Process Order ---");
        View.displayMessage("Your managed incomplete orders:");

        String sql = "SELECT Order_ID, Order_Date, Total_Amount, Type, Customer_SSN, Prescription_ID " +
                "FROM \"Order\" WHERE Employee_ID = ? AND Status = 'incomplete'";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentEmployeeId);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String[]> rows = new ArrayList<>();
            while(rs.next()) {
                String dateStr = (rs.getDate(2) != null) ? rs.getDate(2).toString() : "N/A";
                rows.add(new String[]{
                        String.valueOf(rs.getInt(1)),
                        dateStr,
                        String.valueOf(rs.getDouble(3)),
                        rs.getString(4),
                        rs.getString(5),
                        String.valueOf(rs.getInt(6))
                });
            }
            if(!rows.isEmpty()) {
                View.displayTable(new String[]{"Order ID", "Date", "Total Amount", "Type", "Customer SSN", "Prescription ID"}, rows);
            }
            else {
                View.displayMessage("No incomplete orders found.");
                return;
            }
        }

        int orderId = Integer.parseInt(readEntry("Enter Order ID to process: "));

        // Check if the order belongs to this employee
        String checkSql = "SELECT 1 FROM \"Order\" WHERE Order_ID = ? AND Employee_ID = ? AND Status = 'incomplete'";
        try(PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
            checkPstmt.setInt(1, orderId);
            checkPstmt.setInt(2, currentEmployeeId);
            ResultSet rs = checkPstmt.executeQuery();
            if(rs.next()) {
                String update = "UPDATE \"Order\" SET Status = 'complete' WHERE Order_ID = ?";
                try(PreparedStatement updatePstmt = conn.prepareStatement(update)) {
                    updatePstmt.setInt(1, orderId);
                    int rowsUpdated = updatePstmt.executeUpdate();
                    if(rowsUpdated > 0) {
                        View.displayMessage("Order processed successfully.");
                    }
                    else {
                        View.displayError("Failed to process order.");
                    }
                }
            }
            else {
                View.displayError("You can only process your own incomplete orders.");
            }
        }
    }

    private static void monthlyRevenueReport(Connection conn) throws SQLException {
        String month = readEntry("Enter Month (YYYY-MM): ") + "-01";
        String sql = "SELECT SUM(Total_Amount) AS Revenue FROM \"Order\" WHERE Order_Date BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND ADD_MONTHS(TO_DATE(?, 'YYYY-MM-DD'), 1)";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, month);
            pstmt.setString(2, month);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) {
                View.displayMessage("Monthly Revenue: " + rs.getDouble(1));
            }
            else {
                View.displayMessage("No revenue data found.");
            }
        }
    }

    private static void annualRevenueReport(Connection conn) throws SQLException {
        String year = readEntry("Enter Year (YYYY): ");
        String startDate = year + "-01-01";
        String endDate = year + "-12-31";
        String sql = "SELECT SUM(Total_Amount) AS Revenue FROM \"Order\" WHERE Order_Date BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()) {
                View.displayMessage("Annual Revenue: " + rs.getDouble(1));
            }
            else {
                View.displayMessage("No revenue data found.");
            }
        }
    }

    private static void viewExpiredStock(Connection conn) throws SQLException {
        String sql = "SELECT Drug_Name, Pharmacy_ID, Batch_Number, Expiry_Date FROM Medicine WHERE Expiry_Date < SYSDATE";
        try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            View.displayMessage("\nExpired Stock:");
            ArrayList<String[]> rows = new ArrayList<>();
            while(rs.next()) {
                String expiryStr = (rs.getDate(4) != null) ? rs.getDate(4).toString() : "N/A";
                rows.add(new String[]{
                        rs.getString(1),
                        String.valueOf(rs.getInt(2)),
                        rs.getString(3),
                        expiryStr
                });
            }
            if(!rows.isEmpty()) {
                View.displayTable(new String[]{"Drug Name", "Pharmacy ID", "Batch Number", "Expiry Date"}, rows);
            }
            else {
                View.displayMessage("No expired stock found.");
            }
        }
    }

    private static void topSoldDrugsReport(Connection conn) throws SQLException {
        View.displayMessage("\n--- Top 5 Most Sold Drugs Report ---");
        String sql = "SELECT Drug_Name, SUM(Ordered_Quantity) AS Total_Sold " +
                "FROM Ordered_Drugs od JOIN \"Order\" o ON od.Order_ID = o.Order_ID " +
                "WHERE o.Status = 'complete' " +
                "GROUP BY Drug_Name " +
                "ORDER BY Total_Sold DESC " +
                "FETCH FIRST 5 ROWS ONLY";
        try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            ArrayList<String[]> rows = new ArrayList<>();
            while(rs.next()) {
                rows.add(new String[]{
                        rs.getString(1),
                        String.valueOf(rs.getInt(2))
                });
            }
            if(!rows.isEmpty()) {
                View.displayTable(new String[]{"Drug Name", "Total Sold"}, rows);
            }
            else {
                View.displayMessage("No data found.");
            }
        }
    }

    private static void topCustomersReport(Connection conn) throws SQLException {
        View.displayMessage("\n--- Top 5 Customers by Spending Report ---");
        String sql = "SELECT c.First_Name || ' ' || c.Last_Name AS Customer_Name, SUM(o.Total_Amount) AS Total_Spent " +
                "FROM \"Order\" o JOIN Customer c ON o.Customer_SSN = c.SSN " +
                "WHERE o.Status = 'complete' " +
                "GROUP BY c.SSN, c.First_Name, c.Last_Name " +
                "ORDER BY Total_Spent DESC " +
                "FETCH FIRST 5 ROWS ONLY";
        try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            ArrayList<String[]> rows = new ArrayList<>();
            while(rs.next()) {
                rows.add(new String[]{
                        rs.getString(1),
                        String.valueOf(rs.getDouble(2))
                });
            }
            if(!rows.isEmpty()) {
                View.displayTable(new String[]{"Customer Name", "Total Spent"}, rows);
            }
            else {
                View.displayMessage("No data found.");
            }
        }
    }

    private static void topDoctorsReport(Connection conn) throws SQLException {
        View.displayMessage("\n--- Top 5 Doctors by Prescriptions Report ---");
        String sql = "SELECT d.First_Name || ' ' || d.Last_Name AS Doctor_Name, COUNT(p.Prescription_ID) AS Prescription_Count " +
                "FROM Prescription p JOIN Doctor d ON p.Doctor_ID = d.Doctor_ID " +
                "GROUP BY d.Doctor_ID, d.First_Name, d.Last_Name " +
                "ORDER BY Prescription_Count DESC " +
                "FETCH FIRST 5 ROWS ONLY";
        try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            ArrayList<String[]> rows = new ArrayList<>();
            while(rs.next()) {
                rows.add(new String[]{
                        rs.getString(1),
                        String.valueOf(rs.getInt(2))
                });
            }
            if(!rows.isEmpty()) {
                View.displayTable(new String[]{"Doctor Name", "Prescription Count"}, rows);
            }
            else {
                View.displayMessage("No data found.");
            }
        }
    }

    private static void lowStockReport(Connection conn) throws SQLException {
        View.displayMessage("\n--- Low Stock Medicines Report ---");
        String sql = "SELECT Drug_Name, Pharmacy_ID, Batch_Number, Stock_Quantity " +
                "FROM Medicine " +
                "WHERE Stock_Quantity < 10 " +
                "ORDER BY Stock_Quantity ASC";
        try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            ArrayList<String[]> rows = new ArrayList<>();
            while(rs.next()) {
                rows.add(new String[]{
                        rs.getString(1),
                        String.valueOf(rs.getInt(2)),
                        rs.getString(3),
                        String.valueOf(rs.getInt(4))
                });
            }
            if(!rows.isEmpty()) {
                View.displayTable(new String[]{"Drug Name", "Pharmacy ID", "Batch Number", "Stock Quantity"}, rows);
            }
            else {
                View.displayMessage("No low stock found.");
            }
        }
    }

    // Utility method to read user input
    static String readEntry(String prompt) {
        try {
            View.displayPrompt(prompt);
            StringBuffer buffer = new StringBuffer();
            System.out.flush();
            int c = System.in.read();
            while(c != '\n' && c != -1) {
                buffer.append((char)c);
                c = System.in.read();
            }
            return buffer.toString().trim();
        }
        catch(IOException e) {
            return "";
        }
    }

    // Utility method to clear screen
    static void clearScreen() throws IOException, InterruptedException {
        if(System.getProperty("os.name").contains("Windows"))
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        else
            System.out.print("\033[H\033[2J");
    }

    // Helper method: Get role description
    private static String getRoleDesc(String role) {
        switch(role) {
            case "customer": return "customers";
            case "doctor": return "doctors";
            case "employee": return "employees";
            case "admin": return "administrators";
            default: return "unknown role";
        }
    }

    // View class integrated directly into the file for unified interface
    static class View {
        /**
         * Display options for selection
         */
        public static void displayOptions(String action, String options) {
            System.out.println("\n\nSelect " + action + ": ");
            System.out.print(options);
        }

        /**
         * Display prompt for input
         */
        public static void displayPrompt(String s) {
            System.out.print("\nEnter " + s + ":\n>>> ");
        }

        /**
         * Display bad input error (string received)
         */
        public static void displayBadInput(String expect, String received) {
            System.out.printf("Error: expects %s, but received %s\n", expect, received);
        }

        /**
         * Display bad input error (int received)
         */
        public static void displayBadInput(String expect, int received) {
            System.out.printf("Error: expects %s, but received %d\n", expect, received);
        }

        /**
         * Display exit message
         */
        public static void displayExit() {
            System.out.println("System exited gracefully.");
        }

        /**
         * Display error message
         */
        public static void displayError(String err) {
            System.out.println("Error: " + err);
        }

        /**
         * Display info message
         */
        public static void displayMessage(String msg) {
            System.out.println("Info: " + msg);
        }

        /**
         * Display formatted table
         */
        public static void displayTable(String[] header, ArrayList<String[]> rows){
            System.out.println();
            int[] mx = new int[header.length];
            for(int i = 0; i < header.length; i++) {
                mx[i] = header[i].length();
            }
            for(String[] row : rows) {
                for(int i = 0; i < row.length; i++) {
                    int len = (row[i] != null) ? row[i].length() : 0;
                    mx[i] = Math.max(mx[i], len);
                }
            }
            for(int i = 0; i < header.length; i++) {
                System.out.printf("%-" + mx[i] + "s  ", header[i]);
            }
            System.out.println();
            for(String[] row : rows) {
                for(int i = 0; i < row.length; i++) {
                    String val = (row[i] != null) ? row[i] : "";
                    System.out.printf("%-" + mx[i] + "s  ", val);
                }
                System.out.println();
            }
        }
    }
}