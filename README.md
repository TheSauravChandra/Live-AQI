# SauravC Air Quality Index
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
- Live Changing Emoji & Shifting Gradient: give more articulate feedback to user, even when chart collapsed.
- For Human touch, a responsible note added.
