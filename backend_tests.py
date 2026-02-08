#!/usr/bin/env python3
"""
Cola Tracker Backend API Tests
Tests to verify that the backend correctly handles requests and persists data.

Usage:
    python backend_tests.py

Requirements:
    pip install requests
"""

import requests
import time
from datetime import datetime

# Configuration
BASE_URL = "http://<SERVER_IP>:8000"
AUTH_TOKEN = "REDACTED_OLD_TOKEN"

# Headers for authenticated requests
HEADERS = {
    "Authorization": f"Bearer {AUTH_TOKEN}",
    "Content-Type": "application/json"
}


def log(message: str, level: str = "INFO"):
    """Print formatted log message"""
    timestamp = datetime.now().strftime("%H:%M:%S")
    symbols = {"INFO": "[i]", "OK": "[OK]", "FAIL": "[FAIL]", "WARN": "[WARN]"}
    print(f"[{timestamp}] {symbols.get(level, '*')} {message}")


def test_health_check():
    """Test 1: Check if API is accessible"""
    log("Testing API health check...")
    try:
        response = requests.get(f"{BASE_URL}/", headers=HEADERS, timeout=10)
        if response.status_code == 200:
            log(f"API is accessible. Response: {response.json()}", "OK")
            return True
        else:
            log(f"API returned status {response.status_code}", "FAIL")
            return False
    except requests.exceptions.RequestException as e:
        log(f"Cannot connect to API: {e}", "FAIL")
        return False


def test_get_children():
    """Test 2: Get list of children"""
    log("Testing GET /children...")
    try:
        response = requests.get(f"{BASE_URL}/children", headers=HEADERS, timeout=10)
        if response.status_code == 200:
            children = response.json()
            log(f"Got {len(children)} children", "OK")
            for child in children:
                log(f"  - {child.get('name', 'Unknown')}: "
                    f"consumed={child.get('consumed_this_month', 0)}ml, "
                    f"remaining={child.get('remaining', 0)}ml")
            return children
        else:
            log(f"Failed to get children: {response.status_code}", "FAIL")
            return None
    except requests.exceptions.RequestException as e:
        log(f"Request failed: {e}", "FAIL")
        return None


def test_get_child_history(child_id: int):
    """Test 3: Get drink history for a child"""
    log(f"Testing GET /children/{child_id}/history...")
    try:
        response = requests.get(
            f"{BASE_URL}/children/{child_id}/history",
            headers=HEADERS,
            timeout=10
        )
        if response.status_code == 200:
            history = response.json()
            log(f"Got {len(history)} history items", "OK")
            return history
        else:
            log(f"Failed to get history: {response.status_code}", "FAIL")
            return None
    except requests.exceptions.RequestException as e:
        log(f"Request failed: {e}", "FAIL")
        return None


def test_add_drink(child_id: int, amount_ml: int = 100):
    """Test 4: Add a drink and verify response"""
    log(f"Testing POST /children/{child_id}/drink with {amount_ml}ml...")
    try:
        response = requests.post(
            f"{BASE_URL}/children/{child_id}/drink",
            headers=HEADERS,
            json={"amount_ml": amount_ml},
            timeout=10
        )
        if response.status_code == 200:
            data = response.json()
            log(f"Drink added successfully", "OK")
            log(f"  Response: {data}")

            # Check if response contains updated child data
            if "child" in data:
                child = data["child"]
                log(f"  Updated child: consumed={child.get('consumed_this_month')}ml, "
                    f"remaining={child.get('remaining')}ml")
            return data
        else:
            log(f"Failed to add drink: {response.status_code} - {response.text}", "FAIL")
            return None
    except requests.exceptions.RequestException as e:
        log(f"Request failed: {e}", "FAIL")
        return None


def test_delete_drink(drink_id: int):
    """Test 5: Delete a drink record"""
    log(f"Testing DELETE /drinks/{drink_id}...")
    try:
        response = requests.delete(
            f"{BASE_URL}/drinks/{drink_id}",
            headers=HEADERS,
            timeout=10
        )
        if response.status_code == 200:
            data = response.json()
            log(f"Drink deleted successfully", "OK")
            log(f"  Response: {data}")
            return data
        else:
            log(f"Failed to delete drink: {response.status_code} - {response.text}", "FAIL")
            return None
    except requests.exceptions.RequestException as e:
        log(f"Request failed: {e}", "FAIL")
        return None


def test_data_persistence(child_id: int):
    """
    Test 6: CRITICAL - Test if data persists after adding a drink

    This is the main test to detect the bug:
    1. Get current child data
    2. Add a drink
    3. Wait a moment
    4. Get child data again
    5. Compare - if values don't match, data is NOT being saved!
    """
    log("=" * 50)
    log("TESTING DATA PERSISTENCE (Critical Bug Detection)")
    log("=" * 50)

    # Step 1: Get initial state
    log("Step 1: Getting initial child data...")
    children_before = test_get_children()
    if not children_before:
        log("Cannot proceed without children data", "FAIL")
        return False

    child_before = next((c for c in children_before if c["id"] == child_id), None)
    if not child_before:
        log(f"Child with id={child_id} not found", "FAIL")
        return False

    consumed_before = child_before.get("consumed_this_month", 0)
    remaining_before = child_before.get("remaining", 0)
    log(f"  Before: consumed={consumed_before}ml, remaining={remaining_before}ml")

    # Step 2: Add a small test drink
    test_amount = 50  # Small amount for testing
    log(f"Step 2: Adding test drink of {test_amount}ml...")
    add_result = test_add_drink(child_id, test_amount)
    if not add_result:
        log("Failed to add drink", "FAIL")
        return False

    # Check immediate response
    if "child" in add_result:
        immediate_consumed = add_result["child"].get("consumed_this_month", 0)
        immediate_remaining = add_result["child"].get("remaining", 0)
        log(f"  Immediate response: consumed={immediate_consumed}ml, remaining={immediate_remaining}ml")

        expected_consumed = consumed_before + test_amount
        if immediate_consumed != expected_consumed:
            log(f"  WARNING: Expected consumed={expected_consumed}, got {immediate_consumed}", "WARN")

    # Step 3: Wait for potential async save
    log("Step 3: Waiting 2 seconds for data to persist...")
    time.sleep(2)

    # Step 4: Get data again
    log("Step 4: Getting child data again...")
    children_after = test_get_children()
    if not children_after:
        log("Cannot get children data after adding drink", "FAIL")
        return False

    child_after = next((c for c in children_after if c["id"] == child_id), None)
    if not child_after:
        log(f"Child with id={child_id} not found after adding drink", "FAIL")
        return False

    consumed_after = child_after.get("consumed_this_month", 0)
    remaining_after = child_after.get("remaining", 0)
    log(f"  After: consumed={consumed_after}ml, remaining={remaining_after}ml")

    # Step 5: Compare results
    log("Step 5: Comparing results...")
    expected_consumed = consumed_before + test_amount
    expected_remaining = remaining_before - test_amount

    persistence_ok = True

    if consumed_after == expected_consumed:
        log(f"  Consumed: {consumed_before} + {test_amount} = {consumed_after} [PASS]", "OK")
    elif consumed_after == consumed_before:
        log(f"  BUG DETECTED: Consumed unchanged! Expected {expected_consumed}, got {consumed_after}", "FAIL")
        log(f"  This means the backend is NOT saving data to data.json!", "FAIL")
        persistence_ok = False
    else:
        log(f"  Unexpected consumed value: expected {expected_consumed}, got {consumed_after}", "WARN")
        persistence_ok = False

    if remaining_after == expected_remaining:
        log(f"  Remaining: {remaining_before} - {test_amount} = {remaining_after} [PASS]", "OK")
    elif remaining_after == remaining_before:
        log(f"  BUG DETECTED: Remaining unchanged! Expected {expected_remaining}, got {remaining_after}", "FAIL")
        persistence_ok = False
    else:
        log(f"  Unexpected remaining value: expected {expected_remaining}, got {remaining_after}", "WARN")
        persistence_ok = False

    # Final verdict
    log("=" * 50)
    if persistence_ok:
        log("DATA PERSISTENCE TEST PASSED!", "OK")
        log("The backend is correctly saving data.")

        # Clean up - delete the test drink
        if "drink" in add_result:
            drink_id = add_result["drink"].get("id")
            if drink_id:
                log(f"Cleaning up: deleting test drink id={drink_id}")
                test_delete_drink(drink_id)
    else:
        log("DATA PERSISTENCE TEST FAILED!", "FAIL")
        log("The backend is NOT saving data correctly.")
        log("")
        log("Possible causes:")
        log("  1. save_data() is not called after modifying data")
        log("  2. File write permissions issue on data.json")
        log("  3. Data is modified in memory but not written to disk")
        log("")
        log("Fix: Make sure your add_drink endpoint calls save_data() after updating")
    log("=" * 50)

    return persistence_ok


def test_history_consistency(child_id: int):
    """
    Test 7: Check if history matches consumed amount
    """
    log("=" * 50)
    log("TESTING HISTORY CONSISTENCY")
    log("=" * 50)

    # Get child data
    children = test_get_children()
    if not children:
        return False

    child = next((c for c in children if c["id"] == child_id), None)
    if not child:
        log(f"Child with id={child_id} not found", "FAIL")
        return False

    stored_consumed = child.get("consumed_this_month", 0)

    # Get history
    history = test_get_child_history(child_id)
    if history is None:
        return False

    # Calculate consumed from history
    calculated_consumed = sum(item.get("amount_ml", 0) for item in history)

    log(f"Stored consumed_this_month: {stored_consumed}ml")
    log(f"Calculated from history: {calculated_consumed}ml")

    if stored_consumed == calculated_consumed:
        log("History is consistent with stored value", "OK")
        return True
    else:
        log(f"MISMATCH: Stored={stored_consumed}, Calculated={calculated_consumed}", "WARN")
        log("This may indicate data corruption or incomplete history")
        return False


def run_all_tests():
    """Run all tests"""
    print("\n" + "=" * 60)
    print("   COLA TRACKER BACKEND API TESTS")
    print("=" * 60)
    print(f"   Base URL: {BASE_URL}")
    print(f"   Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60 + "\n")

    # Test 1: Health check
    if not test_health_check():
        log("Cannot proceed - API is not accessible", "FAIL")
        return

    print()

    # Test 2: Get children
    children = test_get_children()
    if not children:
        log("Cannot proceed - no children data", "FAIL")
        return

    print()

    # Use first child for testing
    test_child_id = children[0]["id"]
    test_child_name = children[0].get("name", "Unknown")
    log(f"Using child '{test_child_name}' (id={test_child_id}) for tests")

    print()

    # Test 3: Get history
    test_get_child_history(test_child_id)

    print()

    # Test 6: Data persistence (CRITICAL)
    test_data_persistence(test_child_id)

    print()

    # Test 7: History consistency
    test_history_consistency(test_child_id)

    print("\n" + "=" * 60)
    print("   TESTS COMPLETED")
    print("=" * 60 + "\n")


if __name__ == "__main__":
    run_all_tests()
