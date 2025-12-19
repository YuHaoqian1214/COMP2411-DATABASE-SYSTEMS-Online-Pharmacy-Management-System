CREATE TABLE Customer(
    SSN VARCHAR2(20) PRIMARY KEY,
    First_Name VARCHAR2(50) NOT NULL,
    Last_Name VARCHAR2(50) NOT NULL,
    Gender VARCHAR2(10) NOT NULL CHECK (Gender IN ('male', 'female')),
    Date_of_Birth DATE NOT NULL,
    Phone VARCHAR2(15) NOT NULL,
    Password VARCHAR2(15) NOT NULL,
    Address VARCHAR2(100) NOT NULL
);

CREATE TABLE Doctor(
    Doctor_ID NUMBER PRIMARY KEY,
    First_Name VARCHAR2(50) NOT NULL,
    Last_Name VARCHAR2(50) NOT NULL,
    Specialty VARCHAR2(50) NOT NULL,
    Phone VARCHAR2(15) NOT NULL
);

CREATE TABLE Drug(
    Drug_Name VARCHAR2(100) PRIMARY KEY,
    Price NUMBER(10, 2) NOT NULL CHECK (Price >= 0),
    Medicine_Type VARCHAR2(50) NOT NULL
);

CREATE TABLE Pharmacy(
    Pharmacy_ID NUMBER PRIMARY KEY,
    Name VARCHAR2(50) NOT NULL,
    Address VARCHAR2(100) NOT NULL,
    Phone VARCHAR2(15) NOT NULL
);

CREATE TABLE Employee(
    Employee_ID NUMBER PRIMARY KEY,
    First_Name VARCHAR2(50) NOT NULL,
    Last_Name VARCHAR2(50) NOT NULL,
    Gender VARCHAR2(10) NOT NULL CHECK (Gender IN ('male', 'female')),
    Date_of_Birth DATE NOT NULL,
    Salary NUMBER(10, 2) NOT NULL CHECK (Salary >= 0),
    Phone VARCHAR2(15) NOT NULL,
    Pharmacy_ID NUMBER NOT NULL,
    CONSTRAINT FK_Employee_Pharmacy FOREIGN KEY (Pharmacy_ID) REFERENCES Pharmacy(Pharmacy_ID) ON DELETE CASCADE
);

CREATE TABLE Prescription(
    Prescription_ID NUMBER PRIMARY KEY,
    Prescribed_Date DATE NOT NULL,
    Note VARCHAR2(4000),
    Customer_SSN VARCHAR2(20) NOT NULL,
    Doctor_ID NUMBER NOT NULL,
    CONSTRAINT FK_Prescription_Customer FOREIGN KEY (Customer_SSN) REFERENCES Customer(SSN) ON DELETE CASCADE,
    CONSTRAINT FK_Prescription_Doctor FOREIGN KEY (Doctor_ID) REFERENCES Doctor(Doctor_ID) ON DELETE CASCADE
);

CREATE TABLE Insurance(
    Insurance_ID NUMBER PRIMARY KEY,
    Company_Name VARCHAR2(50) NOT NULL,
    Start_Date DATE NOT NULL,
    End_Date DATE NOT NULL,
    SSN VARCHAR2(20) NOT NULL UNIQUE,
    CONSTRAINT FK_Insurance_Customer FOREIGN KEY (SSN) REFERENCES Customer(SSN) ON DELETE CASCADE
);

CREATE TABLE Medicine(
    Drug_Name VARCHAR2(100) NOT NULL,
    Pharmacy_ID NUMBER NOT NULL,
    Batch_Number VARCHAR2(20) NOT NULL,
    Stock_Quantity NUMBER NOT NULL CHECK (Stock_Quantity >= 0),
    Expiry_Date DATE NOT NULL,
    PRIMARY KEY (Drug_Name, Pharmacy_ID, Batch_Number),
    CONSTRAINT FK_Medicine_Drug FOREIGN KEY (Drug_Name) REFERENCES Drug(Drug_Name) ON DELETE CASCADE,
    CONSTRAINT FK_Medicine_Pharmacy FOREIGN KEY (Pharmacy_ID) REFERENCES Pharmacy(Pharmacy_ID) ON DELETE CASCADE
);

CREATE TABLE "Order"(
    Order_ID NUMBER PRIMARY KEY,
    Order_Date DATE NOT NULL,
    Total_Amount NUMBER(10, 2) NOT NULL CHECK (Total_Amount >= 0),
    Type VARCHAR2(20) NOT NULL CHECK (Type IN ('urgent', 'normal')),
    Status VARCHAR2(20) NOT NULL CHECK (Status IN ('complete', 'incomplete')),
    Customer_SSN VARCHAR2(20) NOT NULL,
    Employee_ID NUMBER NOT NULL,
    Prescription_ID NUMBER NOT NULL,
    CONSTRAINT FK_Order_Customer FOREIGN KEY (Customer_SSN) REFERENCES Customer(SSN) ON DELETE CASCADE,
    CONSTRAINT FK_Order_Employee FOREIGN KEY (Employee_ID) REFERENCES Employee(Employee_ID) ON DELETE CASCADE,
    CONSTRAINT FK_Order_Prescription FOREIGN KEY (Prescription_ID) REFERENCES Prescription(Prescription_ID) ON DELETE CASCADE
);

CREATE TABLE Prescribed_Drugs(
    Prescription_ID NUMBER NOT NULL,
    Drug_Name VARCHAR2(100) NOT NULL,
    Prescribed_Quantity NUMBER NOT NULL CHECK (Prescribed_Quantity > 0),
    Refill_Limit NUMBER NOT NULL CHECK (Refill_Limit >= 0),
    PRIMARY KEY (Prescription_ID, Drug_Name),
    CONSTRAINT FK_Prescribed_Prescription FOREIGN KEY (Prescription_ID) REFERENCES Prescription(Prescription_ID) ON DELETE CASCADE,
    CONSTRAINT FK_Prescribed_Drug FOREIGN KEY (Drug_Name) REFERENCES Drug(Drug_Name) ON DELETE CASCADE
);

CREATE TABLE Ordered_Drugs(
    Order_ID NUMBER NOT NULL,
    Drug_Name VARCHAR2(100) NOT NULL,
    Pharmacy_ID NUMBER NOT NULL,
    Batch_Number VARCHAR2(20) NOT NULL,
    Ordered_Quantity NUMBER NOT NULL CHECK (Ordered_Quantity >= 0),
    Price NUMBER(10, 2) NOT NULL CHECK (Price >= 0),
    PRIMARY KEY (Order_ID, Drug_Name, Pharmacy_ID, Batch_Number),
    CONSTRAINT FK_Ordered_Order FOREIGN KEY (Order_ID) REFERENCES "Order"(Order_ID) ON DELETE CASCADE,
    CONSTRAINT FK_Ordered_Medicine FOREIGN KEY (Drug_Name, Pharmacy_ID, Batch_Number) REFERENCES Medicine(Drug_Name, Pharmacy_ID, Batch_Number) ON DELETE CASCADE
);