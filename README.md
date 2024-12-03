# Pinder - Location-Based Reminder App

## Overview
Pinder is a location-based reminder application that helps users create and manage reminders triggered by their physical location. Set reminders for when you arrive at or leave specific locations, with customizable notification radius and snooze functionality.

## Features
### Completed Features
- Location-based reminders with customizable radius
- Two types of geofencing triggers:
  - Arrive At: Notification when entering location
  - Leave At: Notification when exiting location
- Location Services:
  - Geocoding: Convert addresses to coordinates
  - Reverse Geocoding: Convert coordinates to readable addresses
  - Interactive map for location selection
  - Google Places integration for location search
- Customizable notification radius (50-10000 meters)
- Reminder management (create, edit, delete)
- Swipe-to-delete with undo functionality
- Snooze functionality for notifications
- Reminder status tracking (Active, Completed, Expired)
- Default settings configuration
- Background location monitoring
- Persistent notifications

## Device & API Requirements
- Minimum SDK: API 34 (Android 14)
- Target SDK: API 34 (Android 14)
- Supported API Levels: 34+
- Required Permissions:
  - Fine Location
  - Coarse Location
  - Background Location
  - Post Notifications

## Installation & Setup
1. Clone the repository or unzip the project files
2. Open the project in Android Studio
3. Sync project with Gradle files
4. Build and run the application

Note: The application requires Google Play Services and valid API keys for Maps and Places services.

## Testing Instructions
### Test Environments
The app has been tested on:
- Google Pixel 4a (API 34)
- Device orientation: Both portrait and landscape

### Sample Test Cases
1. **Basic Reminder Creation**
   - Launch app
   - Click '+' FAB
   - Enter reminder title and description
   - Select location via map or search
   - Set radius (try default 100m)
   - Save reminder

2. **Location Selection & Geocoding**
   - Test address search functionality
   - Test map tap for coordinate selection
   - Verify address display after coordinate selection (reverse geocoding)
   - Verify coordinate accuracy after address search (geocoding)

3. **Geofence Triggering**
   - Create reminder for nearby location
   - Select "Arrive At" trigger
   - Move to location and verify notification
   - Test "Leave At" with different location

4. **Notification Interaction**
   - Receive location-based notification
   - Test "Mark as Done" action
   - Test "Snooze" functionality
   - Verify reminder status updates

5. **Settings Configuration**
   - Access settings via toolbar menu
   - Modify default radius
   - Modify default snooze duration
   - Create new reminder to verify defaults

### Known Issues
1. Geofence monitoring may be delayed on some devices due to system battery optimization
2. Location updates may be less frequent when app is in background
3. Initial location permission requests may need app restart on some devices
4. Geocoding services require an active internet connection

## Architecture & Dependencies
- MVVM Architecture
- AndroidX Libraries
- Google Play Services (Location, Maps, Places)
- Room Database
- Kotlin Coroutines
- WorkManager for background tasks

## Build Information
- compileSdk: 34
- buildTools: Latest stable version
- Kotlin version: 1.9.10
- Gradle version: 8.5.2

## License
This project is created for academic purposes and is not licensed for commercial use.
