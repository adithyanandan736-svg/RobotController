# Robot Controller — Android App

Controls your robot over WiFi or Bluetooth: movement joystick, arm/head
buttons, eye blink, mode switching (manual / obstacle avoidance / human
following / lane following), and a typing AI chat panel that speaks replies
back in a male voice.

## What's real vs what you still need to wire up

- The app is fully functional UI + networking code.
- `pi_server_reference.py` is a **starting skeleton** for the Raspberry Pi
  side — it defines the exact API the app expects, but the TODO sections
  (motor control, servo control, obstacle/following/lane algorithms, LLM
  call) need to be filled in with your actual hardware code.
- Battery percentage only shows if your Pi server actually reports
  `battery_pct` from a real voltage reading — the app will show "—" instead
  of guessing, as requested.
- Male voice: the app tries to auto-pick a male-sounding voice from your
  phone's installed TTS voices. Voice gender isn't reliably labeled by
  Android across all phones/engines, so if the auto-pick sounds wrong, use
  `TtsHelper.listVoiceNames()` / `setVoiceByName()` to add a manual picker —
  the code for this is already there, just needs a small dropdown UI added
  if the automatic guess isn't right on your phone.

---

## How to turn this into an installable APK (free, ~15–20 minutes)

### 1. Install Android Studio (free)
Download from **https://developer.android.com/studio** — pick your OS
(Windows/Mac/Linux), install normally. This is Google's official, free IDE
for Android — no paid account needed to build and install your own APK.

### 2. Get the project onto your computer
You have this project as a folder called `RobotController`. Copy the whole
folder to your computer (e.g. Desktop).

### 3. Open the project
- Launch Android Studio
- Click **Open** (not "New Project")
- Select the `RobotController` folder
- Android Studio will start "Gradle Sync" automatically — this downloads
  the build tools it needs. **This step requires internet** and can take
  5–15 minutes the first time. Just wait for it to finish (progress bar at
  the bottom).

### 4. Fix any sync prompts
If Android Studio shows a banner asking to "update Gradle" or "install
missing SDK platform," click the suggested fix button — these are normal
one-time setup prompts.

### 5. Build the APK
- Once sync finishes (no red errors in the bottom bar), go to the top menu:
  **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- Wait for it to finish (a notification pops up bottom-right: "APK(s)
  generated successfully")
- Click **locate** in that notification — it opens the folder containing
  `app-debug.apk`

### 6. Install it on your phone
- Copy `app-debug.apk` to your phone (via USB cable, or upload to Google
  Drive/WhatsApp yourself and download on the phone)
- On your phone, tap the file to install — Android will warn about
  "installing from unknown sources" the first time; allow it (this warning
  is normal for any app not from the Play Store, not a sign of a problem)
- Open the app, grant the permissions it asks for (microphone, Bluetooth,
  location — location is required by Android for Bluetooth scanning, not
  used for tracking)

### 7. Connect it to your robot
- Make sure your phone and Raspberry Pi are on the **same WiFi network**
- On the Pi, run: `pip install flask --break-system-packages` then
  `python3 pi_server_reference.py`
- Find the Pi's IP address (`hostname -I` on the Pi terminal)
- In the app, select **WiFi**, type the Pi's IP followed by `:5000`
  (e.g. `192.168.1.42:5000`), tap **Connect**

---

## If Android Studio's Gradle sync fails (common on first install)

- Make sure you're connected to the internet (it downloads Gradle + SDK
  components the first time)
- If it's stuck, go to **File → Invalidate Caches / Restart**
- If a specific SDK version is missing, Android Studio shows a blue link
  to install it directly — click it

## Adding your own robot build photos

The "About This Robot" section has 3 empty photo slots (`imgPart1`, `imgPart2`,
`imgPart3` in `activity_main.xml`). To show your actual robot instead of
empty boxes:

1. Take photos of your robot, resize them smaller (under 500KB each is
   plenty — full camera resolution isn't needed and bloats the app)
2. Rename them `robot_photo_1.jpg`, `robot_photo_2.jpg`, `robot_photo_3.jpg`
3. Place them in `app/src/main/res/drawable/` in the project
4. In `MainActivity.kt`, inside `onCreate()`, add:
   ```kotlin
   binding.imgPart1.setImageResource(R.drawable.robot_photo_1)
   binding.imgPart2.setImageResource(R.drawable.robot_photo_2)
   binding.imgPart3.setImageResource(R.drawable.robot_photo_3)
   ```
5. Commit and push — the next GitHub Actions build will include them

## Chat & voice assistant

- The chat panel now shows proper message bubbles (yours on the right,
  the assistant's on the left) with a "typing..." indicator while waiting
  for a reply from the Pi.
- On launch, the assistant introduces itself and speaks the intro aloud
  (male-leaning voice, see the male voice note earlier in this file).
- The reply text always comes from the Pi's `/chat` endpoint — the app
  itself doesn't generate replies, so make sure `pi_server_reference.py`
  has its LLM call wired in for real conversations rather than the
  placeholder echo response.

## Next steps once the basic app works

- Fill in the TODO sections in `pi_server_reference.py` to actually drive
  your motors/servos and connect your STT→LLM→TTS pipeline
- Obstacle avoidance / human following / lane following all need a camera
  or sensors on the Pi — the app only sends "switch to this mode," the Pi
  has to run the actual detection logic

