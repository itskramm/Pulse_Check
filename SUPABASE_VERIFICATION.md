# PulseCheck Supabase Verification Guide

**Status:** Schema submitted for execution  
**Project ID:** yfpmsaqxxfrqchkduhta  
**Date:** September 30, 2026

---

## ⚠️ Important Notice

The schema script has been submitted but **execution result was not confirmed**. You MUST verify in SQL Editor before proceeding.

---

## 🔍 Step 1: Verify Schema Execution

### 1.1 Check SQL Editor Output

Go to: https://supabase.com/dashboard/project/yfpmsaqxxfrqchkduhta/sql

**Look for:**
- ✅ "Success" message
- ✅ No error messages
- ❌ Any warnings or failures

**If errors occurred:**
- Note the error message
- Check if tables already existed
- Review the troubleshooting section below

---

### 1.2 Verify Tables Created

Go to: https://supabase.com/dashboard/project/yfpmsaqxxfrqchkduhta/editor

**Expected 8 tables:**
```
✅ user_profiles
✅ emergency_contacts
✅ sos_alerts
✅ location_updates
✅ alert_notifications
✅ video_recordings
✅ video_sharing_log
✅ schema_version
```

**For each table, verify:**
- Table exists and is visible
- 🔒 RLS icon shows "enabled"
- Click table → Check columns match schema

---

### 1.3 Verify Views Created

In Table Editor sidebar, look for **Views** section:

```
✅ user_alert_stats
✅ video_statistics
✅ user_comprehensive_data
```

**Note:** Views appear in a separate section from tables.

---

### 1.4 Verify Storage Bucket

Go to: https://supabase.com/dashboard/project/yfpmsaqxxfrqchkduhta/storage/buckets

**Check for:**
```
✅ sos-videos bucket exists
✅ 🔒 Private (not public)
✅ 100 MB file size limit
✅ Allowed MIME types: video/*
```

**If bucket doesn't exist:**
1. Click "Create a new bucket"
2. Name: `sos-videos`
3. Public: ❌ **UNCHECK**
4. File size limit: `100 MB` (104857600 bytes)
5. Allowed MIME types:
   - `video/mp4`
   - `video/quicktime`
   - `video/mpeg`
   - `video/x-matroska`

---

## 📋 Step 2: Verify Table Schemas

### 2.1 Check user_profiles

**Required columns:**
```sql
id                 UUID PRIMARY KEY
user_id            UUID (unique, references auth.users)
full_name          TEXT
phone              TEXT
email              TEXT
gender             TEXT
permanent_address  TEXT
work_address       TEXT
other_addresses    TEXT
avatar_url         TEXT
created_at         TIMESTAMP WITH TIME ZONE
updated_at         TIMESTAMP WITH TIME ZONE
```

**To verify:**
1. Click `user_profiles` table
2. Click "Structure" tab
3. Compare with list above

---

### 2.2 Check emergency_contacts

**Required columns:**
```sql
id              UUID PRIMARY KEY
user_id         UUID (references auth.users)
name            TEXT
phone           TEXT
email           TEXT
affiliation     TEXT
gender          TEXT
home_address    TEXT
work_address    TEXT
other_address   TEXT
avatar_url      TEXT
created_at      TIMESTAMP WITH TIME ZONE
updated_at      TIMESTAMP WITH TIME ZONE
```

---

### 2.3 Check sos_alerts

**Required columns:**
```sql
id                  UUID PRIMARY KEY
user_id             UUID (references auth.users)
trigger_type        TEXT
latitude            DOUBLE PRECISION
longitude           DOUBLE PRECISION
location            TEXT
contacts_notified   INTEGER
status              TEXT (active/resolved/cancelled)
created_at          TIMESTAMP WITH TIME ZONE
resolved_at         TIMESTAMP WITH TIME ZONE
```

---

### 2.4 Check video_recordings

**Required columns:**
```sql
id                UUID PRIMARY KEY
user_id           UUID (references auth.users)
alert_id          UUID (references sos_alerts, nullable)
file_name         TEXT
file_path         TEXT
storage_url       TEXT
duration_ms       INTEGER
file_size_bytes   BIGINT
recorded_at       TIMESTAMP WITH TIME ZONE
uploaded_at       TIMESTAMP WITH TIME ZONE
shared_count      INTEGER
notes             TEXT
metadata          JSONB
created_at        TIMESTAMP WITH TIME ZONE
updated_at        TIMESTAMP WITH TIME ZONE
```

---

### 2.5 Check video_sharing_log

**Required columns:**
```sql
id               UUID PRIMARY KEY
video_id         UUID (references video_recordings)
contact_id       UUID (references emergency_contacts, nullable)
contact_name     TEXT
contact_email    TEXT
contact_phone    TEXT
sharing_method   TEXT
shared_at        TIMESTAMP WITH TIME ZONE
delivery_status  TEXT
error_message    TEXT
created_at       TIMESTAMP WITH TIME ZONE
```

---

## 🔐 Step 3: Verify RLS Policies

### 3.1 Check user_profiles RLS

Go to: Table Editor → `user_profiles` → "RLS" tab

**Expected policies:**
```
✅ Users can view own profile (SELECT)
✅ Users can insert own profile (INSERT)
✅ Users can update own profile (UPDATE)
```

**Test SQL:**
```sql
-- Should work (your own profile)
SELECT * FROM user_profiles WHERE user_id = auth.uid();

-- Should return empty (other users' profiles)
SELECT * FROM user_profiles WHERE user_id != auth.uid();
```

---

### 3.2 Check video_recordings RLS

**Expected policies:**
```
✅ Users can view own videos (SELECT)
✅ Users can insert own videos (INSERT)
✅ Users can update own videos (UPDATE)
✅ Users can delete own videos (DELETE)
```

---

### 3.3 Check Storage Bucket RLS

Go to: Storage → `sos-videos` → Policies

**Expected policies:**
```
✅ Users can upload own videos (INSERT)
   - Path must start with their user_id
✅ Users can view own videos (SELECT)
   - Path must start with their user_id
✅ Users can delete own videos (DELETE)
   - Path must start with their user_id
```

**Path format:**
```
<user-uuid>/<video-id>.encrypted
Example: a3f8d91e-4c2a-4b3f-9d7e-123456789abc/video-123.encrypted
```

---

## ⚙️ Step 4: Verify Functions & Triggers

### 4.1 Check update_updated_at_column function

Run this query:
```sql
SELECT routine_name 
FROM information_schema.routines 
WHERE routine_schema = 'public' 
  AND routine_name = 'update_updated_at_column';
```

**Expected:** 1 row returned

---

### 4.2 Check triggers exist

Run this query:
```sql
SELECT trigger_name, event_object_table 
FROM information_schema.triggers 
WHERE trigger_schema = 'public';
```

**Expected triggers:**
```
✅ update_user_profiles_updated_at (on user_profiles)
✅ update_emergency_contacts_updated_at (on emergency_contacts)
✅ video_recordings_updated_at (on video_recordings)
```

---

### 4.3 Verify broken trigger removed

**Issue:** Original schema had `notify_video_uploaded()` trigger that referenced nonexistent columns.

Run this query:
```sql
SELECT trigger_name 
FROM information_schema.triggers 
WHERE trigger_name = 'video_uploaded_notification';
```

**Expected:** 0 rows (trigger should NOT exist)

**If it exists:**
```sql
DROP TRIGGER IF EXISTS video_uploaded_notification ON video_recordings;
```

---

## 📊 Step 5: Verify Views Work

### 5.1 Test video_statistics view

```sql
SELECT * FROM video_statistics LIMIT 5;
```

**Expected:**
- Query executes without error
- Returns 0 rows (no data yet) or existing data
- Columns: user_id, total_videos, total_storage_bytes, total_duration_ms, etc.

---

### 5.2 Test user_comprehensive_data view

```sql
SELECT * FROM user_comprehensive_data LIMIT 5;
```

**Expected:**
- Query executes without error
- Shows user profiles with aggregated counts

---

### 5.3 Test user_alert_stats view

```sql
SELECT * FROM user_alert_stats LIMIT 5;
```

**Expected:**
- Query executes without error
- Shows alert statistics per user

---

## 🔧 Step 6: Verify security_invoker on Views

**Important:** Views should use `security_invoker` to respect RLS policies.

Run this query:
```sql
SELECT table_name, security_type 
FROM information_schema.views 
WHERE table_schema = 'public' 
  AND table_name IN ('video_statistics', 'user_comprehensive_data', 'user_alert_stats');
```

**Expected:** All views show `security_invoker = INVOKER`

**If not:**
```sql
ALTER VIEW video_statistics SET (security_invoker = true);
ALTER VIEW user_comprehensive_data SET (security_invoker = true);
ALTER VIEW user_alert_stats SET (security_invoker = true);
```

---

## 🛠️ Troubleshooting Common Issues

### Issue 1: "Table already exists"

**Cause:** Schema ran multiple times or tables exist from previous setup.

**Solution:**

**Option A:** Check existing columns match
```sql
-- Check user_profiles columns
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'user_profiles' 
ORDER BY ordinal_position;
```

**Option B:** Drop and recreate (⚠️ DELETES ALL DATA)
```sql
-- Only do this if you're sure!
DROP TABLE IF EXISTS video_sharing_log CASCADE;
DROP TABLE IF EXISTS video_recordings CASCADE;
DROP TABLE IF EXISTS alert_notifications CASCADE;
DROP TABLE IF EXISTS location_updates CASCADE;
DROP TABLE IF EXISTS sos_alerts CASCADE;
DROP TABLE IF EXISTS emergency_contacts CASCADE;
DROP TABLE IF EXISTS user_profiles CASCADE;
DROP TABLE IF EXISTS schema_version CASCADE;

-- Then re-run SUPABASE_SCHEMA.sql
```

---

### Issue 2: Missing columns in existing tables

**Problem:** `CREATE TABLE IF NOT EXISTS` doesn't add missing columns to existing tables.

**Solution:** Add columns manually
```sql
-- Example: Add missing column to existing table
ALTER TABLE video_recordings 
ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;

ALTER TABLE video_recordings 
ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
```

**Check what columns are missing:**
```sql
-- Compare expected vs actual columns
SELECT column_name 
FROM information_schema.columns 
WHERE table_name = 'video_recordings' 
ORDER BY ordinal_position;
```

---

### Issue 3: RLS policies not working

**Symptoms:**
- Users can see other users' data
- Insert/update/delete fails unexpectedly

**Debug:**
```sql
-- Check if RLS is enabled
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE schemaname = 'public' 
  AND tablename = 'video_recordings';
-- rowsecurity should be TRUE

-- List all policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'video_recordings';
```

**Fix:**
```sql
-- Enable RLS if disabled
ALTER TABLE video_recordings ENABLE ROW LEVEL SECURITY;

-- Recreate policies (see SUPABASE_SCHEMA.sql for full policy definitions)
```

---

### Issue 4: Storage bucket policies not working

**Symptoms:**
- Cannot upload videos
- 403 Forbidden errors

**Solution:**

1. Check bucket exists and is private:
   - Storage → sos-videos → Settings
   - Public should be OFF

2. Verify policies exist:
   - Storage → sos-videos → Policies
   - Should see 3 policies (upload, view, delete)

3. Test path format:
   ```
   Correct: {user-uuid}/video-123.encrypted
   Wrong:   video-123.encrypted (missing user prefix)
   ```

4. Recreate policies if missing:
   ```sql
   -- See storage policies section in SUPABASE_SCHEMA.sql
   ```

---

### Issue 5: Views return no data

**Cause:** RLS on underlying tables prevents view from accessing data.

**Solution:**

1. Views should use `security_invoker`:
   ```sql
   ALTER VIEW video_statistics SET (security_invoker = true);
   ```

2. Test underlying table access:
   ```sql
   -- Can you query the table directly?
   SELECT * FROM video_recordings WHERE user_id = auth.uid();
   ```

---

## ✅ Final Verification Checklist

### Database Structure
- [ ] 8 tables created (including schema_version)
- [ ] 3 views created
- [ ] 1 function created (update_updated_at_column)
- [ ] 3 triggers created
- [ ] RLS enabled on all application tables
- [ ] All policies created (17 total)

### Storage
- [ ] sos-videos bucket exists
- [ ] Bucket is private (not public)
- [ ] File size limit: 100 MB
- [ ] MIME types configured
- [ ] 3 storage policies created

### Security
- [ ] RLS works (users can only see own data)
- [ ] Storage policies work (path validation)
- [ ] Views use security_invoker
- [ ] No broken triggers exist

### Functionality
- [ ] Can query all tables without error
- [ ] Views return results (or empty if no data)
- [ ] Functions execute without error
- [ ] Triggers fire on UPDATE

---

## 🧪 Test Data Insertion

Once verification passes, test with sample data:

```sql
-- Insert test user profile (replace with actual auth user ID)
INSERT INTO user_profiles (user_id, full_name, phone, email)
VALUES (auth.uid(), 'Test User', '+639171234567', 'test@example.com');

-- Insert test emergency contact
INSERT INTO emergency_contacts (user_id, name, phone, email, affiliation)
VALUES (auth.uid(), 'John Doe', '+639171234568', 'john@example.com', 'Friend');

-- Insert test SOS alert
INSERT INTO sos_alerts (user_id, trigger_type, latitude, longitude, location, status)
VALUES (auth.uid(), 'dead_mans_switch', 14.5995, 120.9842, 'Manila, Philippines', 'active');

-- Verify inserts worked
SELECT * FROM user_profiles WHERE user_id = auth.uid();
SELECT * FROM emergency_contacts WHERE user_id = auth.uid();
SELECT * FROM sos_alerts WHERE user_id = auth.uid();
```

**Expected:** All queries return your test data.

---

## 📱 App Integration Verification

After Supabase verification passes:

### 1. Build & Install App
```bash
cd /Users/mark/Pulse_Check
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Check Logs
```bash
adb logcat -c
adb logcat | grep -E "Supabase|PulseCheckApp"
```

**Expected output:**
```
✅ Supabase initialized successfully
✅ Supabase configured: https://yfpmsaqxxfrqchkduhta.supabase.co
```

**If you see:**
```
⚠️ WARNING: Supabase credentials not configured!
```
Then credentials weren't saved properly in SupabaseConfig.kt

### 3. Test Video Recording
1. Open app
2. Hold SOS button for 10 seconds
3. Check logs for:
   ```
   📹 Recording segment 1...
   ✅ Segment 1 complete
   📹 Recording segment 2...
   ✅ Segment 2 complete
   ```

### 4. Test Cloud Upload
```bash
adb logcat | grep "CloudSync"
```

**Expected:**
```
☁️ Uploading to cloud...
✅ Video uploaded successfully
```

### 5. Verify in Supabase
- Storage → sos-videos → Should see uploaded files
- Table Editor → video_recordings → Should see metadata entries

---

## 📞 Support

If verification fails at any step:

1. **Capture error messages** from SQL Editor
2. **Screenshot** the error
3. **Note which step failed**
4. **Check troubleshooting section** above

Common issues are usually:
- Missing columns in existing tables
- RLS policies not created
- Storage bucket not configured
- Wrong path format for videos

---

## ✅ Success Indicators

### Supabase is ready when:
- ✅ All tables exist with correct columns
- ✅ All views query successfully
- ✅ RLS is enabled and working
- ✅ Storage bucket exists and is private
- ✅ Sample data can be inserted
- ✅ App connects without errors
- ✅ Videos upload successfully

### You're ready to test when:
- ✅ All verification steps pass
- ✅ App logs show "Supabase initialized"
- ✅ No errors in logcat

---

## 🎯 Next Steps After Verification

1. **Test SOS flow end-to-end**
   - Record videos (hold 15 seconds)
   - Trigger SOS
   - Verify SMS with video links
   - Click links to test playback

2. **Test with multiple contacts**
   - Add 3-5 contacts
   - Verify all receive SMS
   - Check carrier delays working

3. **Test edge cases**
   - No internet (videos save locally)
   - Poor signal (retry logic)
   - Large videos (>10 segments)
   - Link expiry (after 72 hours)

4. **Production readiness**
   - Release build configuration
   - APK signing
   - ProGuard setup
   - Version management

---

*Verification guide created: September 30, 2026*  
*Project: yfpmsaqxxfrqchkduhta*
