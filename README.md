# 🎯 Gesture-Controlled Volume Adjustment  
*Using CC3200 LaunchPad + Ultrasonic Sensor + Android App*  

<img width="1493" height="856" alt="image" src="https://github.com/user-attachments/assets/62d83be7-f0d1-43e6-af5f-dd29b7ee1e9c" />


---

## 📋 Table of Contents  
- [Project Overview](#-project-overview)  
- [Hardware Requirements](#-hardware-requirements)  
- [Software Setup](#-software-setup)  
- [Installation Guide](#-installation-guide)
- [Software Configuration](#-software-configuration)
- [How to Run](#-how-to-run)  
- [Code Structure](#-code-structure)
- [Evaluation](#-evaluation)

---

## 🔍 Project Overview  
An ESP32-based system that:  
- 📏 Measures hand distance using **Grove Ultrasonic Sensor** (single-pin mode)  
- 🌐 Hosts a web server with:  
  - Real-time JSON API (`/sensor`)  
  - Visual web interface (`/`)  
- 📱 Connects to Android/iOS devices via WiFi   

**Key Components**:  
| Type       | Components                          |
|------------|-------------------------------------|
| Hardware   | CC3200 LaunchPad, HC-SR04 Sensor    |
| Software   | Energia IDE, Android Studio         |

---

## 🛠️ Hardware Requirements  
| Component               | Connection        |  
|-------------------------|-------------------|  
| ESP32 Dev Board         | Base microcontroller |  
| Grove Ultrasonic Sensor | GPIO24 (Single-pin mode) |  
| Micro-USB Cable         | Power/Programming |  

---

## 💻 Software Setup  
### 1. Energia IDE (CC3200 Programming)  
- Download [Energia IDE]([http://energia.nu/download/](https://energia.nu/download/))  
- Install CC3200 board support via **Tools → Board → Boards Manager**  

### 2. Android Studio (Companion App)  
- Install [Android Studio](https://developer.android.com/studio)  
- Open the provided `Android_app` project  

### 3. Google Forms (Survey)  
- Create your form [here]([https://forms.google.com](https://docs.google.com/forms/d/e/1FAIpQLSc81LrK-BMuI_YL7YnHIu7k_sjG8p39c47EGHD46PVpbT4Fgw/viewform?usp=header))  

---

## ⚙️ Installation Guide  
### Hardware Connections  
| Sensor Pin | CC3200 Pin |  
|------------|------------|  
| VCC        | 3.3V       |  
| GND        | GND        |  
| Trig       | P2.1       |  
| Echo       | P2.2       |  



---

## 💻 Software Configuration  
### 1. Upload Firmware  
1. Install [ESP32 board support](https://docs.espressif.com/projects/arduino-esp32/en/latest/installing.html) in Arduino IDE/Energia  
2. Copy this code to your IDE:  
   ```cpp
   #define ULTRASONIC_PIN 24  // Grove sensor on GPIO24
   char ssid[] = "YourWiFiSSID";
   char password[] = "YourWiFiPassword";

---

## 💻 How to Run
### 1. Power the CC3200 (check Serial Monitor for Wi-Fi status)

### 2. Launch Android app and verify connection

Gesture Control:

👆 Hand closer → Volume decreases

✋ Hand farther → Volume increases

---

## 💻 Code Structure

Gesture_Control_App/  
│  
├── /Energia_code/                                           # CC3200 Firmware  
│   ├── GestureContrrolApp.ino                               # Main logic + HTTP server  
│  
├── # Android Studio Project  
│   ├── MainActivity.java                                    # HTTP client for volume control  
│  
├── /GestureControlApp_Bharath Subramaniam Jayakumar.pdf/    # Presentation  
│  
└── README.md                                                # This file  
└── Use Case_GestureControlApp_Bharath_Subramaniam_Jayakumar.pdf                                              # This file  

---

## 💻 Evaluation
Help us improve by completing survey:
[https://forms.google.com](https://docs.google.com/forms/d/e/1FAIpQLSc81LrK-BMuI_YL7YnHIu7k_sjG8p39c47EGHD46PVpbT4Fgw/viewform?usp=header)
