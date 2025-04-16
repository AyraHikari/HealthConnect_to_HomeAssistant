# Health Connect to Home Assistant

Easily export your Health Connect data to Home Assistant and visualize it with beautiful charts.

<p align="center">
    <img src="https://github.com/user-attachments/assets/67819701-32a4-46db-a9e6-33cddd0bc3eb"/>
</p>

## Features
- Syncs heart rate, sleep, steps, weight
- Background sync support
- Customizable sensor name and update interval
- Works locally — no SSL server required

<h2>📸 Screenshots</h2>

<details><summary><b>Click here to expand</b></summary>
<p float="left">
  <img src="https://github.com/user-attachments/assets/24e2ef96-fb43-4d98-90b4-e46f5d9e103d" width="30%" />
  <img src="https://github.com/user-attachments/assets/19631fca-6330-47ec-b3c5-37cdc0bad1fd" width="30%" />
</p>
</details>

## Download

- [Download from Pling](https://www.pling.com/p/2280404/)

## How to Use

<details><summary><b>Click here to expand</b></summary>
  
### 1. Generate a Home Assistant Access Token

- Log in to your Home Assistant instance.

- Click your profile (top right corner).

- Go to the Security tab.

- Scroll to the bottom and click Create Token.

- Name it something like Health Connect, then copy the token.

### 2. Set Up Health Connect to Home Assistant

- Enter your Home Assistant URL.

- Paste your Access Token into the token field.

- (Optional) Customize the sensor entity ID and sync interval.

### 3. Log In & Grant Permissions

- Click Login and confirm it's connected successfully.

- Click Grant Permission:

  - Toggle Allow All.

  - Confirm all requested permissions.

  - Allow background sync if prompted.

### 4. Sync Your Data

- The app will automatically sync in the background.

- You can also tap Manual Sync to trigger it immediately.

### 5. (Optional) Check Home Assistant Entities

- Go to Developer Tools > States in Home Assistant.

- Search for sensor.health_connect to see your synced data.
</details>

## Home Assistant Card Example

<details><summary><b>Click here to expand</b></summary>
  
### Prerequisite

  - Install [apexcharts-card](https://github.com/RomRider/apexcharts-card) via HACS or manual.

  - Refresh your browser after install.

### Heart Rate Chart

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
      const date = new Date();
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const key = `${year}-${month}-${day}`;
      const samples = entity.attributes.heart?.[key] || {};
      return Object.values(samples).map(sample => {
        return [sample.time * 1000, sample.bpm];
      }).sort((a, b) => a[0] - b[0]);
yaxis:
  - min: 0
    max: 150
    decimals: 0
```

### Sleep Stats Chart

YAML Configuration
```yaml
type: custom:apexcharts-card
graph_span: 1d
header:
  show: true
  title: Sleep Stats
  show_states: true
  colorize_states: true
all_series_config:
  stroke_width: 10
series:
  - entity: sensor.health_connect
    name: Total Sleep
    color: "#d62728"
    show:
      in_chart: false
      in_header: true
      as_duration: second
    extend_to: false
    float_precision: 0
    data_generator: |
      const date = new Date();
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate() - 1).padStart(2, '0'); // Yesterday's
      const key = `${year}-${month}-${day}`;
      const sleep = entity.attributes.sleep?.[key] || {};
      if (sleep.start && sleep.end) {
        return [[sleep.start * 1000, sleep.end - sleep.start]];
      }
      return [];
  - entity: sensor.health_connect
    name: Awake
    type: line
    color: grey
    show:
      in_header: false
      legend_value: false
      as_duration: second
    extend_to: false
    float_precision: 0
    data_generator: >
      const date = new Date();

      const year = date.getFullYear();

      const month = String(date.getMonth() + 1).padStart(2, '0');

      const day = String(date.getDate() - 1).padStart(2, '0'); // Yesterday's
      data

      const key = `${year}-${month}-${day}`;

      const sleepStages = entity.attributes.sleep?.[key]?.stage || [];

      let result = [];

      sleepStages.forEach(stage => {
        if (stage.stage === "1") {
          stage.sessions.forEach(session => {
            result.push([session.startTime * 1000, stage.totalTime]);
            result.push([session.endTime * 1000, stage.totalTime]);
          });
        }
      });

      return result.sort((a, b) => a[0] - b[0]);
  - entity: sensor.health_connect
    name: Light Sleep
    type: line
    color: "#1f77b4"
    show:
      in_header: false
      legend_value: false
      as_duration: second
    extend_to: false
    float_precision: 0
    data_generator: >
      const date = new Date();

      const year = date.getFullYear();

      const month = String(date.getMonth() + 1).padStart(2, '0');

      const day = String(date.getDate() - 1).padStart(2, '0'); // Yesterday's
      data

      const key = `${year}-${month}-${day}`;

      const sleepStages = entity.attributes.sleep?.[key]?.stage || [];

      let result = [];

      sleepStages.forEach(stage => {
        if (stage.stage === "4") {
          stage.sessions.forEach(session => {
            result.push([session.startTime * 1000, stage.totalTime]);
            result.push([session.endTime * 1000, stage.totalTime]);
          });
        }
      });

      return result.sort((a, b) => a[0] - b[0]);
  - entity: sensor.health_connect
    name: Deep Sleep
    type: line
    color: "#2ca02c"
    extend_to: false
    show:
      in_header: false
      legend_value: false
      as_duration: second
    float_precision: 0
    data_generator: >
      const date = new Date();

      const year = date.getFullYear();

      const month = String(date.getMonth() + 1).padStart(2, '0');

      const day = String(date.getDate() - 1).padStart(2, '0'); // Yesterday's
      data

      const key = `${year}-${month}-${day}`;

      const sleepStages = entity.attributes.sleep?.[key]?.stage || [];

      let result = [];

      sleepStages.forEach(stage => {
        if (stage.stage === "5") {
          stage.sessions.forEach(session => {
            result.push([session.startTime * 1000, stage.totalTime]);
            result.push([session.endTime * 1000, stage.totalTime]);
          });
        }
      });

      return result.sort((a, b) => a[0] - b[0]);
  - entity: sensor.health_connect
    name: REM
    type: line
    color: "#9467bd"
    extend_to: false
    show:
      in_header: false
      legend_value: false
      as_duration: second
    float_precision: 0
    data_generator: >
      const date = new Date();

      const year = date.getFullYear();

      const month = String(date.getMonth() + 1).padStart(2, '0');

      const day = String(date.getDate() - 1).padStart(2, '0'); // Yesterday's
      data

      const key = `${year}-${month}-${day}`;

      const sleepStages = entity.attributes.sleep?.[key]?.stage || [];

      let result = [];

      sleepStages.forEach(stage => {
        if (stage.stage === "6") {
          stage.sessions.forEach(session => {
            result.push([session.startTime * 1000, stage.totalTime]);
            result.push([session.endTime * 1000, stage.totalTime]);
          });
        }
      });

      return result.sort((a, b) => a[0] - b[0]);
```

### Calories Burned Chart

```yaml
type: custom:apexcharts-card
header:
  title: Calories Burned
  show: true
graph_span: 7d
span:
  start: day
  offset: "-6d"
apex_config:
  chart:
    height: 300px
  stroke:
    curve: smooth
    width: 2
series:
  - entity: sensor.health_connect
    name: Calories Burned
    color: "#d62728"
    type: line
    extend_to: false
    float_precision: 0
    show:
      in_header: true
    data_generator: |
      const data = entity.attributes.calories || {};
      return Object.entries(data).map(([date, item]) => {
        const timestamp = item.startTime * 1000;
        return [timestamp, item.energy];
      }).sort((a, b) => a[0] - b[0]);
```

</details>

## 📝 License

This project is licensed under the [GNU General Public License v3.0 (GPLv3)](https://github.com/AyraHikari/HealthConnect_to_HomeAssistant/blob/master/LICENSE).
