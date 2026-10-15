#!/usr/bin/env python3
"""
RemoteForge Latency Benchmark Suite
Measures:
1. Raw overlay TCP connection establishment time
2. HTTP fresh connection (cold, Connection: close)
3. HTTP reused connection (warm, Keep-Alive)
4. End-to-end command dispatch latency (get_system_status)
5. Screenshot capture & payload transfer latency
Runs 30 iterations per test; computes Median, P95, Min, Max.
"""

import time
import socket
import json
import statistics
import sys
import httpx

TARGET_HOST = sys.argv[1] if len(sys.argv) > 1 else "127.0.0.1"
TARGET_PORT = 8090
BASE_URL = f"http://{TARGET_HOST}:{TARGET_PORT}"
DEVICE_ID = "macbook-pro"
NUM_ITERATIONS = 30

def measure_tcp_handshake(host: str, port: int, iterations: int = 30):
    latencies = []
    for _ in range(iterations):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(5.0)
        t0 = time.perf_counter()
        try:
            s.connect((host, port))
            t1 = time.perf_counter()
            latencies.append((t1 - t0) * 1000)
        finally:
            s.close()
        time.sleep(0.02)
    return latencies

def get_auth_token():
    try:
        resp = httpx.post(
            f"{BASE_URL}/api/auth/login",
            json={"username": "admin", "password": "remoteforge_secure_pass"},
            timeout=5.0
        )
        if resp.status_code == 200:
            return resp.json()["access_token"]
    except Exception as e:
        print(f"Auth failed: {e}")
    return None

def measure_http_fresh(iterations: int = 30):
    latencies = []
    for _ in range(iterations):
        t0 = time.perf_counter()
        with httpx.Client(headers={"Connection": "close"}, timeout=10.0) as client:
            resp = client.get(f"{BASE_URL}/api/health")
            t1 = time.perf_counter()
            if resp.status_code == 200:
                latencies.append((t1 - t0) * 1000)
        time.sleep(0.02)
    return latencies

def measure_http_reused(iterations: int = 30):
    latencies = []
    with httpx.Client(timeout=10.0) as client:
        # Warmup connection
        client.get(f"{BASE_URL}/api/health")
        for _ in range(iterations):
            t0 = time.perf_counter()
            resp = client.get(f"{BASE_URL}/api/health")
            t1 = time.perf_counter()
            if resp.status_code == 200:
                latencies.append((t1 - t0) * 1000)
            time.sleep(0.02)
    return latencies

def measure_command_dispatch(token: str, iterations: int = 30):
    latencies = []
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    with httpx.Client(timeout=15.0) as client:
        # Warmup
        client.post(
            f"{BASE_URL}/api/devices/{DEVICE_ID}/commands",
            json={"tool": "get_system_status", "arguments": {}},
            headers=headers
        )
        for _ in range(iterations):
            t0 = time.perf_counter()
            resp = client.post(
                f"{BASE_URL}/api/devices/{DEVICE_ID}/commands",
                json={"tool": "get_system_status", "arguments": {}},
                headers=headers
            )
            t1 = time.perf_counter()
            if resp.status_code == 200 and resp.json().get("status") == "COMPLETED":
                latencies.append((t1 - t0) * 1000)
            else:
                print(f"Command error: {resp.status_code} {resp.text[:100]}")
            time.sleep(0.05)
    return latencies

def measure_screenshot(token: str, iterations: int = 5):
    latencies = []
    sizes_kb = []
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    with httpx.Client(timeout=20.0) as client:
        for _ in range(iterations):
            t0 = time.perf_counter()
            resp = client.post(
                f"{BASE_URL}/api/devices/{DEVICE_ID}/commands",
                json={"tool": "take_screenshot", "arguments": {}},
                headers=headers
            )
            t1 = time.perf_counter()
            if resp.status_code == 200:
                data = resp.json()
                b64 = data.get("result", {}).get("data_base64", "")
                sizes_kb.append(len(b64) / 1024)
                latencies.append((t1 - t0) * 1000)
            time.sleep(0.2)
    return latencies, sizes_kb

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

def main():
    print(f"Running RemoteForge Benchmark on {BASE_URL} ({NUM_ITERATIONS} iterations)...")
    token = get_auth_token()
    if not token:
        print("Failed to authenticate with backend.")
        sys.exit(1)

    print("[1/5] Measuring TCP Handshake Latency...")
    tcp_stats = calc_stats(measure_tcp_handshake(TARGET_HOST, TARGET_PORT, NUM_ITERATIONS))
    print_table("1. TCP Connection Establishment", tcp_stats)

    print("[2/5] Measuring Fresh HTTP Connection (Cold)...")
    fresh_stats = calc_stats(measure_http_fresh(NUM_ITERATIONS))
    print_table("2. HTTP Fresh Connection (Cold)", fresh_stats)

    print("[3/5] Measuring Reused HTTP Connection (Warm)...")
    reused_stats = calc_stats(measure_http_reused(NUM_ITERATIONS))
    print_table("3. HTTP Reused Connection (Warm)", reused_stats)

    print("[4/5] Measuring End-to-End Command Dispatch (get_system_status)...")
    cmd_stats = calc_stats(measure_command_dispatch(token, NUM_ITERATIONS))
    print_table("4. End-to-End Command Dispatch", cmd_stats)

    print("[5/5] Measuring Screenshot Capture & Transfer...")
    ss_times, ss_sizes = measure_screenshot(token, iterations=10)
    ss_time_stats = calc_stats(ss_times)
    ss_size_stats = calc_stats(ss_sizes)
    print_table("5. Screenshot Total Latency", ss_time_stats)
    print_table("5b. Screenshot Payload Size", ss_size_stats, unit="KB")

    # Output JSON summary for artifact / comparison
    summary = {
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "tcp_handshake": tcp_stats,
        "http_fresh": fresh_stats,
        "http_reused": reused_stats,
        "command_dispatch": cmd_stats,
        "screenshot_latency": ss_time_stats,
        "screenshot_size_kb": ss_size_stats
    }
    with open("benchmark_baseline.json", "w") as f:
        json.dump(summary, f, indent=2)
    print("\nSaved benchmark results to benchmark_baseline.json")

if __name__ == "__main__":
    main()
