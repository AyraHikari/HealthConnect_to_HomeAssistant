# Health Connect to Home Assistant

Easily export your Health Connect data to Home Assistant and visualize it with beautiful charts.

<p align="center">
    <img src="https://github.com/user-attachments/assets/1981aada-8c78-47b6-b5da-55b44bcaa9f7"/>
</p>

## Features
- Syncs Health Connect metrics to Home Assistant, including heart rate, sleep stages, steps, weight, body temperature, exercise sessions, blood oxygen saturation, hydration, and calories burned data.
- Background sync support with foreground service for better reliable.
- Customizable sensor name and update interval in app settings.
- Works locally — no SSL server required.
- Toggle which data categories are exported, choose the entity ID, and adjust sync ranges (1–30 days) and intervals directly from the in-app settings.
- Automatic background sync powered by WorkManager with retry logic, battery-friendly constraints, and boot persistence, plus an optional foreground service for resilient syncing.
- Manual sync controls, last sync history, and error reporting right on the home screen, backed by a home screen widget for one-tap syncing and status updates.
- Built-in stats dashboard with cached heart rate, sleep, and steps charts so you can review trends even when offline.

<h2> Screenshots</h2>

<details><summary><b>Click here to expand</b></summary>
<p float="left">
  <img src="https://github.com/user-attachments/assets/c3dcc859-1ca3-4af4-a753-6855436568ca" width="30%" />
  <img src="https://github.com/user-attachments/assets/c02e6796-2d62-482c-82c2-2af003645b74" width="30%" />
  <img src="https://github.com/user-attachments/assets/d4be983f-bb19-4c6d-9a95-290477b97cab" width="30%" />
</p>
</details>

## Download

- [Download from Pling](https://www.pling.com/p/2281233/)
- [Download from Linux Apps Store](https://www.linux-apps.com/p/2281233/)
- [Download from KDE Store](https://www.linux-apps.com/p/2281233/)

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

```yaml
type: custom:apexcharts-card
header:
  title: Heart Rate
  show: true
now:
  show: true
  label: now
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
      const todayKey = new Intl.DateTimeFormat('en-CA', {
        timeZone: hass.config.time_zone ?? 'UTC'
      }).format(new Date());
      const samples = entity.attributes.heart?.[todayKey] || {};
      return Object.values(samples)
        .map(sample => [sample.time * 1000, sample.bpm])
        .sort((a, b) => a[0] - b[0]);
yaxis:
  - min: 0
    max: 150
    decimals: 0
```

### Sleep Stats Chart

```yaml
type: custom:apexcharts-card
graph_span: 1d
header:
  show: true
  title: Sleep Stats
  show_states: true
  colorize_states: true
apex_config:
  chart:
    height: 320px
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
      const lastSleep = entity.attributes.sleep?.lastSleep;
      if (!lastSleep?.start || !lastSleep?.end) {
        return [];
      }
      return [[lastSleep.start * 1000, lastSleep.end - lastSleep.start]];
  - entity: sensor.health_connect
    name: Awake
    type: line
    color: "#9e9e9e"
    show:
      in_header: false
      legend_value: false
      as_duration: second
    extend_to: false
    float_precision: 0
    data_generator: |
      const stages = entity.attributes.sleep?.lastSleep?.stage || [];
      return stages
        .filter(stage => stage.stage === '1')
        .flatMap(stage => stage.sessions.map(session => {
          const duration = session.duration ?? (session.endTime - session.startTime);
          return [session.startTime * 1000, duration];
        }))
        .sort((a, b) => a[0] - b[0]);
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
    data_generator: |
      const stages = entity.attributes.sleep?.lastSleep?.stage || [];
      return stages
        .filter(stage => stage.stage === '4')
        .flatMap(stage => stage.sessions.map(session => {
          const duration = session.duration ?? (session.endTime - session.startTime);
          return [session.startTime * 1000, duration];
        }))
        .sort((a, b) => a[0] - b[0]);
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
    data_generator: |
      const stages = entity.attributes.sleep?.lastSleep?.stage || [];
      return stages
        .filter(stage => stage.stage === '5')
        .flatMap(stage => stage.sessions.map(session => {
          const duration = session.duration ?? (session.endTime - session.startTime);
          return [session.startTime * 1000, duration];
        }))
        .sort((a, b) => a[0] - b[0]);
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
    data_generator: |
      const stages = entity.attributes.sleep?.lastSleep?.stage || [];
      return stages
        .filter(stage => stage.stage === '6')
        .flatMap(stage => stage.sessions.map(session => {
          const duration = session.duration ?? (session.endTime - session.startTime);
          return [session.startTime * 1000, duration];
        }))
        .sort((a, b) => a[0] - b[0]);
```

### Daily Steps & Goal Tracking

```yaml
type: custom:apexcharts-card
header:
  show: true
  title: Steps (Last 14 Days)
graph_span: 14d
span:
  start: day
  offset: "-13d"
series:
  - entity: sensor.health_connect
    name: Steps
    type: column
    color: "#ff7f0e"
    extend_to: false
    float_precision: 0
    show:
      in_header: true
    data_generator: |
      const steps = entity.attributes.steps || {};
      return Object.values(steps)
        .map(entry => {
          const base = (entry?.endTime ?? entry?.startTime ?? 0) * 1000;
          if (!Number.isFinite(base) || base === 0) {
            return null;
          }
          return [base, entry?.count ?? 0];
        })
        .filter(Boolean)
        .sort((a, b) => a[0] - b[0]);
  - entity: sensor.health_connect
    type: line
    name: Goal
    color: "#607d8b"
    data_generator: |
      const dayMs = 24 * 60 * 60 * 1000;
      const anchor = new Date();
      anchor.setHours(0, 0, 0, 0);
      const base = anchor.getTime();
      return Array.from({ length: 14 }, (_, idx) => {
        const ts = base - (13 - idx) * dayMs;
        return [ts, 5000];
      });
```

### Weight & Body Composition Trend

```yaml
type: custom:apexcharts-card
header:
  title: Weight Trend
  show: true
graph_span: 30d
series:
  - entity: sensor.health_connect
    name: Weight (kg)
    type: line
    color: "#8e24aa"
    extend_to: false
    float_precision: 2
    data_generator: |
      const weightData = entity.attributes.weight || {};
      return Object.values(weightData)
        .flatMap(day => Object.entries(day).map(([timestamp, value]) => {
          const ts = Number(timestamp) * 1000;
          if (!Number.isFinite(ts) || ts === 0) {
            return null;
          }
          return [ts, value];
        }))
        .filter(Boolean)
        .sort((a, b) => a[0] - b[0]);
```

### Blood Oxygen Saturation

```yaml
type: custom:apexcharts-card
header:
  title: SpO₂ Readings
  show: true
graph_span: 7d
apex_config:
  chart:
    height: 300px
series:
  - entity: sensor.health_connect
    name: SpO₂ (%)
    type: area
    color: "#03a9f4"
    extend_to: false
    float_precision: 1
    show:
      in_header: true
    data_generator: |
      const oxygenData = entity.attributes.oxygen || {};
      return Object.values(oxygenData)
        .flatMap(day => Object.entries(day).map(([timestamp, value]) => {
          const ts = Number(timestamp) * 1000;
          if (!Number.isFinite(ts) || ts === 0) {
            return null;
          }
          return [ts, value];
        }))
        .filter(Boolean)
        .sort((a, b) => a[0] - b[0]);
yaxis:
  - min: 80
    max: 100
    decimals: 0
```

### Hydration Log

```yaml
type: custom:apexcharts-card
header:
  title: Hydration (ml)
  show: true
graph_span: 7d
span:
  start: day
  offset: "-6d"
series:
  - entity: sensor.health_connect
    name: Intake
    type: column
    color: "#4fc3f7"
    extend_to: false
    float_precision: 0
    data_generator: |
      const hydration = entity.attributes.hydration || {};
      return Object.values(hydration)
        .map(entry => {
          const base = (entry?.endTime ?? entry?.startTime ?? 0) * 1000;
          if (!Number.isFinite(base) || base === 0) {
            return null;
          }
          return [base, entry?.volume ?? 0];
        })
        .filter(Boolean)
        .sort((a, b) => a[0] - b[0]);
```

### Exercise Session Timeline

```yaml
type: custom:apexcharts-card
header:
  title: Exercise Duration
  show: true
graph_span: 14d
apex_config:
  chart:
    type: area
    height: 320px
series:
  - entity: sensor.health_connect
    name: Session Minutes
    type: area
    color: "#43a047"
    extend_to: false
    float_precision: 0
    data_generator: |
      const exercise = entity.attributes.exercise || {};
      const minutes = Object.values(exercise)
        .flatMap(day => (day.sessions || []).map(session => [session.startTime * 1000, session.duration / 60]));
      return minutes.sort((a, b) => a[0] - b[0]);
  - entity: sensor.health_connect
    name: Total Daily Duration
    type: column
    color: "#66bb6a"
    extend_to: false
    float_precision: 0
    data_generator: |
      const exercise = entity.attributes.exercise || {};
      return Object.entries(exercise)
        .map(([date, data]) => {
          if (!data?.totalDuration) {
            return null;
          }
          const firstSession = data.sessions?.[0]?.startTime;
          const fallbackMs = date && date !== 'unknown' ? Date.parse(`${date}T00:00:00Z`) : NaN;
          const baseMs = Number.isFinite(firstSession) ? firstSession * 1000 : fallbackMs;
          if (!Number.isFinite(baseMs) || baseMs === 0) {
            return null;
          }
          return [baseMs, (data.totalDuration || 0) / 60];
        })
        .filter(Boolean)
        .sort((a, b) => a[0] - b[0]);
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
      return Object.values(data)
        .map(item => {
          const base = (item?.endTime ?? item?.startTime ?? 0) * 1000;
          if (!Number.isFinite(base) || base === 0) {
            return null;
          }
          return [base, item?.energy ?? 0];
        })
        .filter(Boolean)
        .sort((a, b) => a[0] - b[0]);
```

</details>

## License

This project is licensed under the [GNU General Public License v3.0 (GPLv3)](https://github.com/AyraHikari/HealthConnect_to_HomeAssistant/blob/master/LICENSE).
