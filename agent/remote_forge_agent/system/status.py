import os
import sys
import platform
import time
import datetime
import subprocess
from typing import Dict, Any, List
import psutil

AGENT_VERSION = "0.2.0"

# Pre-sample CPU counter so interval=None is non-blocking and accurate
psutil.cpu_percent(interval=None)

# Cache immutable platform and boot metrics once
_CACHED_OS_NAME = "macOS" if platform.system() == "Darwin" else platform.system()
_CACHED_OS_VERSION = platform.mac_ver()[0] if sys.platform == "darwin" else platform.release()
_CACHED_ARCH = platform.machine()
_CACHED_CPU_LOGICAL = psutil.cpu_count(logical=True) or 4
_CACHED_CPU_PHYSICAL = psutil.cpu_count(logical=False) or _CACHED_CPU_LOGICAL
_CACHED_BOOT_TIME = psutil.boot_time()
_HOME_PATH = os.path.expanduser("~")

# Network I/O throughput state tracker
_last_net_time = time.time()
try:
    _init_net = psutil.net_io_counters()
    _last_bytes_sent = _init_net.bytes_sent
    _last_bytes_recv = _init_net.bytes_recv
except Exception:
    _last_bytes_sent = 0
    _last_bytes_recv = 0

def get_system_status_info() -> Dict[str, Any]:
    global _last_net_time, _last_bytes_sent, _last_bytes_recv

    # Non-blocking CPU measurement (0.0ms vs 200ms blocking sleep)
    cpu_percent = psutil.cpu_percent(interval=None)

    # Memory Information
    vm = psutil.virtual_memory()
    total_ram_gb = round(vm.total / (1024 ** 3), 2)
    used_ram_gb = round(vm.used / (1024 ** 3), 2)
    ram_percent = vm.percent

    # Disk Information (Home / User directory mount)
    try:
        disk = psutil.disk_usage(_HOME_PATH)
        total_disk_gb = round(disk.total / (1024 ** 3), 2)
        used_disk_gb = round(disk.used / (1024 ** 3), 2)
        disk_percent = disk.percent
    except Exception:
        total_disk_gb = 0
        used_disk_gb = 0
        disk_percent = 0

    # System Uptime
    uptime_seconds = int(time.time() - _CACHED_BOOT_TIME)
    uptime_hours = uptime_seconds // 3600
    uptime_minutes = (uptime_seconds % 3600) // 60
    uptime_str = f"{uptime_hours}h {uptime_minutes}m"

    # Battery Status
    battery_info = None
    try:
        batt = psutil.sensors_battery()
        if batt is not None:
            secs_left = batt.secsleft if (batt.secsleft != psutil.POWER_TIME_UNLIMITED and batt.secsleft > 0) else None
            battery_info = {
                "percent": int(batt.percent),
                "power_plugged": bool(batt.power_plugged),
                "secs_left": secs_left,
                "status": "Charging" if batt.power_plugged else "Discharging"
            }
    except Exception:
        pass

    # Thermal & Throttling Status
    thermal_info = {
        "status": "Nominal",
        "cpu_speed_limit": 100
    }
    if sys.platform == "darwin":
        try:
            res = subprocess.run(["pmset", "-g", "therm"], capture_output=True, text=True, timeout=1.0)
            if res.returncode == 0:
                speed = 100
                for line in res.stdout.splitlines():
                    if "CPU_Speed_Limit" in line:
                        parts = line.split("=")
                        if len(parts) > 1:
                            try:
                                speed = int(parts[1].strip())
                            except ValueError:
                                pass
                thermal_info["cpu_speed_limit"] = speed
                thermal_info["status"] = "Throttled" if speed < 100 else "Nominal"
        except Exception:
            pass

    # Network Throughput calculation
    now = time.time()
    dt = max(0.1, now - _last_net_time)
    rx_kbps = 0.0
    tx_kbps = 0.0
    try:
        net = psutil.net_io_counters()
        rx_kbps = round(((net.bytes_recv - _last_bytes_recv) / dt) / 1024, 1)
        tx_kbps = round(((net.bytes_sent - _last_bytes_sent) / dt) / 1024, 1)
        if rx_kbps < 0:
            rx_kbps = 0.0
        if tx_kbps < 0:
            tx_kbps = 0.0
        _last_bytes_recv = net.bytes_recv
        _last_bytes_sent = net.bytes_sent
        _last_net_time = now
    except Exception:
        pass

    network_info = {
        "rx_kbps": rx_kbps,
        "tx_kbps": tx_kbps
    }

    # Top Processes (top 5 by memory)
    top_processes: List[Dict[str, Any]] = []
    try:
        raw_procs = []
        for p in psutil.process_iter(["pid", "name", "cpu_percent", "memory_percent", "status"]):
            try:
                raw_procs.append(p.info)
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                pass
        sorted_procs = sorted(raw_procs, key=lambda x: (x.get("memory_percent") or 0.0), reverse=True)[:5]
        for sp in sorted_procs:
            top_processes.append({
                "pid": sp.get("pid", 0),
                "name": sp.get("name", "unknown"),
                "cpu_percent": round(sp.get("cpu_percent") or 0.0, 1),
                "memory_percent": round(sp.get("memory_percent") or 0.0, 1),
                "status": sp.get("status", "running")
            })
    except Exception:
        pass

    return {
        "os_name": _CACHED_OS_NAME,
        "os_version": _CACHED_OS_VERSION,
        "machine_architecture": _CACHED_ARCH,
        "cpu": {
            "percent": cpu_percent,
            "logical_cores": _CACHED_CPU_LOGICAL,
            "physical_cores": _CACHED_CPU_PHYSICAL
        },
        "ram": {
            "total_gb": total_ram_gb,
            "used_gb": used_ram_gb,
            "percent": ram_percent
        },
        "disk": {
            "total_gb": total_disk_gb,
            "used_gb": used_disk_gb,
            "percent": disk_percent
        },
        "battery": battery_info,
        "thermal": thermal_info,
        "network": network_info,
        "top_processes": top_processes,
        "uptime_seconds": uptime_seconds,
        "uptime_formatted": uptime_str,
        "agent_version": AGENT_VERSION,
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat()
    }
