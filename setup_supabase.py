#!/usr/bin/env python3
"""
PulseCheck Supabase Setup Script
Executes database schema via Supabase REST API
"""

import requests
import json
import sys

# Configuration
PROJECT_REF = "yfpmsaqxxfrqchkduhta"
SUPABASE_URL = f"https://{PROJECT_REF}.supabase.co"
ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlmcG1zYXF4eGZycWNoa2R1aHRhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA4OTcxMTYsImV4cCI6MjEwNjQ3MzExNn0.81aCYKib-biNwMi9WsfWbrPtkbw_3-cqaQ4aJqx-H20"

def print_header():
    print("=" * 60)
    print("PulseCheck Supabase Setup")
    print("=" * 60)
    print(f"Project: {PROJECT_REF}")
    print(f"URL: {SUPABASE_URL}")
    print("=" * 60)
    print()

def test_connection():
    """Test connection to Supabase"""
    print("🔌 Testing connection...")
    try:
        headers = {
            "apikey": ANON_KEY,
            "Authorization": f"Bearer {ANON_KEY}"
        }
        
        response = requests.get(
            f"{SUPABASE_URL}/rest/v1/",
            headers=headers,
            timeout=10
        )
        
        if response.status_code == 200:
            print("✅ Connection successful!")
            return True
        else:
            print(f"❌ Connection failed: {response.status_code}")
            print(f"   Response: {response.text}")
            return False
    except Exception as e:
        print(f"❌ Connection error: {e}")
        return False

def read_schema_file():
    """Read SQL schema file"""
    print("📄 Reading schema file...")
    try:
        with open('SUPABASE_SCHEMA.sql', 'r') as f:
            schema = f.read()
        print(f"✅ Schema loaded ({len(schema)} characters)")
        return schema
    except Exception as e:
        print(f"❌ Failed to read schema: {e}")
        return None

def create_tables_via_dashboard():
    """Instructions for manual creation"""
    print()
    print("=" * 60)
    print("⚠️  MANUAL SETUP REQUIRED")
    print("=" * 60)
    print()
    print("The anon key doesn't have permissions to create tables.")
    print("Please follow these steps:")
    print()
    print("1. Go to Supabase Dashboard:")
    print(f"   https://supabase.com/dashboard/project/{PROJECT_REF}")
    print()
    print("2. Click 'SQL Editor' in left sidebar")
    print()
    print("3. Click 'New Query'")
    print()
    print("4. Copy the contents of SUPABASE_SCHEMA.sql")
    print()
    print("5. Paste into SQL Editor and click 'Run'")
    print()
    print("6. Verify tables created in 'Table Editor'")
    print()
    print("=" * 60)
    print()

def create_storage_bucket():
    """Instructions for storage bucket creation"""
    print("=" * 60)
    print("📦 STORAGE BUCKET SETUP")
    print("=" * 60)
    print()
    print("Create 'sos-videos' bucket:")
    print()
    print("1. Go to Storage section:")
    print(f"   https://supabase.com/dashboard/project/{PROJECT_REF}/storage/buckets")
    print()
    print("2. Click 'Create a new bucket'")
    print()
    print("3. Configure:")
    print("   Name: sos-videos")
    print("   Public: ❌ UNCHECK (must be private!)")
    print("   File size limit: 100 MB")
    print("   Allowed MIME types:")
    print("     - video/mp4")
    print("     - video/quicktime")
    print("     - video/mpeg")
    print("     - video/x-matroska")
    print()
    print("4. Click 'Create bucket'")
    print()
    print("=" * 60)
    print()

def main():
    print_header()
    
    # Test connection
    if not test_connection():
        print()
        print("❌ Cannot connect to Supabase. Please check:")
        print("   - Internet connection")
        print("   - Project ID is correct")
        print("   - Anon key is valid")
        sys.exit(1)
    
    print()
    
    # Read schema
    schema = read_schema_file()
    if not schema:
        sys.exit(1)
    
    print()
    
    # Provide manual instructions
    create_tables_via_dashboard()
    create_storage_bucket()
    
    print("=" * 60)
    print("✅ Setup guide complete!")
    print("=" * 60)
    print()
    print("After completing the manual steps above:")
    print()
    print("1. Build the app:")
    print("   cd /Users/mark/Pulse_Check")
    print("   ./gradlew assembleDebug")
    print()
    print("2. Install on device:")
    print("   adb install -r app/build/outputs/apk/debug/app-debug.apk")
    print()
    print("3. Test video recording and SOS")
    print()
    print("=" * 60)

if __name__ == "__main__":
    main()
