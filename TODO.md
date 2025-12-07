# FastestLap - TODO & Improvements List

## Project Overview
FastestLap is an Android application for Formula 1 enthusiasts, providing race results, driver and constructor standings, event schedules, news feeds, and support for junior categories (Formula 2 & Formula 3).

**Team:** The Coffee Coders  
**Date Created:** December 7, 2025

---

## 🔴 Critical Issues

### 1. Database Configuration
- [ ] **Enable Room Schema Export** (`AppRoomDatabase.java:28`)
  - Currently set to `exportSchema = false`
  - Should enable schema export and configure proper directory
  - Add schema location to `.gitignore` if containing sensitive data
  - Benefits: Version control, migration management, debugging

### 2. Remove Main Thread Database Queries
- [ ] **Remove `allowMainThreadQueries()`** (`AppRoomDatabase.java:41`)
  - Currently allows blocking operations on main thread
  - Refactor all database operations to use coroutines or RxJava
  - High priority: Can cause ANR (Application Not Responding) errors
  - Use LiveData, Flow, or callbacks for async operations

### 3. Error Handling
- [ ] **Replace printStackTrace with proper logging** (`UIUtils.java:498`)
  - Use Android's Log framework consistently
  - Implement centralized error logging
  - Consider adding Firebase Crashlytics for production error tracking

---

## 🟡 High Priority Improvements

### Testing & Quality Assurance

#### 4. Unit Testing Coverage
- [ ] Add comprehensive unit tests
  - Current state: Only basic example tests exist
  - Empty test classes: `DriverTest.java`, `ConstructorTest.java`
  - Target coverage: At least 60-70% for core business logic
  - Focus areas:
    - Repository layer tests
    - ViewModel tests
    - Data mapping/parsing tests
    - Business logic in domain models

#### 5. Integration & UI Testing
- [ ] Implement instrumented tests
  - Test database operations
  - Test API integrations
  - Add UI tests using Espresso for critical user flows
  - Test navigation between activities/fragments

#### 6. Add Missing Test Dependencies
- [ ] Add testing libraries to `build.gradle.kts`:
  ```kotlin
  testImplementation("org.mockito:mockito-core:5.20.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:6.1.0")
  testImplementation("org.robolectric:robolectric:4.16")
  testImplementation("androidx.arch.core:core-testing:2.x.x")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.x.x")
  ```

### Security & Configuration

#### 7. API Key Management
- [ ] Move API keys to secure location
  - Use BuildConfig for API keys
  - Implement `local.properties` for sensitive keys
  - Add to `.gitignore` (currently `local.properties` is ignored)
  - Use Android's Secrets Gradle Plugin
  - Review: Check if any API keys are hardcoded in source files

#### 8. Firebase Configuration Security
- [ ] Verify Firebase credentials are not exposed
  - Check `google-services.json` in version control
  - Consider using different Firebase projects for dev/prod
  - Review Firebase security rules

#### 9. ProGuard/R8 Configuration
- [ ] Enable code obfuscation for release builds (`build.gradle.kts`)
  - Currently: `isMinifyEnabled = false`
  - Configure ProGuard rules properly
  - Add rules for Retrofit, Room, Firebase, Lombok, Glide
  - Test release builds thoroughly after enabling

---

## 🟢 Architecture & Code Quality

### 10. Migrate to Kotlin
- [ ] **Consider Kotlin migration**
  - Current: 100% Java codebase
  - Benefits: Null safety, coroutines, modern syntax
  - Can be done incrementally, file by file
  - Start with utility classes and models
  - Update build configuration for Kotlin support

### 11. Dependency Injection
- [ ] Implement proper DI framework
  - Current: Manual dependency management via `ServiceLocator`
  - Recommended: Hilt/Dagger for Android
  - Benefits: Better testability, lifecycle management
  - Alternative: Koin (lighter weight, Kotlin-first)

### 12. Coroutines/Flow Integration
- [ ] Replace callbacks with Coroutines/Flow
  - Current: Using callbacks extensively (e.g., `UserResponseCallback`)
  - Benefits: Better async handling, reduced callback hell
  - Integrate with LiveData/StateFlow for UI updates
  - Handle cancellation properly

### 13. ViewModel Improvements
- [ ] Standardize ViewModel creation
  - Some use ViewModelFactory, consider ViewModelProvider.Factory with Hilt
  - Add SavedStateHandle support for process death scenarios
  - Implement proper error state handling in ViewModels

### 14. Navigation Component Enhancement
- [ ] Review navigation architecture
  - Currently using mix of activities and fragments
  - Consider single-activity architecture with Navigation Component
  - Use SafeArgs plugin for type-safe navigation
  - Review: Many activities with `screenOrientation="portrait"` - make flexible

### 15. Repository Pattern Enhancement
- [ ] Improve data layer architecture
  - Add proper caching strategy documentation
  - Implement single source of truth pattern
  - Consider using Repository + UseCase layer
  - Add offline-first capabilities with proper sync

---

## 🎨 UI/UX Improvements

### 16. Material Design 3 Migration
- [ ] Update to Material Design 3
  - Current: Material Design 2
  - Update color schemes, typography, components
  - Implement dynamic theming (Material You)
  - Update dependency: `com.google.android.material:material`

### 17. Dark Mode Enhancement
- [ ] Improve dark mode implementation
  - Current: Has `values-night` resources
  - Test all screens in dark mode
  - Ensure proper contrast ratios
  - Add user preference to force dark/light mode

### 18. Accessibility
- [ ] Add comprehensive accessibility support
  - Content descriptions for all images
  - Proper focus order
  - Screen reader testing (TalkBack)
  - Minimum touch target sizes (48dp)
  - Color contrast validation (WCAG AA compliance)

### 19. Loading States & Error Handling
- [ ] Standardize loading and error states
  - Create consistent loading screen designs
  - Implement proper error messages with retry actions
  - Add empty state screens
  - Network error vs. server error differentiation
  - Offline mode indicators

### 20. Image Loading Optimization
- [ ] Optimize Glide usage
  - Current: Custom retry logic implemented
  - Consider adding placeholder/error images consistently
  - Implement image caching strategies
  - Add image compression for better performance
  - Review: Multiple Glide retry implementations - consolidate

---

## 📱 Features & Functionality

### 21. Offline Support
- [ ] Enhance offline capabilities
  - Cache race results and standings
  - Offline news reading
  - Better cache invalidation strategy
  - Add sync indicators

### 22. Push Notifications
- [ ] Implement push notifications
  - Race start reminders
  - Result availability notifications
  - News alerts for favorite drivers/teams
  - Use Firebase Cloud Messaging (already has Firebase)

### 23. User Preferences Enhancement
- [ ] Expand user customization
  - Theme selection (light/dark/system)
  - Notification preferences
  - News source customization
  - Language preference persistence
  - Data usage settings (WiFi only)

### 24. Widget Support
- [ ] Add home screen widgets
  - Next race countdown widget
  - Current standings widget
  - Latest results widget

### 25. Share Functionality
- [ ] Add sharing capabilities
  - Share race results
  - Share driver/constructor profiles
  - Share news articles
  - Social media integration

### 26. Calendar Integration
- [ ] Add race events to device calendar
  - Export race schedule
  - Set reminders
  - Sync with Google Calendar

### 27. Search Functionality
- [ ] Implement search features
  - Search drivers
  - Search constructors
  - Search past races
  - Search news articles

---

## 🔧 Technical Improvements

### 28. API Client Enhancements
- [ ] Improve API layer
  - Add request/response interceptors for logging
  - Implement API response caching (HTTP cache)
  - Add rate limiting handling
  - Consider GraphQL for flexible data fetching
  - Review: Custom `RetryInterceptor` - ensure it's optimized

### 29. Update Dependencies
- [ ] Review and update all dependencies
  - Check for security vulnerabilities
  - Update to latest stable versions
  - Current Gradle: 8.13.1 (review for newer versions)
  - Firebase BOM: 34.6.0 (check for updates)
  - Retrofit: 3.0.0 (recently updated, good)
  - Consider adding dependency update tools (e.g., Dependabot)

### 30. Build Configuration
- [ ] Optimize build configuration
  - Enable incremental compilation
  - Configure build variants (dev, staging, prod)
  - Add version name automation
  - Implement build-time code generation where applicable
  - Review: `compileSdk = 36`, `targetSdk = 34` - align these

### 31. Memory Management
- [ ] Optimize memory usage
  - Profile app with Android Profiler
  - Fix memory leaks (check ViewModels, callbacks)
  - Optimize image loading and caching
  - Reduce APK size with resource optimization
  - Review: Bitmap usage in adapters

### 32. Network Optimization
- [ ] Improve network efficiency
  - Implement pagination for lists
  - Use GraphQL or optimize REST queries
  - Add request prioritization
  - Implement exponential backoff properly
  - Connection pooling optimization

### 33. Database Optimization
- [ ] Enhance Room database
  - Add database migrations for future schema changes
  - Implement database indices for frequently queried fields
  - Consider using FTS (Full-Text Search) for search features
  - Add database version upgrade tests
  - Review: Current version = 1, prepare for v2

### 34. Logging Framework
- [ ] Implement proper logging
  - Replace mix of `Log.e`, `Log.i`, `Log.d` with Timber
  - Add log levels configuration
  - Remove logs from release builds
  - Add debug logging tools (e.g., Hyperion, Chuck/Chucker)

---

## 📚 Documentation

### 35. Code Documentation
- [ ] Add comprehensive documentation
  - JavaDoc/KDoc for public APIs
  - Inline comments for complex logic
  - Architecture decision records (ADR)
  - Data flow diagrams

### 36. README Enhancement
- [ ] Improve README.md
  - Add project description
  - Installation instructions
  - Build instructions
  - Architecture overview
  - Screenshots (already exist in folder)
  - Contribution guidelines
  - License information

### 37. API Documentation
- [ ] Document API integrations
  - Jolpi.ca F1 API documentation
  - Firebase configuration guide
  - RSS feed sources
  - API rate limits and constraints

### 38. User Documentation
- [ ] Create user guides
  - Feature walkthrough
  - FAQ section
  - Troubleshooting guide
  - Privacy policy
  - Terms of service

---

## 🚀 DevOps & CI/CD

### 39. Continuous Integration
- [ ] Set up CI/CD pipeline
  - GitHub Actions / GitLab CI / Jenkins
  - Automated testing on commits
  - Lint checks (ktlint/detekt for Kotlin)
  - Code coverage reports
  - Automated APK builds

### 40. Code Quality Tools
- [ ] Integrate code quality tools
  - SonarQube or CodeClimate
  - Android Lint checks
  - Checkstyle/Spotless for code formatting
  - Detekt for Kotlin (if migrating)
  - Pre-commit hooks

### 41. Automated Release Process
- [ ] Implement release automation
  - Semantic versioning
  - Changelog generation
  - Google Play Store deployment
  - Beta testing via Firebase App Distribution
  - APK signing automation

### 42. Monitoring & Analytics
- [ ] Add production monitoring
  - Firebase Crashlytics (crash reporting)
  - Firebase Analytics (currently added but may need events)
  - Performance monitoring
  - Network monitoring
  - User behavior analytics

---

## 🌍 Localization

### 43. Complete Localization
- [ ] Enhance multi-language support
  - Current: English (en-GB) and Italian (it)
  - Verify all strings are translatable
  - Remove hardcoded strings (check for any remaining)
  - Add more languages (Spanish, German, French, etc.)
  - Test RTL language support (Arabic, Hebrew)
  - Review: Some `translatable="false"` flags - verify correctness

### 44. Regional Formatting
- [ ] Implement proper regional formats
  - Date/time formatting per locale
  - Number formatting
  - Distance units (km/miles)
  - Temperature units (°C/°F)
  - Use ThreeTenBP properly (already included)

---

## 🔐 Privacy & Compliance

### 45. GDPR Compliance
- [ ] Ensure GDPR compliance
  - Privacy policy implementation
  - User data export functionality
  - Right to be forgotten (account deletion)
  - Cookie consent (if using web views)
  - Data retention policies

### 46. Firebase Compliance
- [ ] Review Firebase data collection
  - Analytics opt-in/opt-out
  - Data collection disclosure
  - User consent management
  - Anonymous usage statistics

---

## 🎯 Performance

### 47. App Startup Optimization
- [ ] Optimize app launch time
  - Profile startup with App Startup library
  - Lazy initialization of components
  - Optimize splash screen
  - Review: `SplashActivity` - ensure quick transition

### 48. RecyclerView Optimization
- [ ] Optimize list performance
  - Implement DiffUtil in all adapters
  - Use proper ViewHolder pattern
  - Image loading optimization in lists
  - Pagination/endless scrolling
  - Review: Multiple RecyclerView adapters - ensure they're optimized

### 49. Animation & Transitions
- [ ] Add smooth transitions
  - Fragment transitions
  - Activity transitions
  - Shared element transitions
  - Loading animations
  - Skeleton screens for content loading

---

## 🧪 Junior Categories Features

### 50. Formula 2 & Formula 3 Enhancement
- [ ] Expand junior categories support
  - Current: Basic F2/F3 support exists
  - Add standings for F2/F3
  - Historical data for junior categories
  - Driver progression tracking (F3 → F2 → F1)
  - Comparison features

---

## 📊 Data Management

### 51. Cache Management
- [ ] Implement intelligent caching
  - Current: `FRESH_TIMEOUT = 1 minute`
  - Review and optimize cache durations
  - Add cache size limits
  - Implement cache clearing
  - User control over cache settings

### 52. Data Synchronization
- [ ] Improve data sync strategy
  - Background sync using WorkManager
  - Periodic updates for standings
  - Real-time updates for live races
  - Conflict resolution strategies

---

## 🔄 Future Enhancements

### 53. Live Race Features
- [ ] Add live race functionality
  - Live timing data (if API available)
  - Live position tracking
  - Push notifications for key events
  - Live commentary integration

### 54. Social Features
- [ ] Add community features
  - User profiles
  - Predictions and fantasy league
  - Discussion forums
  - Friend system
  - Leaderboards

### 55. Advanced Analytics
- [ ] Add data visualization
  - Race statistics charts
  - Driver comparison graphs
  - Historical performance trends
  - Team performance analysis
  - Track statistics

### 56. AR Features
- [ ] Explore AR possibilities
  - 3D car models
  - Track visualizations
  - Driver helmet scanning
  - Circuit exploration

### 57. Wear OS Support
- [ ] Add smartwatch companion app
  - Race countdown
  - Live results
  - Quick notifications
  - Glance tiles

---

## 🧹 Code Cleanup

### 58. Remove Unused Resources
- [ ] Clean up resources
  - Remove unused drawables
  - Remove unused layouts
  - Remove unused strings
  - Optimize image assets
  - Use Android Lint to identify unused resources

### 59. Code Refactoring
- [ ] Refactor complex classes
  - Break down large activities
  - Extract utility functions
  - Reduce code duplication
  - Apply SOLID principles
  - Review: `UIUtils.java` - consider breaking into smaller classes

### 60. Naming Conventions
- [ ] Standardize naming
  - Review package structure
  - Consistent variable naming
  - Consistent file naming
  - Follow Android Kotlin Style Guide (if migrating)

### 61. Remove TODO Comments
- [ ] Address existing TODOs
  - Database schema export (AppRoomDatabase.java:28)
  - Review strings.xml:18 "Remove or change this placeholder text"
  - Search for any other TODO comments

---

## 🔍 Firebase Functions

### 62. Firebase Functions Enhancement
- [ ] Improve Firebase Cloud Functions
  - Current: Located in `app/functions/`
  - Add error monitoring
  - Implement retry strategies
  - Add unit tests for functions
  - Optimize execution time
  - Review scheduling (currently: Monday 20:00 for race stats)
  - Consider using TypeScript instead of JavaScript

### 63. Functions Deployment
- [ ] Improve functions deployment
  - Add deployment scripts
  - Environment variables management
  - Staging vs. production functions
  - Function versioning
  - Rollback capabilities

---

## 📈 Metrics & KPIs

### 64. App Metrics Dashboard
- [ ] Implement metrics tracking
  - User engagement metrics
  - Feature usage analytics
  - Crash-free users percentage
  - App performance metrics
  - API response times

---

## 🎨 Branding & Polish

### 65. Improve App Identity
- [ ] Enhance branding
  - Consistent color palette
  - Typography guidelines
  - Icon design consistency
  - Splash screen optimization
  - App icon variations (adaptive icons)

### 66. Onboarding Experience
- [ ] Add user onboarding
  - First-time user tutorial
  - Feature highlights
  - Permission explanations
  - Setup wizard for preferences

---

## 🛡️ Error Prevention

### 67. Null Safety
- [ ] Improve null handling
  - Add @Nullable/@NonNull annotations
  - Use Optional where appropriate
  - Defensive programming practices
  - Consider migrating to Kotlin for null safety

### 68. Input Validation
- [ ] Enhance input validation
  - Current: Some validation with commons-validator
  - Validate all user inputs
  - API response validation
  - Date/time validation
  - Network timeout configurations

---

## 📱 Device Compatibility

### 69. Screen Size Support
- [ ] Test on various screen sizes
  - Phones (small, normal, large)
  - Tablets (7", 10")
  - Foldable devices
  - Different aspect ratios
  - Review: Currently portrait-only, consider landscape

### 70. Android Version Support
- [ ] Optimize Android version support
  - Current: minSdk = 24 (Android 7.0)
  - targetSdk = 34 (Android 14)
  - Consider increasing minSdk for modern features
  - Ensure backward compatibility is tested
  - Plan for Android 15 features

---

## 🔌 Third-Party Integrations

### 71. News Sources Expansion
- [ ] Add more news sources
  - Current: Autosport, Crash.net, Motorsport
  - Add The Race, F1 official, RaceFans, etc.
  - User customizable news sources
  - News filtering by category

### 72. Weather Integration
- [ ] Improve weather features
  - Current: Redirects to Google Weather
  - Integrate weather API directly
  - Show weather forecast on event pages
  - Historical weather data

### 73. Map Integration
- [ ] Enhance map features
  - Current: Opens Google Maps
  - Embed maps directly
  - Show circuit layouts
  - Track sector information

---

## 🎓 Learning & Best Practices

### 74. Code Review Process
- [ ] Establish code review guidelines
  - Pull request templates
  - Review checklists
  - Coding standards document

### 75. Technical Debt Management
- [ ] Track technical debt
  - Create technical debt register
  - Prioritize refactoring tasks
  - Allocate time for debt reduction

---

## 📦 Release Management

### 76. Version Strategy
- [ ] Implement versioning strategy
  - Current: versionCode = 1, versionName = "1.0"
  - Semantic versioning (MAJOR.MINOR.PATCH)
  - Release notes automation
  - Beta/alpha channel management

### 77. Play Store Optimization
- [ ] Optimize Play Store presence
  - App description optimization
  - Screenshot updates (folder exists)
  - Feature graphics
  - Promotional video
  - A/B testing for store listing

---

## ✅ Quick Wins (Start Here)

These can be implemented quickly for immediate benefits:

1. ✅ Enable Room schema export
2. ✅ Replace printStackTrace with Log
3. ✅ Add more unit tests (start with DriverTest, ConstructorTest)
4. ✅ Update README with project info
5. ✅ Enable ProGuard for release builds
6. ✅ Add Firebase Crashlytics
7. ✅ Implement DiffUtil in RecyclerView adapters
8. ✅ Add content descriptions for accessibility
9. ✅ Remove unused resources using Lint
10. ✅ Add CI/CD with GitHub Actions

---

## 📊 Priority Matrix

### Must Have (P0)
- Remove allowMainThreadQueries
- Enable schema export
- Security: API key management
- Basic unit test coverage

### Should Have (P1)
- Kotlin migration
- Dependency injection (Hilt)
- Crashlytics integration
- ProGuard configuration

### Nice to Have (P2)
- Material Design 3
- Widgets
- Advanced caching
- Social features

### Future (P3)
- AR features
- Wear OS
- Advanced analytics
- Live timing

---

## 📝 Notes

- This is a well-structured project with good separation of concerns (Repository pattern, MVVM)
- Already using modern libraries (Retrofit, Room, Glide, Navigation Component)
- Firebase integration is set up
- Localization support exists (English & Italian)
- Good use of Lombok for reducing boilerplate

**Recommended Next Steps:**
1. Address critical issues (database main thread, schema export)
2. Add comprehensive testing
3. Set up CI/CD
4. Consider Kotlin migration for long-term maintainability
5. Implement proper error tracking (Crashlytics)

---

**Last Updated:** December 7, 2025  
**Project Version:** 1.0  
**Maintained by:** The Coffee Coders

