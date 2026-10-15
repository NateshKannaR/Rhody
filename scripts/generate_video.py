#!/usr/bin/env python3
"""
Rhody — 2-Minute Product Video Generator
Explains:
1. What's the problem (Parent anxiety, generic GPS lack of bike metrics, high hardware telemetry cost)
2. How Rhody solves it (Turns smartphone into cockpit HUD, IMU lean angle, 0-100 sprint timing, Guardian live safety)
3. How it works (Cockpit HUD, Parent 1-click tracking link, Post-ride analytics, Zero-API cloud sync)
"""

import os
import sys
import math
import subprocess
import numpy as np
from PIL import Image, ImageDraw, ImageFont

WIDTH = 1280
HEIGHT = 720
FPS = 25
TOTAL_SECONDS = 120
TOTAL_FRAMES = TOTAL_SECONDS * FPS

# Font paths
FONT_REGULAR = '/usr/share/fonts/noto/NotoSans-Regular.ttf'
FONT_BOLD = '/usr/share/fonts/liberation/LiberationSans-Bold.ttf'
FONT_MONO = '/usr/share/fonts/liberation/LiberationMono-Bold.ttf'

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()

# Colors
C_DARK = (10, 12, 16)
C_CARD = (18, 22, 31)
C_CARD_HOVER = (26, 32, 45)
C_BORDER = (31, 38, 54)
C_BORDER_LIGHT = (45, 55, 77)
C_BLUE = (37, 99, 235)
C_BLUE_LIGHT = (96, 165, 250)
C_EMERALD = (16, 185, 129)
C_AMBER = (245, 158, 11)
C_ROSE = (239, 68, 68)
C_TEXT = (226, 232, 240)
C_MUTED = (148, 163, 184)
C_WHITE = (255, 255, 255)

# 7 Chapters definition
# Total: 14 + 21 + 19 + 19 + 20 + 15 + 12 = 120 seconds
SCENES = [
    {
        'id': 1,
        'title': 'THE VISION',
        'chapter': 'CHAPTER 1 / 7',
        'duration': 14.0,
        'voice_text': 'Motorcycling gives us unmatched freedom on the open road. But riders face unique safety challenges, and families back home are left with worry and unanswered questions. Welcome to Rhody.',
        'subtitle': 'Motorcycling gives us freedom on the road, but riders face safety challenges, and families are left with worry. Welcome to Rhody.'
    },
    {
        'id': 2,
        'title': 'THE PROBLEM',
        'chapter': 'CHAPTER 2 / 7',
        'duration': 21.0,
        'voice_text': 'When you head out on a ride, your parents worry. Standard navigation apps do not tell them if you are moving or stopped. Apps like Google Maps do not measure motorcycle lean angles or acceleration. And dedicated racing telemetry hardware costs over five hundred dollars.',
        'subtitle': 'The Problem: Parents worry with zero visibility. Generic GPS lacks lean angles and acceleration splits. Dedicated hardware telemetry costs over $500.'
    },
    {
        'id': 3,
        'title': 'THE SOLUTION',
        'chapter': 'CHAPTER 3 / 7',
        'duration': 19.0,
        'voice_text': 'Rhody changes the game. By mounting your existing Android smartphone to your handlebars, Rhody turns internal IMU sensors into an executive telemetry cockpit. It tracks your lean angle, measures standing sprints, and gives your parents real-time safety visibility.',
        'subtitle': 'The Solution: Mount your phone on your handlebar. Rhody measures lean angle, 0–100 km/h sprints, and gives parents real-time live safety visibility.'
    },
    {
        'id': 4,
        'title': 'COCKPIT & SPRINTS',
        'chapter': 'CHAPTER 4 / 7',
        'duration': 19.0,
        'voice_text': 'Step one: Mount your phone and start your ride. The Cockpit HUD provides high-contrast speed, peak velocity, and dual-axis G-force. Dynamic sprint timers automatically calculate zero to sixty and zero to one hundred kilometer per hour standing launches.',
        'subtitle': 'Step 1: Cockpit HUD & Sprint Timers. High-contrast digital speedometer, dual-axis G-force radar, and automatic 0–60 & 0–100 km/h standing sprint timers.'
    },
    {
        'id': 5,
        'title': 'GUARDIAN LIVE SAFETY',
        'chapter': 'CHAPTER 5 / 7',
        'duration': 20.0,
        'voice_text': 'Step two: Guardian Parent Tracking. With one tap, share a private link with your parents. Without installing any app or logging in, parents open their browser to see if your bike is moving or stopped, your current speed, and your exact location on a live map.',
        'subtitle': 'Step 2: Guardian Parent Tracking. 1-tap private link. Parents view live speed, bike moving vs stopped status, and GPS location in any browser with zero login.'
    },
    {
        'id': 6,
        'title': 'LEAN ANGLE & CLOUD',
        'chapter': 'CHAPTER 6 / 7',
        'duration': 15.0,
        'voice_text': 'Step three: After completing your trip, inspect your ride telemetry. View your maximum lean angle on left and right cornering, speed graphs, elevation changes, or replay your entire route on the Rhody cloud companion.',
        'subtitle': 'Step 3: Post-Ride Telemetry. IMU cornering lean angle (42° L / 38° R), elevation profiles, GPX exports, and zero-API cloud replay.'
    },
    {
        'id': 7,
        'title': 'GET RHODY TODAY',
        'chapter': 'CHAPTER 7 / 7',
        'duration': 12.0,
        'voice_text': 'Rhody is available now as a free Android application. Download the APK from GitHub Releases or rhodyy.vercel.app. Ride with passion, and return with peace of mind.',
        'subtitle': 'Get Rhody Today: 100% Free & Open Source Android App. Download APK at rhodyy.vercel.app/apk. Ride with passion, return with peace of mind.'
    }
]

# Precompute cumulative start and end times for each scene
accum = 0.0
for s in SCENES:
    s['start_sec'] = accum
    s['end_sec'] = accum + s['duration']
    accum = s['end_sec']

def synthesize_audio():
    print('[1/4] Synthesizing speech narration audio for all 7 scenes...')
    os.makedirs('/tmp/rhody_video_build', exist_ok=True)
    padded_wavs = []
    
    for s in SCENES:
        raw_wav = f"/tmp/rhody_video_build/scene_{s['id']}_raw.wav"
        padded_wav = f"/tmp/rhody_video_build/scene_{s['id']}_padded.wav"
        
        # Synthesize with espeak-ng en-us voice
        cmd_synth = ['espeak-ng', '-v', 'en-us+m3', '-s', '142', '-p', '48', '-a', '100', '-w', raw_wav, s['voice_text']]
        subprocess.run(cmd_synth, check=True)
        
        # Pad with silence to match exact scene duration
        cmd_pad = ['ffmpeg', '-y', '-i', raw_wav, '-af', 'apad', '-t', f"{s['duration']:.2f}", padded_wav]
        subprocess.run(cmd_pad, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
        padded_wavs.append(padded_wav)
        
    # Concatenate all scene wavs into one 120s master audio
    concat_list = '/tmp/rhody_video_build/concat.txt'
    with open(concat_list, 'w') as f:
        for p in padded_wavs:
            f.write(f"file '{p}'\n")
            
    master_wav = '/tmp/rhody_video_build/master_speech.wav'
    subprocess.run(['ffmpeg', '-y', '-f', 'concat', '-safe', '0', '-i', concat_list, '-c', 'copy', master_wav], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
    
    # Generate subtle harmonic synth drone in A minor (110Hz + 220Hz + 330Hz) mixed at low volume
    final_audio = '/tmp/rhody_video_build/final_soundtrack.wav'
    cmd_bg = [
        'ffmpeg', '-y',
        '-i', master_wav,
        '-f', 'lavfi', '-i', 'sine=frequency=110:duration=120,volume=0.03',
        '-filter_complex', '[0:a][1:a]amix=inputs=2:duration=first:dropout_transition=2,volume=1.3[out]',
        '-map', '[out]',
        final_audio
    ]
    subprocess.run(cmd_bg, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
    print('  ✓ Audio synthesized and mastered to 120.0s')
    return final_audio

def load_and_prepare_screenshots():
    shots = {}
    base_dir = '/home/natesh/Downloads/Rhody/screenshots'
    for name in ['dashboard.jpg', 'live_trip.jpg', 'analytics.jpg']:
        path = os.path.join(base_dir, name)
        if os.path.exists(path):
            img = Image.open(path).convert('RGB')
            # Resize preserving aspect ratio to fit inside 320x560
            img.thumbnail((320, 560), Image.Resampling.LANCZOS)
            shots[name] = img
    return shots

def draw_header_and_chrome(draw, current_sec, scene):
    # Top progress bar
    progress_w = int((current_sec / TOTAL_SECONDS) * WIDTH)
    draw.rectangle([0, 0, WIDTH, 3], fill=C_BORDER)
    draw.rectangle([0, 0, progress_w, 3], fill=C_BLUE_LIGHT)

    # Top Brand Bar
    draw.rounded_rectangle([32, 16, 170, 50], radius=8, fill=C_CARD, outline=C_BORDER)
    f_brand = get_font(FONT_BOLD, 16)
    draw.text((44, 23), "⚡ RHODY", font=f_brand, fill=C_BLUE_LIGHT)
    f_sub = get_font(FONT_REGULAR, 11)
    draw.text((120, 26), "v1.0", font=f_sub, fill=C_EMERALD)

    # Tagline
    f_tag = get_font(FONT_REGULAR, 12)
    draw.text((185, 26), "Precision Motorcycle Telemetry & Guardian Live Safety", font=f_tag, fill=C_MUTED)

    # Top Right Chapter & Timer
    ch_text = f"{scene['chapter']} • {scene['title']}"
    f_ch = get_font(FONT_BOLD, 12)
    bbox_ch = f_ch.getbbox(ch_text)
    ch_w = bbox_ch[2] - bbox_ch[0]
    
    mins = int(current_sec) // 60
    secs = int(current_sec) % 60
    time_text = f"{mins}:{secs:02d} / 2:00"
    
    draw.rounded_rectangle([WIDTH - ch_w - 140, 16, WIDTH - 32, 50], radius=8, fill=C_CARD, outline=C_BORDER)
    draw.text((WIDTH - ch_w - 125, 25), ch_text, font=f_ch, fill=C_BLUE_LIGHT)
    draw.text((WIDTH - 110, 25), time_text, font=f_ch, fill=C_WHITE)

def draw_subtitle_bar(draw, scene):
    sub_y1 = HEIGHT - 76
    sub_y2 = HEIGHT - 18
    draw.rounded_rectangle([32, sub_y1, WIDTH - 32, sub_y2], radius=10, fill=C_CARD, outline=C_BORDER)
    
    f_label = get_font(FONT_BOLD, 11)
    draw.text((48, sub_y1 + 8), "NARRATION", font=f_label, fill=C_BLUE_LIGHT)
    
    f_text = get_font(FONT_REGULAR, 14)
    sub_str = scene['subtitle']
    if len(sub_str) > 110:
        split_idx = sub_str.rfind(' ', 0, 105)
        line1 = sub_str[:split_idx]
        line2 = sub_str[split_idx+1:]
        draw.text((48, sub_y1 + 24), line1, font=f_text, fill=C_TEXT)
        draw.text((48, sub_y1 + 42), line2, font=f_text, fill=C_MUTED)
    else:
        draw.text((48, sub_y1 + 28), sub_str, font=f_text, fill=C_TEXT)

def draw_phone_mockup(img, shot_img, x, y):
    w, h = shot_img.size
    pad = 8
    draw = ImageDraw.Draw(img)
    # Outer device bezel
    draw.rounded_rectangle([x - pad, y - pad, x + w + pad, y + h + pad], radius=18, fill=(18, 22, 31), outline=(45, 55, 77), width=2)
    # Speaker notch
    cx = x + w // 2
    draw.rounded_rectangle([cx - 24, y - pad + 3, cx + 24, y - pad + 6], radius=2, fill=(35, 43, 60))
    # Paste screenshot
    img.paste(shot_img, (x, y))

def render_scene_1(img, draw, t_scene, current_sec, scene):
    cx = WIDTH // 2
    
    # Animated radar pulse waves in center
    pulse_r = 60 + int((t_scene % 2.5) * 60)
    for r_offset in [0, 45, 90]:
        r = (pulse_r + r_offset) % 180 + 30
        draw.ellipse([cx - r, 250 - r, cx + r, 250 + r], outline=(37, 99, 235), width=1)
        
    # Center Brand Emblem Card
    draw.rounded_rectangle([cx - 380, 130, cx + 380, 370], radius=16, fill=C_CARD, outline=C_BORDER, width=2)
    
    # Badge
    f_tag = get_font(FONT_BOLD, 12)
    draw.rounded_rectangle([cx - 160, 150, cx + 160, 180], radius=15, fill=(37, 99, 235), outline=C_BLUE)
    draw.text((cx - 138, 156), "OFFICIAL MOTORCYCLE TELEMETRY", font=f_tag, fill=C_WHITE)
    
    f_title = get_font(FONT_BOLD, 46)
    draw.text((cx - 110, 192), "RHODY", font=f_title, fill=C_WHITE)
    
    f_tagline = get_font(FONT_REGULAR, 18)
    tag_str = "Precision Motorcycle Telemetry & Guardian Live Safety"
    bbox = f_tagline.getbbox(tag_str)
    tw = bbox[2] - bbox[0]
    draw.text((cx - tw // 2, 260), tag_str, font=f_tagline, fill=C_TEXT)
    
    f_desc = get_font(FONT_REGULAR, 14)
    desc_str = "Zero expensive track hardware • Native Android IMU sensor fusion • Real-time parent safety beacon"
    bbox_d = f_desc.getbbox(desc_str)
    td = bbox_d[2] - bbox_d[0]
    draw.text((cx - td // 2, 295), desc_str, font=f_desc, fill=C_MUTED)

    # 3 Pill Badges below
    badges = [
        ("📱 Turn Smartphone to Cockpit", C_BLUE_LIGHT),
        ("🛡️ 1-Click Parent Live Tracking", C_EMERALD),
        ("🏎️ 0-100 Sprints & Lean Angle", C_AMBER)
    ]
    bx = cx - 380
    bw = 245
    for i, (btext, bcol) in enumerate(badges):
        cur_bx = bx + i * 260
        draw.rounded_rectangle([cur_bx, 400, cur_bx + bw, 445], radius=10, fill=C_CARD, outline=C_BORDER)
        fb = get_font(FONT_BOLD, 12)
        draw.text((cur_bx + 14, 415), btext, font=fb, fill=bcol)

def render_scene_2(img, draw, t_scene, current_sec, scene):
    f_head = get_font(FONT_BOLD, 26)
    draw.text((48, 80), "THE PROBLEM: WHY MOTORCYCLISTS & PARENTS STRUGGLE", font=f_head, fill=C_WHITE)
    f_sub = get_font(FONT_REGULAR, 14)
    draw.text((48, 115), "Traditional navigation tools leave massive blind spots for safety, dynamics, and budget.", font=f_sub, fill=C_MUTED)

    cards = [
        {
            'icon': '🚨',
            'title': '1. Constant Parent Anxiety',
            'desc': 'When riders head out for weekend rides, night commutes, or mountain passes, parents and loved ones worry with zero visibility. Phone calls are dangerous while riding, leaving families in anxious silence.',
            'tag': 'ZERO LIVE VISIBILITY',
            'border': C_ROSE,
            'tag_bg': (50, 18, 24)
        },
        {
            'icon': '📉',
            'title': '2. Generic GPS Inadequacy',
            'desc': 'Standard apps like Google Maps and fitness trackers do NOT record motorcycle-specific dynamics: no lean angle cornering detection, no standing 0-100 sprint split timers, and no bike moving vs stopped indicators.',
            'tag': 'NO MOTORCYCLE TELEMETRY',
            'border': C_AMBER,
            'tag_bg': (50, 38, 15)
        },
        {
            'icon': '💸',
            'title': '3. Expensive Track Hardware',
            'desc': 'Dedicated motorsport data-loggers (AiM, Racebox, Starlane) cost $400 to $1,000+ with bulky wiring and complex installation, making precision ride analysis unaffordable for everyday motorcyclists.',
            'tag': 'PROHIBITIVE $500+ COST',
            'border': C_BLUE,
            'tag_bg': (18, 30, 55)
        }
    ]

    card_w = 370
    card_h = 360
    y = 150
    for i, c in enumerate(cards):
        x = 48 + i * 405
        cur_y = y
        draw.rounded_rectangle([x, cur_y, x + card_w, cur_y + card_h], radius=14, fill=C_CARD, outline=c['border'], width=2)
        
        # Tag
        draw.rounded_rectangle([x + 20, cur_y + 20, x + 210, cur_y + 48], radius=6, fill=c['tag_bg'], outline=c['border'])
        f_tag = get_font(FONT_BOLD, 10)
        draw.text((x + 30, cur_y + 27), c['tag'], font=f_tag, fill=c['border'])
        
        # Title
        f_title = get_font(FONT_BOLD, 17)
        draw.text((x + 20, cur_y + 65), c['title'], font=f_title, fill=C_WHITE)
        
        # Description
        f_desc = get_font(FONT_REGULAR, 13)
        words = c['desc'].split(' ')
        lines = []
        cur_line = []
        for w in words:
            cur_line.append(w)
            bbox = f_desc.getbbox(' '.join(cur_line))
            if bbox[2] - bbox[0] > (card_w - 40):
                cur_line.pop()
                lines.append(' '.join(cur_line))
                cur_line = [w]
        if cur_line:
            lines.append(' '.join(cur_line))
            
        ly = cur_y + 115
        for line in lines:
            draw.text((x + 20, ly), line, font=f_desc, fill=C_TEXT)
            ly += 22

def render_scene_3(img, draw, t_scene, current_sec, scene):
    f_head = get_font(FONT_BOLD, 26)
    draw.text((48, 80), "THE SOLUTION: TURN YOUR SMARTPHONE INTO A TELEMETRY COCKPIT", font=f_head, fill=C_WHITE)
    f_sub = get_font(FONT_REGULAR, 14)
    draw.text((48, 115), "Rhody transforms internal mobile IMU sensors into an executive motorcycle telemetry suite.", font=f_sub, fill=C_MUTED)

    pillars = [
        {
            'icon': '🛡️',
            'title': 'Guardian Parent Live Tracking',
            'points': ['1-Click share link via WhatsApp or SMS', 'Zero login or app download for parents', 'Live Speed, Moving vs Stopped status, & GPS', 'Peace of mind on every journey'],
            'color': C_EMERALD
        },
        {
            'icon': '🏍️',
            'title': 'Precision IMU Lean Angle Fusion',
            'points': ['High-frequency Gyroscope & Accelerometer', 'Measures left/right cornering degrees in real-time', 'Aviation dual-axis G-force reticle', 'Calibrated for handlebar phone mounts'],
            'color': C_BLUE_LIGHT
        },
        {
            'icon': '⏱️',
            'title': 'Automatic Sprint Timers',
            'points': ['Standing start launch detection', 'Automated 0–60 km/h acceleration splits', 'Automated 0–100 km/h sprint timing', 'Peak speed & deceleration analytics'],
            'color': C_AMBER
        },
        {
            'icon': '☁️',
            'title': 'Zero-API Cloud & MongoDB Sync',
            'points': ['Automatic cloud sync to MongoDB Atlas', 'CartoDB Dark, OpenStreetMap & Satellite', '100% Free: No Google Maps API keys', 'Full web telemetry replay on rhodyy.vercel.app'],
            'color': C_WHITE
        }
    ]

    for idx, p in enumerate(pillars):
        col = idx % 2
        row = idx // 2
        x = 48 + col * 600
        y = 150 + row * 190
        w = 580
        h = 175

        draw.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=C_CARD, outline=C_BORDER)
        draw.rectangle([x, y + 10, x + 4, y + h - 10], fill=p['color'])

        f_t = get_font(FONT_BOLD, 17)
        draw.text((x + 22, y + 18), f"{p['icon']} {p['title']}", font=f_t, fill=p['color'])

        f_p = get_font(FONT_REGULAR, 12)
        py = y + 50
        for pt in p['points']:
            draw.text((x + 26, py), f"•  {pt}", font=f_p, fill=C_TEXT)
            py += 22

def render_scene_4(img, draw, t_scene, current_sec, scene, shots):
    f_step = get_font(FONT_BOLD, 12)
    draw.rounded_rectangle([48, 75, 140, 102], radius=6, fill=(37, 99, 235), outline=C_BLUE)
    draw.text((60, 81), "STEP 1 OF 3", font=f_step, fill=C_WHITE)

    f_head = get_font(FONT_BOLD, 24)
    draw.text((155, 75), "MOUNT PHONE & LAUNCH COCKPIT HUD", font=f_head, fill=C_WHITE)

    if 'dashboard.jpg' in shots:
        draw_phone_mockup(img, shots['dashboard.jpg'], 60, 115)

    rx = 420
    rw = 810
    
    # Speedometer Card
    draw.rounded_rectangle([rx, 115, rx + rw, 260], radius=12, fill=C_CARD, outline=C_BORDER)
    f_card_t = get_font(FONT_BOLD, 16)
    draw.text((rx + 20, 130), "🏎️ High-Contrast Cockpit Velocity & G-Force Reticle", font=f_card_t, fill=C_WHITE)
    
    speed_anim = int(min(108, t_scene * 18))
    f_spd = get_font(FONT_MONO, 44)
    draw.text((rx + 25, 165), f"{speed_anim}", font=f_spd, fill=C_BLUE_LIGHT)
    f_unit = get_font(FONT_BOLD, 14)
    draw.text((rx + 120, 192), "KM / H", font=f_unit, fill=C_MUTED)

    f_hud_desc = get_font(FONT_REGULAR, 13)
    draw.text((rx + 230, 172), "• Engineered for motorcycle handlebars under bright sunlight", font=f_hud_desc, fill=C_TEXT)
    draw.text((rx + 230, 196), "• Aviation dual-axis G-force reticle tracks braking & acceleration", font=f_hud_desc, fill=C_TEXT)
    draw.text((rx + 230, 220), "• Resilient GPS lock with speed outlier & spike filtering", font=f_hud_desc, fill=C_TEXT)

    # Automated Sprint Timers Card
    draw.rounded_rectangle([rx, 280, rx + rw, 435], radius=12, fill=C_CARD, outline=C_BORDER)
    draw.text((rx + 20, 295), "⏱️ Standing Launch Acceleration Splits (0-60 & 0-100 km/h)", font=f_card_t, fill=C_AMBER)

    draw.rounded_rectangle([rx + 20, 335, rx + 380, 415], radius=8, fill=(10, 12, 16), outline=C_BORDER)
    f_sp_t = get_font(FONT_BOLD, 13)
    draw.text((rx + 35, 348), "0 – 60 KM/H SPRINT", font=f_sp_t, fill=C_TEXT)
    f_sp_val = get_font(FONT_MONO, 28)
    draw.text((rx + 35, 372), "3.82 s", font=f_sp_val, fill=C_AMBER)
    draw.text((rx + 175, 382), "standing launch", font=get_font(FONT_REGULAR, 11), fill=C_MUTED)

    draw.rounded_rectangle([rx + 410, 335, rx + 770, 415], radius=8, fill=(10, 12, 16), outline=C_BORDER)
    draw.text((rx + 425, 348), "0 – 100 KM/H SPRINT", font=f_sp_t, fill=C_TEXT)
    draw.text((rx + 425, 372), "7.94 s", font=f_sp_val, fill=C_BLUE_LIGHT)
    draw.text((rx + 575, 382), "quarter-mile pass", font=get_font(FONT_REGULAR, 11), fill=C_MUTED)

    draw.rounded_rectangle([rx, 455, rx + rw, 520], radius=10, fill=C_CARD, outline=C_BORDER)
    f_foot = get_font(FONT_REGULAR, 13)
    draw.text((rx + 20, 475), "✓ 100% offline-first: GPS track points record reliably even without mobile cellular signal.", font=f_foot, fill=C_EMERALD)

def render_scene_5(img, draw, t_scene, current_sec, scene, shots):
    f_step = get_font(FONT_BOLD, 12)
    draw.rounded_rectangle([48, 75, 140, 102], radius=6, fill=(16, 185, 129), outline=C_EMERALD)
    draw.text((60, 81), "STEP 2 OF 3", font=f_step, fill=C_WHITE)

    f_head = get_font(FONT_BOLD, 24)
    draw.text((155, 75), "1-TAP GUARDIAN PARENT LIVE SAFETY BEACON", font=f_head, fill=C_WHITE)

    if 'live_trip.jpg' in shots:
        draw_phone_mockup(img, shots['live_trip.jpg'], 60, 115)

    rx = 420
    rw = 810

    draw.rounded_rectangle([rx, 115, rx + rw, 520], radius=14, fill=C_CARD, outline=C_EMERALD, width=2)

    draw.rectangle([rx, 115, rx + rw, 165], fill=(22, 28, 40))
    f_web_t = get_font(FONT_BOLD, 15)
    draw.text((rx + 20, 128), "🌐 What Parents See on Their Phone or PC Browser:", font=f_web_t, fill=C_WHITE)
    f_url = get_font(FONT_MONO, 12)
    draw.text((rx + rw - 260, 130), "rhodyy.vercel.app/?live=trip_92a", font=f_url, fill=C_BLUE_LIGHT)

    pulse = (int(t_scene * 2) % 2 == 0)
    dot_col = C_EMERALD if pulse else (5, 150, 105)
    
    draw.rounded_rectangle([rx + 25, 185, rx + 440, 235], radius=25, fill=(16, 185, 129, 30), outline=C_EMERALD)
    draw.ellipse([rx + 42, 203, rx + 56, 217], fill=dot_col)
    f_live_pill = get_font(FONT_BOLD, 16)
    draw.text((rx + 68, 200), "BIKE IS MOVING — 74 KM/H", font=f_live_pill, fill=C_EMERALD)

    draw.rounded_rectangle([rx + 460, 185, rx + 750, 235], radius=25, fill=(245, 158, 11, 20), outline=C_AMBER)
    draw.ellipse([rx + 475, 203, rx + 489, 217], fill=C_AMBER)
    draw.text((rx + 500, 200), "BIKE IS STOPPED (Idle / Rest)", font=get_font(FONT_BOLD, 13), fill=C_AMBER)

    metrics = [
        ("CURRENT SPEED", "74 km/h", C_WHITE),
        ("DISTANCE RIDDEN", "18.4 km", C_BLUE_LIGHT),
        ("TRIP DURATION", "00:24:18", C_WHITE),
        ("PHONE BATTERY", "84%", C_EMERALD)
    ]
    for i, (mtitle, mval, mcol) in enumerate(metrics):
        mx = rx + 25 + i * 190
        draw.rounded_rectangle([mx, 260, mx + 175, 335], radius=8, fill=(10, 12, 16), outline=C_BORDER)
        draw.text((mx + 14, 272), mtitle, font=get_font(FONT_BOLD, 10), fill=C_MUTED)
        draw.text((mx + 14, 295), mval, font=get_font(FONT_MONO, 20), fill=mcol)

    f_b_title = get_font(FONT_BOLD, 14)
    draw.text((rx + 25, 360), "Key Guardian Live Tracking Highlights:", font=f_b_title, fill=C_WHITE)
    f_b = get_font(FONT_REGULAR, 13)
    draw.text((rx + 25, 390), "• ZERO APP OR LOGIN: Parents click the WhatsApp/SMS link and tracking opens instantly.", font=f_b, fill=C_TEXT)
    draw.text((rx + 25, 415), "• LIVE SAFETY BEACON: Replaces nerve-wracking phone calls while riding at highway speeds.", font=f_b, fill=C_TEXT)
    draw.text((rx + 25, 440), "• AUTOMATIC ARRIVAL NOTICE: Displays 'TRIP COMPLETED SAFELY' the moment the rider reaches home.", font=f_b, fill=C_EMERALD)
    draw.text((rx + 25, 465), "• LIVE BATTERY LEVEL: Parents know if rider's phone battery is healthy.", font=f_b, fill=C_TEXT)

def render_scene_6(img, draw, t_scene, current_sec, scene, shots):
    f_step = get_font(FONT_BOLD, 12)
    draw.rounded_rectangle([48, 75, 140, 102], radius=6, fill=(245, 158, 11), outline=C_AMBER)
    draw.text((60, 81), "STEP 3 OF 3", font=f_step, fill=C_WHITE)

    f_head = get_font(FONT_BOLD, 24)
    draw.text((155, 75), "POST-RIDE LEAN ANGLE & CLOUD TELEMETRY", font=f_head, fill=C_WHITE)

    if 'analytics.jpg' in shots:
        draw_phone_mockup(img, shots['analytics.jpg'], 60, 115)

    rx = 420
    rw = 810

    draw.rounded_rectangle([rx, 115, rx + rw, 310], radius=14, fill=C_CARD, outline=C_BORDER)
    draw.text((rx + 20, 130), "🏍️ Precision Motorcycle Lean Angle Fusion (Left / Right Cornering)", font=get_font(FONT_BOLD, 16), fill=C_AMBER)

    draw.rounded_rectangle([rx + 25, 170, rx + 240, 275], radius=10, fill=(10, 12, 16), outline=C_AMBER)
    draw.text((rx + 40, 185), "MAX LEFT LEAN", font=get_font(FONT_BOLD, 11), fill=C_MUTED)
    draw.text((rx + 40, 210), "42.0°", font=get_font(FONT_MONO, 36), fill=C_AMBER)
    draw.text((rx + 40, 252), "Western Ghats Apex", font=get_font(FONT_REGULAR, 10), fill=C_MUTED)

    draw.rounded_rectangle([rx + 265, 170, rx + 480, 275], radius=10, fill=(10, 12, 16), outline=C_BLUE)
    draw.text((rx + 280, 185), "MAX RIGHT LEAN", font=get_font(FONT_BOLD, 11), fill=C_MUTED)
    draw.text((rx + 280, 210), "38.5°", font=get_font(FONT_MONO, 36), fill=C_BLUE_LIGHT)
    draw.text((rx + 280, 252), "Hairpin Turn #4", font=get_font(FONT_REGULAR, 10), fill=C_MUTED)

    f_lean_desc = get_font(FONT_REGULAR, 13)
    draw.text((rx + 510, 185), "• High-frequency gyroscope & accelerometer", font=f_lean_desc, fill=C_TEXT)
    draw.text((rx + 510, 212), "• Dynamic pitch & roll filtering", font=f_lean_desc, fill=C_TEXT)
    draw.text((rx + 510, 239), "• Ideal for track days, canyon carving & twisties", font=f_lean_desc, fill=C_TEXT)

    draw.rounded_rectangle([rx, 330, rx + rw, 520], radius=14, fill=C_CARD, outline=C_BORDER)
    draw.text((rx + 20, 345), "☁️ Cloud Telemetry Companion (rhodyy.vercel.app)", font=get_font(FONT_BOLD, 16), fill=C_WHITE)

    f_cloud_p = get_font(FONT_REGULAR, 13)
    draw.text((rx + 25, 385), "• 100% Free Zero-API Leaflet Maps: CartoDB Dark, OpenStreetMap, & Esri Satellite.", font=f_cloud_p, fill=C_TEXT)
    draw.text((rx + 25, 415), "• Interactive Playback Scrubber: Replay speed, elevation, and location second-by-second.", font=f_cloud_p, fill=C_TEXT)
    draw.text((rx + 25, 445), "• Standard GPX & CSV Export: Ready for Strava, Garmin Connect, and Google Earth.", font=f_cloud_p, fill=C_TEXT)
    draw.text((rx + 25, 475), "• MongoDB Atlas Cloud Sync: Automatically backs up rides across all your devices.", font=f_cloud_p, fill=C_EMERALD)

def render_scene_7(img, draw, t_scene, current_sec, scene):
    cx = WIDTH // 2

    draw.rounded_rectangle([cx - 440, 95, cx + 440, 520], radius=18, fill=C_CARD, outline=C_BLUE, width=2)

    draw.rounded_rectangle([cx - 180, 120, cx + 180, 155], radius=16, fill=(37, 99, 235), outline=C_BLUE)
    draw.text((cx - 148, 128), "100% FREE & OPEN SOURCE MOTORCYCLE APP", font=get_font(FONT_BOLD, 11), fill=C_WHITE)

    draw.text((cx - 240, 175), "START YOUR RIDE WITH RHODY", font=get_font(FONT_BOLD, 30), fill=C_WHITE)

    f_slogan = get_font(FONT_REGULAR, 18)
    slogan = "“Ride with Passion. Return with Peace of Mind.”"
    bbox_s = f_slogan.getbbox(slogan)
    sw = bbox_s[2] - bbox_s[0]
    draw.text((cx - sw // 2, 225), slogan, font=f_slogan, fill=C_AMBER)

    draw.rounded_rectangle([cx - 360, 275, cx + 360, 375], radius=12, fill=(10, 12, 16), outline=C_EMERALD, width=2)
    draw.text((cx - 320, 295), "📱 DOWNLOAD OFFICIAL ANDROID APK (v1.0.0)", font=get_font(FONT_BOLD, 16), fill=C_EMERALD)
    draw.text((cx - 320, 330), "Direct Download Link:  rhodyy.vercel.app/apk", font=get_font(FONT_MONO, 15), fill=C_WHITE)

    draw.rounded_rectangle([cx - 360, 395, cx - 20, 465], radius=10, fill=(18, 24, 34), outline=C_BORDER)
    draw.text((cx - 340, 412), "🌐 Live Companion Web", font=get_font(FONT_BOLD, 12), fill=C_BLUE_LIGHT)
    draw.text((cx - 340, 435), "rhodyy.vercel.app", font=get_font(FONT_MONO, 12), fill=C_TEXT)

    draw.rounded_rectangle([cx + 20, 395, cx + 360, 465], radius=10, fill=(18, 24, 34), outline=C_BORDER)
    draw.text((cx + 40, 412), "💻 GitHub Repository", font=get_font(FONT_BOLD, 12), fill=C_BLUE_LIGHT)
    draw.text((cx + 40, 435), "github.com/NateshKannaR/Rhody", font=get_font(FONT_MONO, 12), fill=C_TEXT)

    draw.text((cx - 240, 485), "Kotlin • Jetpack Compose • IMU Fusion • MongoDB Atlas • Zero-API Leaflet", font=get_font(FONT_REGULAR, 12), fill=C_MUTED)

def main():
    audio_path = synthesize_audio()

    print('[2/4] Loading authentic mobile screenshots...')
    shots = load_and_prepare_screenshots()
    print(f"  ✓ Loaded screenshots: {list(shots.keys())}")

    output_mp4 = '/home/natesh/Downloads/Rhody/docs/rhody_overview_2min.mp4'
    print(f"[3/4] Rendering {TOTAL_FRAMES} frames (120s @ 25fps) to {output_mp4}...")

    cmd_ffmpeg = [
        'ffmpeg', '-y',
        '-f', 'rawvideo',
        '-vcodec', 'rawvideo',
        '-s', f"{WIDTH}x{HEIGHT}",
        '-pix_fmt', 'bgr24',
        '-r', f"{FPS}",
        '-i', '-',
        '-i', audio_path,
        '-c:v', 'libx264',
        '-preset', 'veryfast',
        '-crf', '22',
        '-pix_fmt', 'yuv420p',
        '-c:a', 'aac',
        '-b:a', '192k',
        '-movflags', '+faststart',
        output_mp4
    ]

    proc = subprocess.Popen(cmd_ffmpeg, stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    base_bg = Image.new('RGB', (WIDTH, HEIGHT), color=C_DARK)

    for frame_idx in range(TOTAL_FRAMES):
        current_sec = frame_idx / FPS

        active_scene = SCENES[-1]
        for s in SCENES:
            if s['start_sec'] <= current_sec < s['end_sec']:
                active_scene = s
                break

        t_scene = current_sec - active_scene['start_sec']

        frame_img = base_bg.copy()
        draw = ImageDraw.Draw(frame_img)

        draw_header_and_chrome(draw, current_sec, active_scene)
        draw_subtitle_bar(draw, active_scene)

        sid = active_scene['id']
        if sid == 1:
            render_scene_1(frame_img, draw, t_scene, current_sec, active_scene)
        elif sid == 2:
            render_scene_2(frame_img, draw, t_scene, current_sec, active_scene)
        elif sid == 3:
            render_scene_3(frame_img, draw, t_scene, current_sec, active_scene)
        elif sid == 4:
            render_scene_4(frame_img, draw, t_scene, current_sec, active_scene, shots)
        elif sid == 5:
            render_scene_5(frame_img, draw, t_scene, current_sec, active_scene, shots)
        elif sid == 6:
            render_scene_6(frame_img, draw, t_scene, current_sec, active_scene, shots)
        elif sid == 7:
            render_scene_7(frame_img, draw, t_scene, current_sec, active_scene)

        frame_np = np.array(frame_img)
        frame_bgr = frame_np[:, :, ::-1]
        proc.stdin.write(frame_bgr.tobytes())

        if (frame_idx + 1) % 250 == 0 or frame_idx == TOTAL_FRAMES - 1:
            pct = ((frame_idx + 1) / TOTAL_FRAMES) * 100
            print(f"  -> Rendered frame {frame_idx + 1}/{TOTAL_FRAMES} ({pct:.1f}%) — {current_sec:.1f}s / 120s")

    proc.stdin.close()
    proc.wait()
    print('  ✓ Video rendering complete!')

    print('[4/4] Generating animated GIF preview & poster image for README.md...')
    gif_path = '/home/natesh/Downloads/Rhody/docs/rhody_demo_preview.gif'
    thumb_path = '/home/natesh/Downloads/Rhody/docs/video_poster.jpg'

    cmd_gif = [
        'ffmpeg', '-y',
        '-ss', '74', '-t', '6',
        '-i', output_mp4,
        '-vf', 'fps=10,scale=600:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse',
        '-loop', '0',
        gif_path
    ]
    subprocess.run(cmd_gif, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)

    cmd_thumb = ['ffmpeg', '-y', '-ss', '42', '-i', output_mp4, '-vframes', '1', '-q:v', '2', thumb_path]
    subprocess.run(cmd_thumb, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)

    print(f"  ✓ Animated GIF preview generated: {gif_path}")
    print(f"  ✓ Poster frame generated: {thumb_path}")
    print(f"  ✓ Master 2-minute video: {output_mp4}")

if __name__ == '__main__':
    main()
