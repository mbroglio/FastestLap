# FastestLap 🏎️

<div align="center">

![FastestLap Logo](Screenshots/Home%20with%20Results%20and%20Preferences.png)

**Your Ultimate Formula 1 Companion App**

[![Version](https://img.shields.io/badge/version-1.0-blue.svg)](https://github.com/mbroglio/fastestlap)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/license-MIT-orange.svg)](LICENSE)

[Features](#features) • [Screenshots](#screenshots) • [Installation](#installation) • [Architecture](#architecture) • [Contributing](#contributing)

</div>

---

## 📖 About

**FastestLap** is a comprehensive Android application designed for Formula 1 enthusiasts. Stay up-to-date with real-time race results, driver and constructor standings, upcoming events, breaking F1 news, and insights into junior categories (Formula 2 & Formula 3).

Built with modern Android development practices, FastestLap provides an intuitive and engaging user experience for following your favorite drivers, teams, and races throughout the season.

### 👥 Team: The Coffee Coders

- **Broglio Matteo**
- **Caputo Lorenzo**
- **Gargioli Federico**
- **Groppo Gabriele**
- **Lanticina Riccardo**

---

## ✨ Features

### 🏁 Race Information
- **Live Race Results** - Get up-to-the-minute results from qualifying, sprint, and race sessions
- **Event Schedule** - View complete weekend schedules with session times
- **Countdown Timers** - Never miss a race with live countdown to upcoming events
- **Past Events** - Browse historical race results and standings

### 🏆 Standings & Statistics
- **Driver Standings** - Real-time driver championship rankings
- **Constructor Standings** - Team championship leaderboard
- **Historical Data** - Access to complete season statistics
- **Detailed Bio Pages** - In-depth profiles for drivers, constructors, and circuits

### 📰 News & Updates
- **Multi-Source News Feed** - Aggregated F1 news from:
  - Autosport
  - Crash.net
  - Motorsport
- **English & Italian Sources** - Multilingual news coverage
- **RSS Integration** - Real-time news updates

### 🎯 Junior Categories
- **Formula 2 Support** - Results and standings for F2
- **Formula 3 Support** - Results and standings for F3
- **Driver Progression** - Track drivers as they move through categories

### 👤 User Features
- **Personal Preferences** - Set favorite drivers and teams
- **Auto-Login** - Seamless authentication with Firebase
- **Google Sign-In** - Quick OAuth integration
- **Profile Management** - Customize your experience

### 🌍 Localization
- **Multi-Language Support** - Available in:
  - English (en-GB)
  - Italian (it)
- **Regional Formatting** - Date, time, and number formats based on locale

### 🎨 User Experience
- **Dark Mode** - Full dark theme support
- **Material Design** - Modern, intuitive interface
- **Responsive Layouts** - Optimized for various screen sizes
- **Smooth Animations** - Polished transitions and interactions

---

## 📱 Screenshots

<div align="center">

| Home Screen | Standings | Driver Bio |
|------------|-----------|-----------|
| ![Home](Screenshots/Home%20with%20Results%20and%20Preferences.png) | ![Standings](Screenshots/Drivers%20Standing.png) | ![Driver Bio](Screenshots/Driver%20Bio.png) |

| Event Details | Profile | Calendar |
|--------------|---------|----------|
| ![Event](Screenshots/Upcoming%20Event%20Page.png) | ![Profile](Screenshots/Profile%20Page.png) | ![Calendar](Screenshots/Calendar%20Fragment.png) |

</div>

<details>
<summary>View More Screenshots</summary>

- [Login Screen](Screenshots/Login.png)
- [Sign Up](Screenshots/Sign%20Up.png)
- [Forgot Password](Screenshots/Forgot%20Password.png)
- [Loading Screen](Screenshots/Loading%20Screen.png)
- [Team Bio](Screenshots/Team%20Bio.png)
- [Teams Standing](Screenshots/Teams%20Standing.png)
- [Event Final Results](Screenshots/Event%20Final%20Results.png)
- [Finished Event Page](Screenshots/Finished%20Event%20Page.png)
- [Past Races](Screenshots/Past%20Races.png)
- [Upcoming Races](Screenshots/Upcoming%20Races.png)
- [Home with No Preferences](Screenshots/Home%20with%20No%20Preferences.png)
- [Home with Updating Results](Screenshots/Home%20with%20Updating%20Results.png)

</details>

---

## 🚀 Installation

### Prerequisites

- **Android Studio** Arctic Fox (2020.3.1) or later
- **JDK** 11 or higher
- **Android SDK** API 26+ (Android 8.0 Oreo)
- **Firebase Account** for authentication and database features

### Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/fastestlap.git
   cd fastestlap
   ```

2. **Configure Firebase**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Download `google-services.json` and place it in `app/` directory
   - Enable Firebase Authentication (Email/Password and Google Sign-In)
   - Set up Firebase Realtime Database
   - Configure database rules appropriately

3. **Configure Local Properties** (if needed)
   ```properties
   # local.properties
   sdk.dir=YOUR_ANDROID_SDK_PATH
   ```

4. **Build the Project**
   ```bash
   ./gradlew build
   ```
   Or on Windows:
   ```cmd
   gradlew.bat build
   ```

5. **Run the App**
   - Open the project in Android Studio
   - Sync Gradle files
   - Run on an emulator or physical device (API 24+)

---

## 🏗️ Architecture

FastestLap follows **Clean Architecture** principles with a clear separation of concerns.

### Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/the_coffe_coders/fastestlap/
│   │   │   ├── adapter/          # RecyclerView Adapters
│   │   │   ├── api/              # API Response Models
│   │   │   ├── database/         # Room Database
│   │   │   ├── domain/           # Business Models
│   │   │   │   ├── driver/
│   │   │   │   ├── constructor/
│   │   │   │   ├── grand_prix/
│   │   │   │   ├── news/
│   │   │   │   └── user/
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── mapper/           # Data Mappers
│   │   │   ├── repository/       # Repository Layer
│   │   │   ├── service/          # API Services
│   │   │   ├── source/           # Data Sources
│   │   │   │   ├── driver/
│   │   │   │   ├── constructor/
│   │   │   │   ├── news/
│   │   │   │   ├── result/
│   │   │   │   ├── standing/
│   │   │   │   ├── track/
│   │   │   │   ├── user/
│   │   │   │   └── weeklyrace/
│   │   │   ├── ui/               # UI Layer (MVVM)
│   │   │   │   ├── bio/          # Biography Activities
│   │   │   │   ├── event/        # Event Activities
│   │   │   │   ├── home/         # Home & Fragments
│   │   │   │   ├── junior/       # Junior Categories
│   │   │   │   ├── profile/      # User Profile
│   │   │   │   ├── standing/     # Standings
│   │   │   │   └── welcome/      # Authentication
│   │   │   └── util/             # Utilities
│   │   ├── res/                  # Resources
│   │   └── AndroidManifest.xml
│   ├── test/                     # Unit Tests
│   └── androidTest/              # Instrumentation Tests
├── functions/                    # Firebase Cloud Functions
│   ├── f1_logic.js
│   ├── junior_categories_logic.js
│   └── index.js
└── build.gradle.kts
```

### Design Patterns

- **MVVM (Model-View-ViewModel)** - UI layer architecture
- **Repository Pattern** - Data access abstraction
- **Singleton Pattern** - Service locator and database instances
- **Observer Pattern** - LiveData for reactive UI updates
- **Factory Pattern** - ViewModel creation

### Tech Stack

#### Core
- **Language:** Java 11
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Compile SDK:** 36

#### Libraries & Frameworks

**UI & Navigation**
- AndroidX AppCompat `1.7.1`
- Material Design Components `1.13.0`
- Navigation Component `2.9.6`
- ConstraintLayout `2.2.1`
- SwipeRefreshLayout `1.1.0`

**Networking**
- Retrofit `3.0.0`
- OkHttp Logging Interceptor `5.3.2`
- Gson `2.13.2`

**Database**
- Room `2.8.4`

**Firebase**
- Firebase BOM `34.6.0`
- Firebase Authentication `24.0.1`
- Firebase Realtime Database `22.0.1`
- Firebase Firestore `26.0.2`
- Firebase Analytics
- Google Play Services Auth `21.4.0`

**Image Loading**
- Glide `5.0.5`

**Utilities**
- Lombok `1.18.42` (Annotation Processing)
- ThreeTenBP `1.7.2` (Date/Time)
- Rome `2.1.0` (RSS Feed Parsing)
- Commons Validator `1.10.1`

**Testing**
- JUnit `4.13.2`
- AndroidX Test `1.3.0`
- Espresso `3.7.0`

---

## 🔌 APIs & Data Sources

### F1 Data API
- **Provider:** [Jolpi.ca Ergast API](https://api.jolpi.ca/ergast/f1/)
- **Data:** Race results, standings, driver/constructor info, schedules

### Firebase Services
- **Authentication** - User sign-in/sign-up
- **Realtime Database** - User preferences and app data
- **Cloud Functions** - Scheduled data updates

### News Sources
- **Autosport** - English F1 news
- **Crash.net** - English F1 coverage
- **Motorsport** - Italian F1 news

---

## 🛠️ Development

### Build Variants

```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### Running Tests

```bash
# Unit Tests
./gradlew test

# Instrumentation Tests
./gradlew connectedAndroidTest

# Generate Coverage Report
./gradlew jacocoTestReport
```

### Code Style

This project follows standard Android coding conventions:
- Package naming: lowercase
- Class naming: PascalCase
- Method/variable naming: camelCase
- Constants: UPPER_SNAKE_CASE
- 4-space indentation

### Lombok Usage

The project uses Lombok for reducing boilerplate code:
- `@Getter` / `@Setter` - Automatic getters/setters
- `@NoArgsConstructor` / `@AllArgsConstructor` - Constructor generation
- `@ToString` - toString() method generation
- `@EqualsAndHashCode` - equals() and hashCode() methods

---

## 📋 TODO & Roadmap

Check out our [TODO.md](TODO.md) for a comprehensive list of planned improvements and features.

### High Priority
- [ ] Add comprehensive unit tests
- [ ] Enable ProGuard for release builds
- [ ] Implement Firebase Crashlytics
- [ ] Migrate to Kotlin (incremental)
- [ ] Add dependency injection (Hilt)

### Future Features
- [ ] Push notifications for race updates
- [ ] Home screen widgets
- [ ] Offline mode enhancement
- [ ] Social features (predictions, discussions)
- [ ] Wear OS support

---

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Commit your changes**
   ```bash
   git commit -m 'Add some amazing feature'
   ```
4. **Push to the branch**
   ```bash
   git push origin feature/amazing-feature
   ```
5. **Open a Pull Request**

### Contribution Guidelines

- Follow existing code style and conventions
- Add unit tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting PR
- Write clear commit messages

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Contact & Support

### Team Members
- **Broglio Matteo** - [GitHub](https://github.com/mbroglio)
- **Caputo Lorenzo** - [GitHub](https://github.com/Lorenzo207378)
- **Gargioli Federico** - [GitHub](https://github.com/fgargioli)
- **Groppo Gabriele** - [GitHub](https://github.com/GabrieleGroppo)
- **Lanticina Riccardo** - [GitHub](https://github.com/Riccardolanticina)

### Project Links
- **GitHub:** [https://github.com/yourusername/fastestlap](https://github.com/mbroglio/fastestlap)
- **Documentation:** [Documentazione/Documentazione FastestLap.pdf](Documentazione/Documentazione%20FastestLap.pdf)
- **Presentation:** [Documentazione/Presentazione FastestLap.pdf](Documentazione/Presentazione%20FastestLap.pdf)

---

## 🙏 Acknowledgments

- **Jolpi.ca** for providing the F1 Ergast API
- **Firebase** for backend services
- **Material Design** for UI components
- **Glide** for efficient image loading
- All open-source libraries that made this project possible

---

## 📊 Project Stats

- **Language:** Java
- **Lines of Code:** ~10,000+
- **Activities:** 15+
- **Fragments:** 10+
- **API Integrations:** 3+
- **Supported Languages:** 2 (EN, IT)

---

<div align="center">

**Made with ☕ by The Coffee Coders**

⭐ Star this repo if you find it useful!

[Report Bug](https://github.com/yourusername/fastestlap/issues) • [Request Feature](https://github.com/yourusername/fastestlap/issues)

</div>

