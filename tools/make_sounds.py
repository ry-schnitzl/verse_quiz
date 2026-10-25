#!/usr/bin/env python3
"""Generate the app's sound effects into app/src/main/res/raw.

  fanfare.wav: a short trumpet "ta-da" (C-E-G-C) played when results appear.
  click.wav:   a short tick played when an answer is submitted.

Uses only the standard library. Run from the project folder:
    python3 tools/make_sounds.py
"""

import math
import random
import struct
import wave
from pathlib import Path

RATE = 44100
OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"


def trumpet_note(freq, seconds, volume=1.0):
    """Brass-like tone: many harmonics that brighten as the note swells, plus light vibrato."""
    samples = []
    count = int(seconds * RATE)
    attack, release = 0.04, min(0.12, seconds * 0.4)
    for i in range(count):
        t = i / RATE
        if t < attack:
            env = t / attack
        elif t > seconds - release:
            env = max(0.0, (seconds - t) / release)
        else:
            env = 1.0 - 0.15 * (t - attack) / seconds
        # Vibrato fades in on longer notes.
        vibrato = 1 + 0.004 * min(1.0, t / 0.3) * math.sin(2 * math.pi * 5.5 * t)
        phase = 2 * math.pi * freq * vibrato * t
        value = 0.0
        for n in range(1, 13):
            # Higher harmonics come in as the note gets louder: the "blat" of a trumpet.
            value += (env ** (1 + n / 4)) / n ** 0.9 * math.sin(n * phase)
        samples.append(value * env * volume)
    return samples


def fanfare():
    notes = [  # (frequency, seconds)
        (523.25, 0.14),  # C5
        (659.25, 0.14),  # E5
        (783.99, 0.14),  # G5
        (1046.50, 0.75),  # C6
    ]
    out = []
    for freq, seconds in notes:
        out += trumpet_note(freq, seconds)
        out += [0.0] * int(0.015 * RATE)  # tiny gap so the notes articulate
    return out


def click():
    random.seed(7)
    count = int(0.03 * RATE)
    out = []
    for i in range(count):
        t = i / RATE
        env = math.exp(-t / 0.004)
        tone = math.sin(2 * math.pi * 2200 * t) * 0.7
        noise = (random.random() * 2 - 1) * 0.3
        out.append((tone + noise) * env)
    return out


def write(name, samples, peak=0.8):
    loudest = max(abs(s) for s in samples) or 1.0
    frames = b"".join(
        struct.pack("<h", int(s / loudest * peak * 32767)) for s in samples
    )
    path = OUT / name
    with wave.open(str(path), "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(frames)
    print(f"wrote {path} ({len(samples) / RATE:.2f}s)")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    write("fanfare.wav", fanfare(), peak=0.7)
    write("click.wav", click(), peak=0.6)
