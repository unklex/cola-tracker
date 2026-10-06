import os
import requests

BASE_URL = os.environ.get("COLA_URL", "http://localhost:8000")
TOKEN = os.environ["COLA_TOKEN"]

def test_auth():
    print(f"Testing connection to {BASE_URL}...")
    
    # 1. Health check (no auth)
    try:
        r = requests.get(f"{BASE_URL}/", timeout=5)
        print(f"Health Check: {r.status_code} {r.text}")
    except Exception as e:
        print(f"Health Check Failed: {e}")
        return

    # 2. Auth check (Get Children)
    headers = {"Authorization": f"Bearer {TOKEN}"}
    try:
        r = requests.get(f"{BASE_URL}/children", headers=headers, timeout=5)
        if r.status_code == 200:
            print("\n✅ SUCCESS: Token is VALID.")
            print(f"Children data: {r.json()}")
        else:
            print(f"\n❌ FAILURE: Token rejected. Status: {r.status_code}")
            print(f"Response: {r.text}")
            print("Conclusion: The token in the app does NOT match the server's token.")
    except Exception as e:
        print(f"Auth Request Failed: {e}")

    # 3. Upload check
    print("\nTesting Upload...")
    files = {'file': ('test.jpg', b'fake image data', 'image/jpeg')}
    try:
        r = requests.post(f"{BASE_URL}/children/1/photo", headers=headers, files=files, timeout=10)
        if r.status_code == 200:
             print("✅ UPLOAD SUCCESS: Server accepts upload with this token.")
        else:
             print(f"❌ UPLOAD FAILURE: {r.status_code} {r.text}")
    except Exception as e:
        print(f"Upload Request Failed: {e}")

if __name__ == "__main__":
    test_auth()
