# RemoteForge Developer Guide

## 1. Local Development Environment

```bash
# 1. Clone repository & initialize virtual environment
git clone <repo-url> remoteforge
cd remoteforge
python3 -m venv venv
./venv/bin/pip install -r backend/requirements.txt
./venv/bin/pip install -r agent/requirements.txt

# 2. Setup environment config
cp .env.example .env

# 3. Install frontend dependencies
cd web
npm install
npm run build
cd ..
```

---

## 2. Running Services in Development Mode

### Terminal 1: Backend Server (FastAPI with Hot Reload)
```bash
PYTHONPATH="backend:." ./venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Terminal 2: PC Agent
```bash
PYTHONPATH="backend:." ./venv/bin/python3 agent/remote_forge_agent/main.py --server ws://127.0.0.1:8000
```

### Terminal 3: Web Dashboard (Vite HMR Development Server)
```bash
cd web
npm run dev
# Running on http://localhost:5173 (automatically proxies to backend on port 8000)
```

---

## 3. Running Automated Tests
```bash
PYTHONPATH="backend:." ./venv/bin/pytest -v tests/
```
