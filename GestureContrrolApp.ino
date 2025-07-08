#include <WiFi.h>  // Include WiFi library for ESP32

#define ULTRASONIC_PIN 24  // Define ultrasonic sensor pin (Grove sensor uses a single pin)

// WiFi credentials
char ssid[]     = "BharathsIphone";
char password[] = "12345678";
WiFiServer server(80);  // Create a server on port 80 (HTTP)

// Timing variables
unsigned long lastSensorRead = 0;
const unsigned long SENSOR_INTERVAL = 150; // Interval between sensor readings (150ms for smooth control)

// Distance tracking variables
int currentDistance = -1;         // Current measured distance (-1 indicates invalid)
int lastValidDistance = -1;       // Store the last valid reading
unsigned long pulseDuration;      // Raw echo time from sensor

// Smoothing variables for stable readings
const int SMOOTHING_WINDOW = 5;
int distanceReadings[SMOOTHING_WINDOW];  // Circular buffer of past readings
int readingIndex = 0;
bool smoothingInitialized = false;

// Measure distance using single-pin Grove ultrasonic sensor
int measureDistance() {
  pinMode(ULTRASONIC_PIN, OUTPUT);
  digitalWrite(ULTRASONIC_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(ULTRASONIC_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(ULTRASONIC_PIN, LOW);
  
  pinMode(ULTRASONIC_PIN, INPUT);
  pulseDuration = pulseIn(ULTRASONIC_PIN, HIGH, 30000);  // Read pulse duration (max 30ms)

  if (pulseDuration == 0) return -1; // No echo or timeout

  int distance = pulseDuration / 58;  // Convert to centimeters

  // Reject distances outside usable range
  if (distance < 3 || distance > 70) return -1;

  return distance;
}

// Smooth readings by averaging last N values
int getSmoothedDistance() {
  int rawDistance = measureDistance();

  if (rawDistance == -1) return lastValidDistance; // Return last good value if invalid

  if (!smoothingInitialized) {
    for (int i = 0; i < SMOOTHING_WINDOW; i++) distanceReadings[i] = rawDistance;
    smoothingInitialized = true;
    lastValidDistance = rawDistance;
    return rawDistance;
  }

  // Insert new reading in circular buffer
  distanceReadings[readingIndex] = rawDistance;
  readingIndex = (readingIndex + 1) % SMOOTHING_WINDOW;

  // Compute average
  long sum = 0;
  for (int i = 0; i < SMOOTHING_WINDOW; i++) sum += distanceReadings[i];

  int smoothedDistance = sum / SMOOTHING_WINDOW;
  lastValidDistance = smoothedDistance;

  return smoothedDistance;
}

void setup() {
  Serial.begin(115200);  // Start serial console for debugging

  // Initialize smoothing buffer
  for (int i = 0; i < SMOOTHING_WINDOW; i++) distanceReadings[i] = 0;

  // Connect to WiFi
  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(200);
    Serial.print(".");
  }
  Serial.println("");
  Serial.print("WiFi connected. IP: ");
  Serial.println(WiFi.localIP());
  server.begin();

  Serial.println("ESP32 Volume Control Server Started");
  Serial.println("Volume Range: 5cm (0%) to 50cm (100%)");
}

void loop() {
  // Update distance reading at defined interval
  unsigned long currentMillis = millis();
  if (currentMillis - lastSensorRead >= SENSOR_INTERVAL) {
    currentDistance = getSmoothedDistance();
    lastSensorRead = currentMillis;

    // Print distance and calculated volume to serial for debugging
    if (currentDistance == -1) {
      Serial.println("Distance out of range");
    } else {
      int volumePercent;
      if (currentDistance <= 5) volumePercent = 0;
      else if (currentDistance >= 50) volumePercent = 100;
      else volumePercent = ((currentDistance - 5) * 100) / 45;

      Serial.print("Distance: ");
      Serial.print(currentDistance);
      Serial.print(" cm, Volume: ");
      Serial.print(volumePercent);
      Serial.println("%");
    }
  }

  // Handle client HTTP requests
  WiFiClient client = server.available();
  if (client) {
    String request = client.readStringUntil('\r');
    client.read(); // Skip '\n'

    if (request.indexOf("GET /sensor") != -1) {
      // Respond with JSON data for /sensor endpoint
      client.println("HTTP/1.1 200 OK");
      client.println("Content-Type: application/json");
      client.println("Connection: close");
      client.println("Access-Control-Allow-Origin: *");
      client.println();
      if (currentDistance == -1) {
        client.println("{\"Distance\":-1,\"status\":\"out_of_range\"}");
      } else {
        String json = "{\"Distance\":" + String(currentDistance) + ",\"status\":\"ok\"}";
        client.println(json);
      }
    }
    else if (request.indexOf("GET /") != -1) {
      // Serve HTML UI for root path
      client.println("HTTP/1.1 200 OK");
      client.println("Content-Type: text/html");
      client.println("Connection: close");
      client.println();

      client.println("<!DOCTYPE html><html><head><title>Volume Control Sensor</title>");
      client.println("<meta http-equiv='refresh' content='1'></head><body>");
      client.println("<h1>Volume Control Ultrasonic Sensor</h1>");

      if (currentDistance == -1) {
        client.println("<p>Distance: Out of range</p><p>Volume: 0%</p>");
      } else {
        int volumePercent;
        if (currentDistance <= 5) volumePercent = 0;
        else if (currentDistance >= 50) volumePercent = 100;
        else volumePercent = ((currentDistance - 5) * 100) / 45;

        client.println("<p>Distance: " + String(currentDistance) + " cm</p>");
        client.println("<p>Volume: " + String(volumePercent) + "%</p>");

        // Draw a volume bar
        client.println("<div style='background-color: #f0f0f0; width: 300px; height: 20px; border: 1px solid #ccc;'>");
        client.println("<div style='background-color: #4CAF50; width: " + String(volumePercent * 3) + "px; height: 20px;'></div>");
        client.println("</div>");
      }

      client.println("<p><a href='/sensor'>Get JSON Data</a></p>");
      client.println("<p>Volume Range: 5cm (0%) to 50cm (100%)</p>");
      client.println("</body></html>");
    }
    else {
      // Invalid path – respond with 404
      client.println("HTTP/1.1 404 Not Found");
      client.println("Connection: close");
      client.println();
    }

    client.stop(); // Close the connection
  }

  delay(10);  // Small delay to keep ESP32 stable
}

