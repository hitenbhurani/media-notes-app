# Assignment 2: Media Notes App with Sensor and Notification

## 1. Objective
The objective of this project was to design and implement an Android-based Media Notes App that allows structured note creation with media attachments and persistent local storage. The application was developed to capture note title and description, associate media references, and store data in a personalized SQLite database. In addition, hardware sensor integration was implemented through the accelerometer for shake-triggered data management, and background processing was implemented using WorkManager to generate periodic reminder notifications.

[INSERT SCREENSHOT 1 HERE]

## 2. Database Structure
The local persistence layer was implemented using SQLiteOpenHelper, while centralized database access was maintained through a Singleton repository pattern. This design ensured that database operations remained consistent, thread-safe at the repository level, and reusable across activities and fragments.

### 2.1 Database Configuration

| Property | Value |
|---|---|
| Roll Number | 8 |
| Database Name | NotesDB_8 |
| Table Name | notes_8 |
| Extra Field Rule | 8 % 4 = 0 |
| Selected Extra Field | note_type |

### 2.2 Table Schema

| Column | Type | Constraint | Description |
|---|---|---|---|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique identifier for each note record |
| title | TEXT | NOT NULL | Title entered by the user |
| description | TEXT | NULL allowed | Detailed note content |
| image_path | TEXT | NULL allowed | URI/path of captured or selected media |
| date | INTEGER | NULL allowed | Timestamp representing note creation/edit time |
| note_type | TEXT | NULL allowed | Personalized extra field for note classification |

The core table creation query used in the implementation is shown below.

    String create = "CREATE TABLE IF NOT EXISTS notes_8 ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "title TEXT NOT NULL,"
            + "description TEXT,"
            + "note_type TEXT,"
            + "image_path TEXT,"
            + "user_id TEXT NOT NULL,"
            + "date INTEGER,"
            + "is_favorite INTEGER DEFAULT 0,"
            + "is_done INTEGER DEFAULT 0"
            + ")";

[INSERT SCREENSHOT 2 HERE]

## 3. Media Handling Implementation
Media handling was implemented using standard Android intents for camera capture and gallery selection. When media was selected, only the media reference (image_path URI/local path) was persisted in SQLite instead of raw binary content, reducing database overhead and improving scalability. During list rendering, Glide was used in RecyclerView adapters to asynchronously decode and display images with fallback placeholders, resulting in smooth scrolling and controlled memory usage.

[INSERT SCREENSHOT 3 HERE]

## 4. Background Task & Notification Implementation
A periodic background task was implemented using WorkManager with a unique periodic work request. The worker checks saved note availability and triggers NotificationManager-based reminders through a dedicated notification channel. Runtime notification permission handling was integrated for Android 13+ devices, and the reminder worker was scheduled during dashboard initialization.

### 4.1 Notification Message Rule

| Rule Item | Calculation/Value |
|---|---|
| Roll Number | 8 |
| Formula | 8 % 3 |
| Result | 2 |
| Notification Message | Check your notes and stay prepared |

[INSERT SCREENSHOT 4 HERE]

## 5. Accelerometer Sensor Integration
Accelerometer support was integrated through SensorManager and SensorEventListener in the Home screen layer. Device shake was detected using normalized acceleration magnitude ($g$-force), and a threshold-based trigger was applied with a cooldown guard to avoid repeated execution. On shake detection, all saved notes for the current user were deleted through the repository layer, the RecyclerView dataset was refreshed, and a toast confirmation was displayed. This produced a clear and verifiable sensor-driven action in the application workflow.

[INSERT SCREENSHOT 5 HERE]

## 6. Results

| # | Feature | Status | Observation |
|---|---|---|---|
| 1 | User Interface | Implemented | Form validation, category selection, media action buttons, and bottom navigation transitions were verified across note create, edit, and list workflows without layout breakage. |
| 2 | SQLite Database | Implemented | The personalized schema (NotesDB_8 / notes_8) was validated with consistent insert, read, update, and delete behavior, and repository-layer callbacks produced stable UI synchronization. |
| 3 | Media Handling | Implemented | Camera/gallery outputs were stored as URI or local path references, and Glide-based rendering in RecyclerView maintained smooth scrolling with correct placeholder-to-image fallback behavior. |
| 4 | Accelerometer | Implemented | G-force threshold logic reliably detected shake events, invoked complete note-clear operations for the active user, and refreshed the visible dataset instantly with toast-based execution confirmation. |
| 5 | WorkManager & Notification | Implemented | Periodic background work was enqueued successfully, runtime notification permission handling (Android 13+) was respected, and rule-based reminder messages were delivered through notification channel infrastructure. |

[INSERT SCREENSHOT 6 HERE]

## 7. Conclusion
The Media Notes App demonstrates integrated Android development across data persistence, hardware interaction, and background processing. SQLiteOpenHelper with repository encapsulation established a reliable local database architecture, SensorManager-based accelerometer integration enabled contextual device-aware behavior, and WorkManager-driven scheduling provided robust periodic reminders through the Android notification framework. Overall, the implementation reflects practical competency in Android storage engineering, sensor APIs, asynchronous task orchestration, and production-style feature integration.

GitHub Repository Link: [INSERT GITHUB LINK HERE]
