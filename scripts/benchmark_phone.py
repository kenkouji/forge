#!/usr/bin/env python3
"""
Runs latency benchmark directly from Android phone via ADB shell curl.
Tests real device performance over NetBird overlay (Wi-Fi or Cellular).
Measures:
  1. Overlay ICMP / TCP connection establishment time
  2. Fresh HTTP request time (new connection)
  3. Reused HTTP request time (connection pooling)
  4. Command dispatch time (get_system_status)
  5. Screenshot capture, transfer, and payload size (take_screenshot preview)
"""

import subprocess
import json
import time
import statistics
import sys
import os

MAC_OVERLAY_IP = "100.117.133.109"
PORT = "8090"
BASE_URL = f"http://{MAC_OVERLAY_IP}:{PORT}"
NUM_ITERATIONS = 30

def get_attached_device() -> str:
    res = subprocess.run(["adb", "devices"], stdout=subprocess.PIPE, text=True)
    for line in res.stdout.strip().splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            return parts[0]
    return "RZCW90Z590X"

DEVICE_ADB_ID = get_attached_device()

def adb_shell(cmd: str, timeout: float = 20.0) -> str:
    full_cmd = ["adb", "-s", DEVICE_ADB_ID, "shell", cmd]
    res = subprocess.run(full_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=timeout)
    return res.stdout.strip()

def get_auth_token():
    payload = json.dumps({"username": "admin", "password": "remoteforge_secure_pass"})
    cmd = f"curl -s -X POST '{BASE_URL}/api/auth/login' -H 'Content-Type: application/json' -d '{payload}'"
    out = adb_shell(cmd)
    try:
        data = json.loads(out)
        return data.get("access_token")
    except Exception as e:
        print(f"Failed to get token via ADB: {e}, output: {out[:100]}")
        return None

def measure_ping_overlay(iterations: int = 30):
    cmd = f"ping -c {iterations} {MAC_OVERLAY_IP}"
    out = adb_shell(cmd, timeout=40.0)
    times = []
    for line in out.splitlines():
        if "time=" in line:
            try:
                part = line.split("time=")[1].split()[0]
                times.append(float(part))
            except Exception:
                pass
    return times

def measure_curl_health_fresh(iterations: int = 30):
    times_connect = []
    times_total = []
    for _ in range(iterations):
        cmd = f"curl -s -o /dev/null -w '%{{time_connect}} %{{time_total}}' -H 'Connection: close' '{BASE_URL}/api/health'"
        out = adb_shell(cmd)
        parts = out.split()
        if len(parts) >= 2:
            try:
                times_connect.append(float(parts[0]) * 1000)
                times_total.append(float(parts[1]) * 1000)
            except Exception:
                pass
        time.sleep(0.04)
    return times_connect, times_total

def measure_curl_health_reused(iterations: int = 30):
    # Reuses existing TCP connection across consecutive requests in single curl invocation
    times_reused = []
    chunk_size = 5
    for _ in range(iterations // chunk_size):
        urls = " ".join([f"'{BASE_URL}/api/health'" for _ in range(chunk_size)])
        dev_nulls = " ".join(["-o /dev/null" for _ in range(chunk_size)])
        cmd = f"curl -s {dev_nulls} -w '%{{time_connect}} %{{time_total}}\\n' {urls}"
        out = adb_shell(cmd)
        for line in out.splitlines():
            parts = line.split()
            if len(parts) >= 2:
                try:
                    conn_ms = float(parts[0]) * 1000
                    total_ms = float(parts[1]) * 1000
                    if conn_ms < 1.0: # Reused connection
                        times_reused.append(total_ms)
                except Exception:
                    pass
        time.sleep(0.04)
    return times_reused

def measure_curl_command_dispatch(token: str, iterations: int = 30):
    cmd_body = json.dumps({"tool": "get_system_status", "arguments": {}})
    times_total = []
    for _ in range(iterations):
        cmd = f"curl -s -o /dev/null -w '%{{time_total}}' -X POST '{BASE_URL}/api/devices/macbook-pro/commands' -H 'Content-Type: application/json' -H 'Authorization: Bearer {token}' -d '{cmd_body}'"
        out = adb_shell(cmd)
        try:
            times_total.append(float(out) * 1000)
        except Exception:
            pass
        time.sleep(0.04)
    return times_total

def measure_curl_screenshot(token: str, iterations: int = 10):
    cmd_body = json.dumps({"tool": "take_screenshot", "arguments": {}})
    times_total = []
    sizes_kb = []
    for _ in range(iterations):
        cmd = f"curl -s -w '\\n%{{time_total}} %{{size_download}}' -X POST '{BASE_URL}/api/devices/macbook-pro/commands' -H 'Content-Type: application/json' -H 'Authorization: Bearer {token}' -d '{cmd_body}'"
        out = adb_shell(cmd, timeout=30.0)
        lines = out.strip().splitlines()
        if lines:
            meta = lines[-1].split()
            if len(meta) >= 2:
                try:
                    times_total.append(float(meta[0]) * 1000)
                    sizes_kb.append(float(meta[1]) / 1024)
                except Exception:
                    pass
        time.sleep(0.1)
    return times_total, sizes_kb

def calc_stats(data):
    if not data:
        return {"min": 0, "med": 0, "p95": 0, "max": 0, "count": 0}
    sorted_d = sorted(data)
    p95_idx = int(0.95 * len(sorted_d))
    return {
        "min": round(sorted_d[0], 2),
        "med": round(statistics.median(sorted_d), 2),
        "p95": round(sorted_d[p95_idx], 2),
        "max": round(sorted_d[-1], 2),
        "count": len(sorted_d)
    }

def print_table(title, stats, unit="ms"):
    print(f"\n=== {title} (N={stats['count']}) ===")
    print(f"  Min:    {stats['min']:>7.2f} {unit}")
    print(f"  Median: {stats['med']:>7.2f} {unit}")
    print(f"  P95:    {stats['p95']:>7.2f} {unit}")
    print(f"  Max:    {stats['max']:>7.2f} {unit}")

def main(label="optimized"):
    print(f"Starting Android phone latency benchmark [{label}] via ADB device {DEVICE_ADB_ID}...")
    token = get_auth_token()
    if not token:
        print("Failed to authenticate from phone.")
        sys.exit(1)

    print("[1/5] Measuring ICMP ping from phone to Mac overlay...")
    ping_stats = calc_stats(measure_ping_overlay(NUM_ITERATIONS))
    print_table("1. Phone ICMP Overlay RTT", ping_stats)

    print("[2/5] Measuring TCP connect & HTTP fresh GET /api/health...")
    conn_times, http_fresh_times = measure_curl_health_fresh(NUM_ITERATIONS)
    conn_stats = calc_stats(conn_times)
    http_fresh_stats = calc_stats(http_fresh_times)
    print_table("2a. Phone TCP Handshake Duration", conn_stats)
    print_table("2b. Phone Fresh HTTP GET /api/health", http_fresh_stats)

    print("[3/5] Measuring HTTP GET /api/health with Connection Reuse...")
    http_reused_times = measure_curl_health_reused(NUM_ITERATIONS)
    http_reused_stats = calc_stats(http_reused_times)
    print_table("3. Phone Reused Connection HTTP GET /api/health", http_reused_stats)

    print("[4/5] Measuring Command Dispatch (get_system_status)...")
    cmd_times = measure_curl_command_dispatch(token, NUM_ITERATIONS)
    cmd_stats = calc_stats(cmd_times)
    print_table("4. Phone Command Dispatch (get_system_status)", cmd_stats)

    print("[5/5] Measuring Optimized Screenshot Capture & Transfer...")
    ss_times, ss_sizes = measure_curl_screenshot(token, 10)
    ss_stats = calc_stats(ss_times)
    ss_size_stats = calc_stats(ss_sizes)
    print_table("5a. Phone Screenshot Transfer Time", ss_stats)
    print_table("5b. Phone Screenshot Payload Size", ss_size_stats, unit="KB")

    result = {
        "label": label,
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "ping_rtt": ping_stats,
        "tcp_handshake": conn_stats,
        "http_health_fresh": http_fresh_stats,
        "http_health_reused": http_reused_stats,
        "command_dispatch": cmd_stats,
        "screenshot_latency": ss_stats,
        "screenshot_size_kb": ss_size_stats
    }
    filename = f"phone_benchmark_{label}.json"
    with open(filename, "w") as f:
        json.dump(result, f, indent=2)
    print(f"\nSaved benchmark results to {filename}")

if __name__ == "__main__":
    lbl = sys.argv[1] if len(sys.argv) > 1 else "optimized"
    main(lbl)
