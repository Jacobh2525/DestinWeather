 🌊 Destin Weather

A beautiful, feature-rich beach weather app for the Florida Gulf Coast. Get real-time weather, surf conditions, live radar, beach 
cams, and severe weather alerts all in one place.

![Android](https://img.shields.io/badge/Android-✓-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-✓-blue?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-✓-purple)

## 📱 Features

### 🌤️ Weather
- Real-time temperature, conditions, UV index
- Sunrise/sunset times
- Precipitation data
- Hourly and 5-day forecasts
- Pull-to-refresh everywhere

### 🏄 Surf
- Real surf data with wave height, period, and direction
- Water temperature
- Wind speed and direction
- Location-specific surf spots (Destin, Panama City, Pensacola, Miami, and more!)
- Surf condition ratings

### 🔔 Alerts
- NOAA weather alerts (tornadoes, rip currents, floods, thunderstorms)
- Push notifications for severe weather
- Location-aware alerts

### 📡 Radar
- Live NWS radar viewer
- Works directly in the app

### 📋 Detailed Forecast
- NOAA text forecast discussion
- Hourly and extended forecasts

### 📷 Beach Cams
- Live beach cam thumbnails
- Opens YouTube for live stream

### ⚙️ Settings
- Temperature units (°F / °C)
- Push notification toggle
- Dark mode toggle

## 🗺️ Locations

Preset beach cities:
- Destin, FL
- Panama City Beach, FL
- Pensacola, FL
- Fort Walton Beach, FL
- Gulf Shores, AL
- Orange Beach, AL
- Myrtle Beach, SC
- Miami, FL
- Tampa, FL
- Jacksonville, FL
- Key West, FL
- Cocoa Beach, FL
- Santa Rosa Beach, FL
- Seaside, FL
- Alys Beach, FL

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM
- **Networking**: Retrofit + OkHttp
- **APIs**: 
  - OpenWeatherMap (weather)
  - NOAA Weather.gov (forecasts, alerts, radar)
  - Surf data (marine weather)
- **Background**: WorkManager (periodic alert checks)
- **Notifications**: Local notifications with channels

## 📂 Project Structure

```
app/src/main/java/com/destinweather/
├── data/
│   ├── api/           # Retrofit API interfaces
│   └── model/         # Data models
├── ui/
│   └── screens/       # Compose screens
├── utils/             # Helper classes
├── viewmodel/         # ViewModels
├── workers/           # Background workers
└── MainActivity.kt    # Main activity
```

## 🚀 Building

1. Clone the repository
2. Open in Android Studio
3. Add your OpenWeatherMap API key in `WeatherApi.kt`
4. Build and run!

## 📄 License

MIT License
