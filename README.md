# Media Notes App 📱📝

An Android application to create, manage, and review media-enabled notes with local persistence, periodic reminders, and sensor-driven actions.

## ✨ Highlights
- 📝 Create and edit notes with title, description, category, and favorite state
- 📷 Attach media using Camera or Gallery
- 🗂️ Store note data locally using SQLite (personalized schema)
- 🔎 Search and filter notes by categories
- ⭐ Manage favorite notes separately
- 📊 View activity analytics (weekly chart, streak, and date-wise grouping)
- 🔔 Receive periodic WorkManager-based reminder notifications
- 📳 Use accelerometer shake gesture to clear all recorded notes (demo/assignment feature)

## 🧱 Tech Stack
| Layer | Technology |
|---|---|
| Language | Java |
| UI | Android Views, Material Components |
| Local Storage | SQLiteOpenHelper + Repository pattern |
| Lists | RecyclerView |
| Background Tasks | WorkManager |
| Media Rendering | Glide |
| Auth | Firebase Authentication (Email/Password + Google Sign-In) |
| Charts | MPAndroidChart |

## 🏗️ Project Configuration
| Property | Value |
|---|---|
| Application ID | com.hiten.medianotesapp |
| compileSdk | 36 |
| targetSdk | 36 |
| minSdk | 24 |
| Java | 21 |
| Database Name | NotesDB_8 |
| Table Name | notes_8 |

## 🗄️ Database Schema
Main table: notes_8

| Column | Type | Constraint | Description |
|---|---|---|---|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique row ID |
| title | TEXT | NOT NULL | Note title |
| description | TEXT | NULL | Note body text |
| image_path | TEXT | NULL | Local path/URI of selected media |
| note_type | TEXT | NULL | Category/personalization field |
| user_id | TEXT | NOT NULL | Auth user mapping |
| date | INTEGER | NULL | Timestamp in millis |
| is_favorite | INTEGER | DEFAULT 0 | Favorite marker (0/1) |
| is_done | INTEGER | DEFAULT 0 | Completion marker (0/1) |

## 📂 Key Functional Modules
- Authentication
	- Email/password login and signup
	- Google Sign-In integration
- Notes CRUD
	- Create, edit, view detail, delete
	- Category selection and favorites
- Media pipeline
	- Capture image via camera
	- Select image from gallery
	- Persist path reference, render with Glide
- Dashboard tabs
	- Home: searchable/filterable notes
	- Favorites: favorite note list
	- Activity: chart + calendar + note activity logs
	- Settings: theme toggle, clear notes, export report, logout
- Background reminder system
	- Periodic WorkManager worker
	- Notification channel + runtime notification permission handling
- Sensor integration
	- Shake detection using accelerometer G-force
	- Clears all recorded notes for current user and refreshes UI

## 🔔 Notification Logic
The notification message follows roll-number personalization:

| Roll No % 3 | Notification Message |
|---|---|
| 0 | Review your saved notes today |
| 1 | Time to read your notes |
| 2 | Check your notes and stay prepared |

For this project (Roll No 8):
- 8 % 3 = 2
- Message used: Check your notes and stay prepared

## 📳 Accelerometer Behavior
- Sensor: TYPE_ACCELEROMETER
- Logic: normalized G-force threshold + cooldown
- Trigger action: clears all recorded notes for active user
- Feedback: toast message + immediate list refresh

Important:
- This gesture is destructive by design (assignment requirement implementation).
- Use carefully on real devices.

## 🖼️ Suggested Demo Screenshots
You can include these in reports/presentations:
1. Home screen with search and filters
2. Add note form with capture/select media buttons
3. Notes list with media thumbnails
4. Activity screen with chart/calendar
5. Notification in status panel
6. Shake action result (notes cleared + toast)

## ⚙️ Setup Instructions

### 1) Prerequisites
- Android Studio (latest stable recommended)
- JDK 21
- Android SDK platform matching compileSdk 36
- Emulator or physical Android device

### 2) Clone and open
Run in terminal:

```bash
git clone <YOUR_REPOSITORY_URL>
cd MediaNotesApp5
```

Then open the project in Android Studio.

### 3) Firebase setup
- Ensure app/google-services.json is present
- Verify package name in Firebase matches com.hiten.medianotesapp

### 4) Build and run
From project root:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

On Windows PowerShell:

```powershell
.\gradlew :app:assembleDebug
.\gradlew :app:testDebugUnitTest
```

## 🧪 Quick Demo Flow
1. Login
2. Add 2-3 notes with images
3. Open Home and verify list rendering
4. Open Activity and show analytics
5. Wait for reminder notification (periodic worker)
6. Shake device (or simulate accelerometer in emulator) to clear notes

## 📱 Permissions Used
- INTERNET
- ACCESS_NETWORK_STATE
- CAMERA
- READ_MEDIA_IMAGES (Android 13+)
- READ_EXTERNAL_STORAGE (below Android 13)
- POST_NOTIFICATIONS

## 📌 Notes for Evaluator
- Architecture emphasizes simple maintainability with repository-driven local DB access.
- UI updates are callback-driven to keep list screens synchronized after data operations.
- WorkManager uses unique periodic scheduling to prevent duplicate reminder chains.

## 👨‍💻 Author
Hiten Jitender Bhurani

## 🌟 Future Improvements
- Add undo confirmation for shake-clear action
- Add cloud sync and backup option
- Add note tagging and advanced filters
- Add in-app analytics dashboard export