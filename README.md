# Health Connect to Home Assistant

Easily export your Health Connect data to Home Assistant and visualize it with beautiful charts.

## 🚀 Features
- Syncs heart rate, sleep, steps, weight
- Background sync support
- Customizable sensor name and update interval

## 📦 How to Use
1. Generate a Home Assistant Access Token

- Log in to your Home Assistant instance.

- Click your profile (top right corner).

- Go to the Security tab.

- Scroll to the bottom and click Create Token.

- Name it something like Health Connect, then copy the token.

2. Set Up Health Connect to Home Assistant

- Enter your Home Assistant URL.

- Paste your Access Token into the token field.

- (Optional) Customize the sensor entity ID and sync interval.

3. Log In & Grant Permissions

- Click Login and confirm it's connected successfully.

- Click Grant Permission:

  - Toggle Allow All.

  - Confirm all requested permissions.

  - Allow background sync if prompted.

4. Sync Your Data

- The app will automatically sync in the background.

- You can also tap Manual Sync to trigger it immediately.

5. (Optional) Check Home Assistant Entities

- Go to Developer Tools > States in Home Assistant.

- Search for sensor.health_connect to see your synced data.

# 📊 Home Assistant Card (Heart Rate Example)

- Prerequisite

  - Install apexcharts-card via HACS.

  - Refresh your browser after install.

YAML Configuration
```yaml
type: custom:apexcharts-card
header:
  title: Heart Rate
  show: true
graph_span: 24h
span:
  start: day
  offset: +0h
apex_config:
  chart:
    height: 300px
  stroke:
    curve: smooth
    width: 2
series:
  - entity: sensor.health_connect
    name: Heart Rate (BPM)
    color: "#e6550d"
    type: line
    extend_to: false
    float_precision: 0
    data_generator: |
      return entity.attributes.heart.heartRateSamples.map(sample => {
        return [new Date(sample.time * 1000), sample.bpm];
      });
yaxis:
  - min: 0
    max: 150
    decimals: 0
```

Example Result:

![test](https://github.com/user-attachments/assets/9cf7837a-4ba3-4efe-84ae-1ae1bd5ceae0)

