
CareConnect
Connecting UNILUS Students to Professional Counselling Support
About the Project
CareConnect is a student mental health and counselling support platform designed for the University of Lusaka (UNILUS). The platform aims to make professional counselling services more accessible to students by providing a centralised, secure, and convenient way to access wellbeing resources and connect with counsellors.
Seeking support can sometimes feel difficult when students are unsure where to find appropriate services or how to arrange an appointment. CareConnect aims to reduce these barriers by bringing essential mental health resources and counselling services together in one platform.
CareConnect is designed to support student wellbeing by facilitating access to professional support while respecting users’ privacy and confidentiality.
Problem Statement
University students may experience academic pressure, stress, personal difficulties, and other challenges that affect their wellbeing. However, accessing counselling support may be difficult when information about available services is scattered, appointment arrangements are inconvenient, or students are uncertain about where to begin.
CareConnect seeks to address these challenges by providing a centralised platform through which students can explore wellbeing resources, identify available counsellors, and arrange counselling appointments.
Project Objectives
The main objectives of CareConnect are to:
	•	Improve students’ access to professional counselling services.
	•	Provide a centralised source of mental health and wellbeing information.
	•	Simplify the process of finding counsellors and booking appointments.
	•	Support communication between students and authorised counselling professionals.
	•	Promote awareness of student mental health and wellbeing.
	•	Encourage students to seek appropriate support when they need it.
Target Users
CareConnect is designed for three primary user groups.
1. Students
Students can access wellbeing resources, explore counselling support options, and use the platform’s available appointment services.
2. Counsellors
Counsellors can manage their availability and appointments through the platform, subject to the permissions and features provided by the system.
3. Administrators
Administrators oversee relevant platform operations, including reviewing and approving counsellor registrations.
Key Features
The project is designed around the following core features. Availability may depend on the current stage of implementation.
Student Registration
Allows students to register for an account and access the platform’s student services.
Counsellor Registration and Approval
Supports counsellor registration and an administrative approval process to help ensure that counsellor accounts are reviewed before approval.
Wellbeing Resources
Provides a centralised space for educational materials and information intended to promote mental health awareness and student wellbeing.
Counsellor Directory
Allows users with the appropriate permissions to retrieve information about available counsellors.
Counsellor Availability
Supports the management and retrieval of counsellor availability so that appointment scheduling can be coordinated.
Appointment Booking and Management
Allows students to request counselling appointments and supports appointment retrieval, confirmation, and cancellation.
Secure Communication and Privacy
The platform is intended to protect users’ information through appropriate authentication, access controls, and secure handling of sensitive data.
Wellbeing Check-ins
The planned wellbeing check-in functionality is intended to help students reflect on their wellbeing and identify when they may wish to seek further support. It is not intended to diagnose mental health conditions.
System Architecture
CareConnect follows a client-server architecture in which the frontend communicates with the backend through REST APIs.
The system consists of three main components:
	•	Frontend: Provides the user interface through which students, counsellors, and administrators interact with the platform.
	•	Backend: Handles application logic, API requests, authentication, registration, appointment operations, and other supported services.
	•	Database: Stores the application’s persistent data, including relevant user, counsellor, and appointment records.
The backend exposes API endpoints that allow the frontend to interact with the application’s services.
Technology Stack
The current backend implementation uses the following technologies:
Technology	Purpose
Java	Backend programming language
Spring Boot	Backend application framework
Spring Data JPA	Database access and persistence
Hibernate	Object-relational mapping
PostgreSQL	Relational database
Maven	Dependency management and build automation
REST APIs	Communication between the frontend and backend
JSON	API request and response data format
The frontend technology stack should be documented here once the team’s final implementation is confirmed.
Backend Overview
The backend is organised into separate components to support maintainability and separation of responsibilities.
	•	DTO: Defines data transfer objects used for API requests and other data exchange.
	•	Entity: Contains the application’s data models.
	•	Repository: Provides database access operations.
	•	Service: Implements application business logic.
	•	Admin: Contains administrative functionality.
	•	Controllers: Expose REST API endpoints.
	•	Security Configuration: Defines authentication and access-control rules.
The backend currently includes functionality for student registration, counsellor registration and approval, counsellor availability, appointment management, counsellor directory access, and health checks.
Security and Privacy
Because CareConnect handles sensitive wellbeing-related information, privacy and appropriate access control are important considerations in its development.
The system’s security approach includes authentication and restrictions on protected API endpoints. Further security measures must be implemented and verified as the platform develops.
CareConnect is intended to facilitate access to professional support. It is not designed to replace professional mental health assessment, diagnosis, or treatment.
Project Status
CareConnect is under active development.
Backend registration, counsellor approval, availability management, appointment operations, and related API functionality have been implemented and tested at the development stage.
Frontend integration, complete end-to-end testing, deployment, and the verification of all planned features should be tracked as the project progresses.
Getting Started
Development setup instructions will be expanded as the frontend and backend environments are finalised.
The backend is built with Java and Spring Boot, uses Maven, and connects to a PostgreSQL database.
Developers should configure their local database credentials and application environment before running the backend. Sensitive credentials should be stored outside committed source files and must not be published in the repository.
Future Development
Potential areas for continued development include:
	•	Completing frontend and backend integration.
	•	Expanding and verifying wellbeing resource functionality.
	•	Completing the planned wellbeing check-in experience.
	•	Improving validation and error handling.
	•	Strengthening security and privacy protections.
	•	Conducting end-to-end testing.
	•	Preparing the application for deployment.
Project Scope
CareConnect is intended to improve access to student wellbeing resources and professional counselling support within the UNILUS context.
The platform facilitates access to support but does not provide mental health diagnoses. Its role is to help students find relevant resources and connect with appropriate counselling services.
Acknowledgements
Developed as a collaborative university project at the University of Lusaka (UNILUS), with the aim of supporting student wellbeing through accessible digital services.
