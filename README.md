# SauravC Air Quality Index
APK: https://github.com/TheSauravChandra/Live-AQI/blob/master/app/release/app-release.apk?raw=true

(Please use master branch for code)

App Architecture and core logic:

- OkHTTP3 used for web socket.
- ViewModel persists incoming data & it's accumulated history.
- Main Activity observes ViewModel & updates adapter,
  adapter selection determines chart should be visible or not,
  & which city's data stream it should show (selection)
- Adapter's selection callback updates: chart data input stream & graph shown of city.
- Click on Chart enlarges it & again clicking it minimizes it.
- Split Screen App support: As VM survives Config Change.(also, dark mode toggle checked.)
- Web Socket re-establishes, hit's every 1 sec & updates list & chart of failure & shifts chronology label back.
- Back press manages chart minize & visibility too.
- Net dialog is auto dismissed when Net goes On, & intermediate network issues are notified as Snackbar.
- Koin Dependency Injection used.
- Live Changing Emoji & Shifting Gradient: give more articulate feedback to user, even when chart collapsed.
- For Human touch, a responsible note added.
![Screenshot_2021-10-11-01-21-59-85_8279874d4eb854db5d1c30bc8dba05b0](https://user-images.githubusercontent.com/6492559/136711119-6e6ab94c-83b7-4267-bd4b-f9def14ee59d.jpg)

Time Taken: 1 Sunday(8hrs~)+ (Tuesday, Friday, Saturday)* 3hrs each = 17hrs.
