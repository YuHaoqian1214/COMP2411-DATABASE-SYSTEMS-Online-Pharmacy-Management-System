# Online Pharmacy Management System

## Description
This repository contains the source code, ER diagram, relational schema, and user manual for the **Online Pharmacy Management System (OPMS)**, developed as part of the COMP2411 Database Systems group project at The Hong Kong Polytechnic University (PolyU HK) in 2025. The project simulates a real-world database system for an online pharmacy platform, managing customer orders, prescriptions, inventory, and employee operations. It demonstrates database design principles, including ER modeling, relational schema, business rules, and implementation with SQL scripts and a simple application interface.

**Note:** This system does not include real-time payment processing, delivery logistics, or advanced authentication. It is designed for educational purposes and uses synthetic data.

## Features
- **Customer Management:** Registration with personal details (SSN, name, gender, DOB, phone, address, password) and optional insurance linkage.
- **Doctor and Prescription Management:** Doctor profiles and prescription issuance with details like date, notes, drugs, quantities, and refill limits.
- **Pharmacy and Employee Operations:** Pharmacy setup, employee assignments with salaries, and order processing.
- **Inventory and Drug Management:** Drug catalog (name, price, type) and medicine batches with stock quantity, expiry date, and pharmacy association.
- **Order Processing:** Order creation based on prescriptions, including multiple drugs/batches, total amount calculation, type (urgent/normal), status (complete/incomplete), and associative tracking.
- **Business Rules Enforcement:** Unique keys, referential integrity with cascading deletes, stock checks (via application logic), and constraints like gender checks.
- **Reporting and Queries:** Sample reports for popular drugs, monthly revenue, low-stock alerts, and operational metrics.
- **Data Integrity:** CHECK constraints for positive values, dates, and enums.
- **User Roles Support:** Data needs for customers (order history), doctors (patient prescriptions), employees (inventory/orders), and managers (reports).

## Setup
### Requirements
- Database: Ojdbc8 (Oracle).
- Programming Language: Java.
- Tools: IntelliJ IDEA for code.

## Usage
For detailed instructions, examples, and screenshots, refer to the User Manual section below or `user_manual.pdf`.

## License
This project is for educational purposes only and is not licensed for commercial use. All rights reserved by Group 20 and PolyU HK. If reusing code or designs, please cite the source appropriately.
