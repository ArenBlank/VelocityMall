"""
Generate 200 test users and their JWT tokens for k6 multi-user load testing.
"""
import json
import sys
import time
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8080/api/v1/users"
PASSWORD = "test123"
TOTAL = 200
RETRY_MAX = 30
RETRY_DELAY = 2


def request_with_retry(url, data, timeout=10):
    """HTTP POST with retry on connection errors."""
    payload = json.dumps(data).encode()
    last_err = None
    for attempt in range(1, RETRY_MAX + 1):
        try:
            req = urllib.request.Request(
                url,
                data=payload,
                headers={"Content-Type": "application/json"},
                method="POST",
            )
            resp = urllib.request.urlopen(req, timeout=timeout)
            return json.loads(resp.read()), None
        except urllib.error.HTTPError as e:
            body = json.loads(e.read())
            return body, e.code
        except (urllib.error.URLError, ConnectionRefusedError, OSError) as e:
            last_err = e
            if attempt < RETRY_MAX:
                print(f"  Connection refused, retrying ({attempt}/{RETRY_MAX})...", file=sys.stderr)
                time.sleep(RETRY_DELAY)
    raise ConnectionError(f"Gateway unreachable after {RETRY_MAX} retries: {last_err}")


users = []

# Warm-up: wait until gateway routes to user service
print("Waiting for gateway routing to user service...", file=sys.stderr)
for i in range(1, RETRY_MAX + 1):
    try:
        body, _ = request_with_retry(f"{BASE}/login", {"username": "e2euser", "password": "e2epass123"})
        if body.get("code") in (20000, 50001, 50002):
            print("Gateway routing OK", file=sys.stderr)
            break
    except ConnectionError:
        if i == RETRY_MAX:
            print("Gateway routing check failed", file=sys.stderr)
            sys.exit(1)
        time.sleep(RETRY_DELAY)
    time.sleep(RETRY_DELAY)

for i in range(1, TOTAL + 1):
    username = f"loaduser_{i}"
    user_data = {"username": username, "password": PASSWORD}

    # Register
    body, http_code = request_with_retry(f"{BASE}/register", user_data)
    if body.get("code") not in (20000, 50001):
        print(f"  [{i:03d}] register {username}: unexpected code {body.get('code')} — {body.get('message')}")
        continue

    # Login
    try:
        body2, http_code2 = request_with_retry(f"{BASE}/login", user_data)
    except ConnectionError as e:
        print(f"  [{i:03d}] login {username}: {e}")
        continue

    if body2.get("code") != 20000:
        print(f"  [{i:03d}] login {username}: code {body2.get('code')} — {body2.get('message')}")
        continue

    token = body2["data"]["token"]
    users.append({"username": username, "token": token})
    if i % 50 == 0:
        print(f"  [{i:03d}] {username} registered + logged in")

    time.sleep(0.02)

output_path = "scripts/performance/users.json"
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(users, f, ensure_ascii=False)

print(f"\nDone: {len(users)}/{TOTAL} users written to {output_path}")
if len(users) < TOTAL:
    print(f"Warning: {TOTAL - len(users)} users failed to generate")
