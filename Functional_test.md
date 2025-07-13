# 📱 Gesture Control Android App - Functional Test Plan
**Version**: 1.0.0  
**Last Updated**: `2025-07-13`  

---

## 🛠️ Test Environment
| Component          | Specification                       |
|--------------------|-------------------------------------|
| Test Device        | Medium Phone API 36(Amdroid16       |
| CC3200 Launchpad   | v1.2.0 (Ultrasonic Server)          |
| App Version        | 1.0.0                               |
| WiFi Network       | 5GHz, Same SSID                     |

---

## 🔍 Test Cases

### 1. 📶 Sensor Connection Test
**Objective**: Verify HTTP connection to sensor  
**Precondition**: CC3200 Launchpad running on same network  

**Steps**:
1. Launch application
2. Observe connection status card  

✅ **Expected**:  
- Status changes to "Connected" (green indicator) within 3 seconds  

📝 **Results**:  
| Actual Time | Status | Pass/Fail | Notes |
|-------------|-------------|-----------|-------|
| ` 2.3` sec  | `Connected` |    `✅`   | `____`|

---

### 2. ✋ Hand Gesture Recognition
**Objective**: Validate distance-to-volume conversion  

**Test Matrix**:
| Distance | Expected Volume | Allowed Variance |
|----------|-----------------|------------------|
| 5cm      | 0%              | ±3%              |
| 30cm     | 55%             | ±3%              | 
| 50cm     | 100%            | ±3%              |

**Results**:
| Test Run | Input | Expected | Actual | Variance | Pass/Fail |
|----------|-------|----------|--------|----------|-----------|
| 1        | 5cm   | 0%       | `_0__` | `_+2_`   | `✅`    |
| 2        | 30cm  | 55%      | `_55_` | `_±4_`   | `✅`    |

---

### 3. ⏱️ Volume Debouncing Test
**Objective**: Confirm 150ms delay between volume changes  

**Procedure**:
1. Set hand at 20cm (initial volume)
2. Rapidly oscillate hand ±5cm
3. Monitor `AudioManager` logs

✅ **Expected**:  
- Volume changes occur at ≥150ms intervals  

📝 **Results**:
Shortest interval observed: __500__ ms


---

### 4. 🚨 Error Handling Test
**Simulated Failure**: WiFi Disconnection  

**Steps**:
1. Disconnect ESP32 from WiFi
2. Observe UI for 10s  
3. Reconnect ESP32  

✅ **Expected Behavior**:
1. Immediate "Disconnected" (red) status
2. Auto-reconnect within 5s of ESP32 availability

📝 **Results**:
| Reconnect Time | Status Transition | Pass/Fail |
|----------------|--------------------|-----------|
| `__3_` sec     |     `✅`            |   `✅`    |

---

## 📊 Test Coverage Summary
| Component          | Verified Cases                  | Status |
|--------------------|---------------------------------|--------|
| **HTTP Client**    | Connection, Timeout, Recovery   | `✅/❌` |
| **Volume Logic**   | Mapping, Boundaries, Debouncing | `✅/❌` |
| **UI Components**  | Animations, Status Updates      | `✅/❌` |

---

> **Notes**:  
> - All tests conducted in controlled environment (25°C, 60% humidity)  
> - ESP32 placed 1m from test device during evaluations  
