# FastestLap 🏎️

<div align="center">

![FastestLap Logo](Screenshots/Home%20with%20Results%20and%20Preferences.png)

**Your Ultimate Formula 1, Formula 2 & Formula 3 Companion App**

[![Version](https://img.shields.io/badge/version-1.2.0-red.svg)](https://github.com/mbroglio/fastestlap)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/license-MIT-orange.svg)](LICENSE)
[![Firebase](https://img.shields.io/badge/Firebase-RTDB%20%7C%20FCM%20%7C%20Functions-orange.svg)](https://firebase.google.com/)

[Features](#features) • [Screenshots](#screenshots) • [Architecture](#architecture) • [Cloud Functions](#cloud-functions--backend) • [Installation](#installation) • [Roadmap](#roadmap)

</div>

---

## 📖 About

**FastestLap** is a high-performance, real-time Android application engineered for motorsport enthusiasts. Covering **Formula 1**, **Formula 2**, and **Formula 3**, FastestLap delivers live race telemetry, live timing intervals, pit stop and stint strategies, driver and team radios with audio playback, race direction steward messages, live weather radar forecasts, championship standings, multi-source breaking news, and detailed career biographies.

FastestLap integrates a cloud-native backend built on **Firebase Realtime Database**, **Cloud Functions (Node.js 22)**, and **Firebase Cloud Messaging (FCM)** to provide automated real-time background notifications and stats reconciliation.

### 👥 Team: The Coffee Coders

- **Broglio Matteo**
- **Caputo Lorenzo**
- **Gargioli Federico**
- **Groppo Gabriele**
- **Lanticina Riccardo**

---

## ✨ Features

### ⏱️ Live Timing & Race Control (OpenF1 Integration)
- **Real-Time Leaderboard** - Live driver positions, gaps to leader, interval deltas, sector times, and current lap times.
- **Team Radios with Audio Playback** - Stream driver-to-pit-wall radio messages with embedded audio player (`.mp3` clips fetched in real-time from OpenF1).
- **Race Direction & Stewards Feed** - Instant visual flags and banners for Safety Car, Virtual Safety Car (VSC), Red/Yellow/Green flags, track limits infractions, and steward investigations.
- **Tyre Stints & Pit Stop Strategies** - Comprehensive strategy breakdown showing tyre compound choices (Soft, Medium, Hard, Intermediate, Wet), stint lap durations, and pit lane stop times.
- **Fault-Tolerant Resilience** - If live telemetry or stint data is temporarily pending or unavailable from external providers, the app gracefully falls back to "Not Available" without disrupting overall race results, caching retrieved sessions into **Room Database** for offline access.

### 🏁 Race Information & Calendars
- **Comprehensive Grand Prix Weekend Schedule** - Complete breakdown of Practice 1, Practice 2, Practice 3, Sprint Shootout, Sprint, Qualifying, and Race sessions.
- **Dynamic Timezone Conversion** - Session times automatically converted to the user's local device timezone with countdown timers (days, hours, minutes, seconds).
- **Calendar Export (.ics)** - One-click export of full weekend session schedules directly into the device's native calendar (Google Calendar, Outlook, etc.).
- **Past & Upcoming Events** - Browse historical race winners, podium finishers, and upcoming Grand Prix with circuit overviews.

### 🌤️ Circuit Weather Suite (Open-Meteo Integration)
- **Live Circuit Weather & Hourly Forecasts** - Accurate track-specific meteorological forecasts powered by Open-Meteo coordinates.
- **Track & Environmental Metrics** - Air temperature, track asphalt temperature, precipitation probability, relative humidity, wind speed, and direction.
- **Dynamic Atmospheric Backgrounds** - Immersive video backgrounds (`.mp4`) that adapt to actual sky conditions (sunny, overcast, light rain, heavy rain, night).

### 🏆 Standings & In-Depth Biographies
- **Championship Standings** - Real-time Driver and Constructor championship tables for F1, F2, and F3 with points, podiums, wins, and team colors.
- **Driver Bio Pages** - High-resolution portrait, permanent driver number, national flag, biography, and **interactive circular tachometer gauges** displaying career podium percentages (`podiums / gps_entered * 100`) and win ratios.
- **Constructor Bio Pages** - Team car liveries, team principal, technical chief, chassis, power unit supplier, headquarters, and current driver pairings.
- **Circuit Bio Pages** - Vector track layout, country flag, inaugural GP date, lap records, lap distance, and total race distance.
- **Pull-To-Refresh** - SwipeRefreshLayout across all bio and standing pages forcing remote synchronization from Firebase RTDB.

### 🎯 Junior Categories Suite (Formula 2 & Formula 3)
- **Modular Fragment Architecture** - Fast, dedicated tabs for:
  - **Calendario**: Round-by-round calendar with Sprint and Feature race results.
  - **Classifica Piloti**: Full driver championship standings with team badges.
  - **Classifica Costruttori**: Team standings and points.
  - **Entry List**: Custom cards showcasing team logo, driver numbers, national flags, driver portraits, and the **official team car livery** rendered beneath the lineup.
  - **Notizie Junior**: Dedicated F2 & F3 news stream scraped directly from official FIA championship portals with tag filtering.

### 📰 Multi-Source News Feed
- **F1 News Hub** - Aggregated news coverage from top motorsport publications:
  - **Motorsport.com (IT)** (Italian)
  - **Autosport** (English)
  - **Crash.net** (English)
- **Official Junior Series News** - Breaking stories from `fiaformula2.com` and `fiaformula3.com`.
- **Scheduled Background Scraping** - Backend Cloud Function checks RSS feeds and FIA pages **every 15 minutes** for new articles.

### 🔔 Centralized Push Notifications (FCM v4)
- **Session Reminders** - Automated alerts sent **30 minutes** and **5 minutes** prior to session start times across F1, F2, and F3.
- **Custom Audio Channel** - Notifications play the custom F1 `team_radio.mp3` alert sound.
- **Breaking News Push** - Immediate notifications when a new article is published by the user's selected news sources or junior series.
- **Post-Race Penalty & Sanction Alerts** - Background verification detects post-race time penalties, disqualifications, or points adjustments within a 12-hour window post-race, pushing updated results immediately.
- **Granular Preference Toggles** - Enable or disable individual session alerts, news topics, and junior notifications in User Profile settings.

### 🎨 Visual Design & Theme
- **High-Contrast Racing Dark Theme** - Built with Material Design 3 tokens (`#121215` background, `#1C1D24` / `#262732` elevated cards, `#E10600` racing red accent).
- **Official Team Liveries** - Accurate hex colors for all constructors (Ferrari, McLaren, Red Bull, Mercedes, Aston Martin, Haas, Racing Bulls, Williams, Alpine, Kick Sauber, Audi, Cadillac).
- **Custom Typography** - **Orbitron** font for digital timers, telemetry, positions, and lap deltas; **Roboto** for clear editorial content.

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

## 🏗️ Architecture

FastestLap follows **Clean Architecture** principles with a strict separation of concerns, reactive data flows, and offline-first caching.

```
FastestLap/
├── app/
│   ├── src/main/java/com/the_coffe_coders/fastestlap/
│   │   ├── adapter/              # RecyclerView & ViewPager Adapters
│   │   ├── api/                  # Retrofit API interfaces (OpenF1, Open-Meteo, Jolpica)
│   │   ├── database/             # Room Database (DAOs & Entities for offline cache)
│   │   ├── domain/               # Domain Models (F1, Junior, Live Timing, News, Weather)
│   │   ├── repository/           # Repository Pattern Implementations & Callbacks
│   │   ├── source/               # Remote & Local Data Sources (Firebase, REST, Scraping)
│   │   ├── ui/                   # MVVM UI Layer
│   │   │   ├── bio/              # Driver, Constructor & Track Bio Activities
│   │   │   ├── event/            # Grand Prix Weekend, Results & Stints Activities
│   │   │   ├── home/             # Home Dashboard & Bottom Nav Fragments
│   │   │   ├── junior/           # Formula 2 & Formula 3 Modular Activities/Fragments
│   │   │   ├── live/             # Live Timing, Race Control & Radio Audio Player
│   │   │   ├── news/             # Multi-source News Fragment & Detail Viewer
│   │   │   ├── profile/          # Profile, Preferences & Notification Settings
│   │   │   ├── standing/         # Drivers & Constructors Standings
│   │   │   └── weather/          # Circuit Weather Forecast Activity
│   │   └── util/                 # Constants, UIUtils, Notification Managers & Schedulers
│   ├── res/                      # XML Layouts, Team Colors, Orbitron/Roboto Fonts & Audio
│   └── functions/                # Cloud Functions Backend (Node.js 22)
│       ├── index.js              # Schedulers & HTTP Trigger Endpoints
│       ├── f1_logic.js           # Idempotent F1 Results, Season Stats & Variations
│       ├── junior_categories_logic.js # F2/F3 Calendar, Standings & Entry List Scrapers
│       ├── notification_logic.js # 15-min News Check & 5-min Session Reminders
│       ├── calendar_logic.js     # Automated Daily F1/F2/F3 Calendar Sync
│       └── career_stats_logic.js # Jolpica Career Stats Verification & Reconciler
└── build.gradle.kts
```

---

## ⚡ Cloud Functions & Backend

The backend runs on **Google Cloud Functions v2** (Node.js 22) connected to **Firebase Realtime Database** and **Firebase Cloud Messaging**:

| Function Name | Type | Schedule / Trigger | Purpose |
| :--- | :--- | :--- | :--- |
| `checkAndPushNews` | Scheduler | Every 15 minutes | Scrapes F1 RSS feeds (Motorsport, Autosport, Crash) and official F2/F3 websites; pushes alerts to dedicated FCM topics. |
| `checkAndPushSessions` | Scheduler | Every 5 minutes | Scans stored F1, F2, and F3 calendars; sends -30m and -5m reminder alerts with custom radio sound. |
| `updateRaceStats` | Scheduler | Every 30 minutes | Activates ~2h post-race; applies race results to season stats, verifies post-race penalties within a 12h window, and pushes updates if classifications change. |
| `syncCalendars` | Scheduler | Daily at 04:00 Rome | Synchronizes full season calendars for F1, F2, and F3 from Jolpica into Firebase RTDB. |
| `updateJuniorSeries` | Scheduler | Sun & Mon at 14:00 | Updates F2 and F3 race results and championship standings. |
| `syncAllCareerStatsNow`| HTTP Endpoint | On-Demand (GET) | Reconciles and repairs career statistics (podi, vittorie, GP disputati) directly from Jolpica API with rate-limit backoff. |
| `updateRaceStatsNow` | HTTP Endpoint | On-Demand (GET) | Forces immediate post-race calculation for a specific round or season. |
| `syncCalendarsNow` | HTTP Endpoint | On-Demand (GET) | Triggers immediate manual calendar synchronization for F1, F2, and F3. |

---

## 🔌 APIs & Data Sources

- **[OpenF1 API](https://openf1.org/)** - Real-time timing, sector intervals, pit stop durations, tyre compound stints, team radio audio clips, and race control banners.
- **[Jolpi.ca Ergast API](https://api.jolpi.ca/ergast/f1/)** - Official historical Formula 1 results, driver championship standings, constructor standings, and schedules.
- **[Open-Meteo API](https://open-meteo.com/)** - High-resolution weather forecasts, rain probability, wind speed, and track temperatures based on circuit coordinates.
- **FIA Formula 2 & Formula 3 Portals** - Live scraping for entry lists, car liveries, and championship standings.
- **Motorsport IT, Autosport, Crash.net** - Multi-language RSS news streams.

---

## 🚀 Installation & Setup

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK** 17 or higher
- **Android SDK** API 26+ (Android 8.0 Oreo minimum)
- **Node.js 22+** and **Firebase CLI** (for Cloud Functions development)

### Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/mbroglio/fastestlap.git
   cd fastestlap
   ```

2. **Configure Firebase**
   - Place your `google-services.json` inside the `app/` directory.
   - Ensure Firebase Authentication (Email/Password, Google OAuth) and Realtime Database are enabled.

3. **Build the Project**
   ```bash
   ./gradlew build
   ```
   Or on Windows:
   ```cmd
   gradlew.bat build
   ```

4. **Deploy Cloud Functions (Optional)**
   ```bash
   cd app/functions
   npm install
   firebase deploy --only functions
   ```

---

## 📋 Roadmap

### 🏎️ Real-Time & Mobile Enhancements
- [x] **OpenF1 Live Timing** (intervals, sector times, tyre compound stints)
- [x] **Live Team Radios** (with embedded audio player)
- [x] **Live Race Control Messages** (Safety Car, VSC, flags, track limits, stewards)
- [x] **Circuit Weather Forecasts** (Open-Meteo with dynamic weather video backgrounds)
- [x] **Junior Categories Suite** (Modular F2 & F3 Calendars, Standings, Entry List with car liveries)
- [x] **Centralized Cloud Push Notifications** (FCM v4 for 30m/5m sessions, breaking news, post-race penalty variations)
- [x] **Idempotent Stats Engine** (Career stats protection and Jolpica reconciliation endpoint)

### 🌐 Cross-Platform Web Expansion
- [ ] **FastestLap iOS-Style Web App / PWA** (`web/` client powered by React + Vite + TypeScript + Tailwind CSS with Apple Human Interface Guidelines).

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

---

<div align="center">

**Made with ☕ by The Coffee Coders**

⭐ Star this repo if you find it useful!

</div>
