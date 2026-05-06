"""
Generate 200 test users and their JWT tokens for k6 multi-user load testing.
"""
import json
import time
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8080/api/v1/users"
PASSWORD = "test123"
TOTAL = 200
users = []

for i in range(1, TOTAL + 1):
    username = f"loaduser_{i}"
    payload = json.dumps({"username": username, "password": PASSWORD}).encode()

    # Register
    req = urllib.request.Request(
        f"{BASE}/register",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        resp = urllib.request.urlopen(req, timeout=10)
        body = json.loads(resp.read())
        if body.get("code") not in (20000, 50001):
            print(f"  [{i:03d}] register {username}: unexpected code {body.get('code')} — {body.get('message')}")
            continue
    except urllib.error.HTTPError as e:
        body = json.loads(e.read())
        if body.get("code") != 50001:  # Duplicate username from previous run
            print(f"  [{i:03d}] register {username}: HTTP {e.code} — {body.get('message')}")
            continue

    # Login
    req2 = urllib.request.Request(
        f"{BASE}/login",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        resp2 = urllib.request.urlopen(req2, timeout=10)
        body2 = json.loads(resp2.read())
        if body2.get("code") != 20000:
            print(f"  [{i:03d}] login {username}: code {body2.get('code')} — {body2.get('message')}")
            continue
        token = body2["data"]["token"]
        users.append({"username": username, "token": token})
        if i % 50 == 0:
            print(f"  [{i:03d}] {username} registered + logged in")
    except Exception as e:
        print(f"  [{i:03d}] login {username}: {e}")
        continue

    time.sleep(0.02)

output_path = "scripts/performance/users.json"
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(users, f, ensure_ascii=False)

print(f"\nDone: {len(users)}/{TOTAL} users written to {output_path}")
if len(users) < TOTAL:
    print(f"Warning: {TOTAL - len(users)} users failed to generate")
