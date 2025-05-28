#!/usr/bin/env python3
"""
Test script to verify Swagger setup is working correctly
"""
import requests
import json
import time

# Configuration
BASE_URL = "http://localhost"
USER_API = f"{BASE_URL}/api/users"

def test_swagger_docs():
    """Test if Swagger documentation is accessible"""
    try:
        response = requests.get(f"{USER_API}/docs/")
        print(f"Swagger Docs Status: {response.status_code}")
        if response.status_code == 200:
            print("✅ Swagger documentation is accessible")
            return True
        else:
            print("❌ Swagger documentation is not accessible")
            print(f"Response: {response.text}")
            return False
    except Exception as e:
        print(f"❌ Error accessing Swagger docs: {e}")
        return False

def test_health_endpoint():
    """Test health check endpoint"""
    try:
        response = requests.get(f"{USER_API}/health")
        print(f"Health Check Status: {response.status_code}")
        if response.status_code == 200:
            print("✅ Health endpoint is working")
            print(f"Response: {response.json()}")
            return True
        else:
            print("❌ Health endpoint is not working")
            return False
    except Exception as e:
        print(f"❌ Error accessing health endpoint: {e}")
        return False

def test_api_registration():
    """Test user registration via new API"""
    try:
        test_user = {
            "username": f"testuser_{int(time.time())}",
            "password": "testpass123",
            "email": "test@example.com",
            "first_name": "Test",
            "last_name": "User",
            "role": "student"
        }
        
        response = requests.post(f"{USER_API}/auth/register", json=test_user)
        print(f"Registration Status: {response.status_code}")
        
        if response.status_code == 201:
            print("✅ User registration working")
            return test_user
        else:
            print("❌ User registration failed")
            print(f"Response: {response.text}")
            return None
    except Exception as e:
        print(f"❌ Error during registration: {e}")
        return None

def test_api_login(user_data):
    """Test user login via new API"""
    if not user_data:
        return None
        
    try:
        login_data = {
            "username": user_data["username"],
            "password": user_data["password"]
        }
        
        response = requests.post(f"{USER_API}/auth/login", json=login_data)
        print(f"Login Status: {response.status_code}")
        
        if response.status_code == 200:
            token_data = response.json()
            print("✅ User login working")
            print(f"Token received: {token_data['token'][:20]}...")
            return token_data['token']
        else:
            print("❌ User login failed")
            print(f"Response: {response.text}")
            return None
    except Exception as e:
        print(f"❌ Error during login: {e}")
        return None

def test_protected_endpoint(token):
    """Test protected endpoint with JWT token"""
    if not token:
        return False
        
    try:
        headers = {"Authorization": f"Bearer {token}"}
        response = requests.get(f"{USER_API}/users/", headers=headers)
        print(f"Protected Endpoint Status: {response.status_code}")
        
        if response.status_code == 200:
            print("✅ Protected endpoint working")
            users = response.json()
            print(f"Found {len(users)} users")
            return True
        else:
            print("❌ Protected endpoint failed")
            print(f"Response: {response.text}")
            return False
    except Exception as e:
        print(f"❌ Error accessing protected endpoint: {e}")
        return False

def main():
    """Run all tests"""
    print("🚀 Testing Swagger Setup for User Service")
    print("=" * 50)
    
    # Test 1: Swagger Documentation
    print("\n1. Testing Swagger Documentation...")
    swagger_ok = test_swagger_docs()
    
    # Test 2: Health Check
    print("\n2. Testing Health Endpoint...")
    health_ok = test_health_endpoint()
    
    # Test 3: User Registration
    print("\n3. Testing User Registration...")
    test_user = test_api_registration()
    
    # Test 4: User Login
    print("\n4. Testing User Login...")
    token = test_api_login(test_user)
    
    # Test 5: Protected Endpoint
    print("\n5. Testing Protected Endpoint...")
    protected_ok = test_protected_endpoint(token)
    
    # Summary
    print("\n" + "=" * 50)
    print("📊 TEST SUMMARY")
    print("=" * 50)
    print(f"Swagger Docs:      {'✅ PASS' if swagger_ok else '❌ FAIL'}")
    print(f"Health Endpoint:   {'✅ PASS' if health_ok else '❌ FAIL'}")
    print(f"User Registration: {'✅ PASS' if test_user else '❌ FAIL'}")
    print(f"User Login:        {'✅ PASS' if token else '❌ FAIL'}")
    print(f"Protected API:     {'✅ PASS' if protected_ok else '❌ FAIL'}")
    
    if swagger_ok:
        print(f"\n🎉 Swagger UI is available at: {USER_API}/docs/")
    else:
        print("\n🔧 TROUBLESHOOTING TIPS:")
        print("1. Make sure Docker containers are running: docker-compose ps")
        print("2. Check nginx logs: docker-compose logs nginx-gateway")
        print("3. Check user-service logs: docker-compose logs user-service")
        print("4. Verify nginx config is properly mounted")

if __name__ == "__main__":
    main()