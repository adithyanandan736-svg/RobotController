"""
Minimal reference server for the Raspberry Pi hub.
Run this on the Pi (pip install flask --break-system-packages), then the
Android app's WiFi mode can connect to http://<pi-ip>:5000

This is a STARTING POINT — wire the marked sections to your actual
Arduino Mega (servos) and ESP32 (motors) serial/WiFi calls, and your
STT -> LLM -> TTS pipeline for the /chat endpoint.
"""

from flask import Flask, request, jsonify

app = Flask(__name__)

# Track real state here instead of the app ever guessing values
robot_state = {
    "battery_pct": None,   # set this from real ADC/voltage divider reading, not a placeholder
    "mode": "manual",
    "connected": True
}


@app.route("/status", methods=["GET"])
def status():
    # Only include battery_pct if you actually have a working voltage sensor
    # wired up. If you don't yet, leave it as None -> app shows "—" honestly.
    return jsonify(robot_state)


@app.route("/command", methods=["POST"])
def command():
    data = request.get_json(force=True)
    cmd = data.get("cmd")

    if cmd == "move":
        x, y = data.get("x", 0), data.get("y", 0)
        # TODO: convert x,y into left/right track speed and send to ESP32
        # e.g. left_speed, right_speed = differential_drive(x, y)
        print(f"[MOVE] x={x} y={y}")

    elif cmd == "arm":
        side, action = data.get("side"), data.get("action")
        # TODO: send serial command to Arduino Mega, e.g. "ARM:L:UP\n"
        print(f"[ARM] {side} {action}")

    elif cmd == "head":
        action = data.get("action")
        # TODO: send serial command to Mega for neck servo
        print(f"[HEAD] {action}")

    elif cmd == "blink":
        # TODO: trigger eyelid servo on Mega
        print("[BLINK]")

    elif cmd == "mode":
        value = data.get("value")
        robot_state["mode"] = value
        # TODO: switch your on-Pi control loop to run the matching algorithm:
        # 'obstacle_avoidance' -> read ultrasonic/IR sensors, override joystick input
        # 'human_following'    -> run camera-based person detection, drive toward it
        # 'lane_following'     -> run camera-based lane detection, steer to stay centered
        print(f"[MODE] switched to {value}")

    elif cmd == "chat":
        text = data.get("text", "")
        # TODO: call your LLM here (e.g. Anthropic API) and optionally trigger
        # local TTS + speaker output on the robot itself
        reply = f"You said: {text}"  # placeholder until LLM is wired in
        return jsonify({"reply": reply})

    elif cmd == "talk_trigger":
        # TODO: manually trigger the STT->LLM->TTS pipeline (e.g. for a button
        # that starts listening instead of always-on mic)
        print("[TALK TRIGGER]")

    return jsonify({"ok": True})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
