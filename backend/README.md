
CareConnect Backend – Implementation Summary
1. Overview
This branch contains the backend implementation for CareConnect, a student mental health and counselling support platform for the University of Lusaka (UNILUS).
The backend provides APIs for student registration, counsellor registration and approval, counsellor availability, appointment booking, and appointment management. It also includes authentication and security configurations to control access to protected endpoints.
2. Project Structure
The backend is organised into the following packages:
DTO (Data Transfer Objects)
Contains classes used to transfer data between API requests and responses. These classes define the information expected when users perform actions such as registering an account.
Examples include:
	•	StudentRegistrationRequest
	•	CounselorRegistrationRequest
Entity
Contains the Java classes representing the main data models stored in the database.
These include models related to:
	•	Student identity and account linking
	•	Counsellor profiles
	•	Appointments
	•	Counsellor availability
Repository
Contains the database access layer. Repositories allow the application to retrieve, save, and update records in the PostgreSQL database.
Service
Contains the application’s business logic. Services handle operations such as registration, counsellor approval, availability management, and appointment processing.
Admin
Contains administrative functionality for managing counsellor registrations and approvals.
Controllers
Contains the REST API endpoints through which the frontend communicates with the backend.
The implemented controllers cover functionality such as:
	•	Student registration and related operations
	•	Counsellor registration and directory access
	•	Counsellor availability
	•	Appointment booking and management
	•	Administrative counsellor approval
	•	Health checks
SecurityConfig
Configures backend security, including authentication and access restrictions for protected API endpoints.
Application Configuration
The application.properties file contains Spring Boot configuration, including database connectivity and JPA/Hibernate settings.
3. Implemented Features
Student Registration
	•	Implemented the student registration endpoint.
	•	Added request handling and service logic for student registration.
	•	Connected registration functionality to the database.
Counsellor Registration and Approval
	•	Implemented counsellor registration.
	•	Added an approval workflow for counsellor accounts.
	•	Enabled administrative approval of pending counsellor registrations.
Counsellor Availability
	•	Implemented endpoints for retrieving and managing counsellor availability.
	•	Added support for defining available days and time slots.
Appointment Management
	•	Implemented appointment booking for students.
	•	Added functionality to retrieve appointments.
	•	Added appointment status updates, including confirmation and cancellation.
Counsellor Directory
	•	Implemented functionality for retrieving counsellor information through protected API endpoints.
Security and Authentication
	•	Configured security for the backend API.
	•	Restricted access to protected endpoints.
	•	Tested access using authenticated requests.
Health Check
	•	Implemented a health endpoint to verify that the backend API is responding.
4. Testing and Verification
The implemented functionality was tested using API requests and database-backed operations.
The following were verified:
	•	Successful student registration
	•	Successful counsellor registration
	•	Administrative approval of a counsellor
	•	Creation and retrieval of counsellor availability
	•	Retrieval of counsellor directory information with authentication
	•	Successful appointment booking
	•	Retrieval of appointments
	•	Appointment confirmation and cancellation
	•	Successful response from the health endpoint
	•	Successful Maven compilation
5. Technology Stack
	•	Language: Java
	•	Framework: Spring Boot
	•	Database: PostgreSQL
	•	ORM: Spring Data JPA / Hibernate
	•	Build Tool: Maven
	•	API Style: REST
	•	Data Format: JSON
6. Notes for Review
This branch contains the backend implementation for the features described above. The frontend can integrate with the available REST endpoints to connect the user interface to the backend.
Please review the implementation and verify the API contracts, security rules, database mappings, and integration requirements before merging into main.
Branch: feature/student-registration
