# PulseCheck Supabase Setup Guide

## Quick Setup (5 minutes)

### Step 1: Access Your Supabase Project
1. Go to: https://supabase.com/dashboard/project/yfpmsaqxxfrqchkduhta/sql
2. You should see the SQL Editor

### Step 2: Create the Database Schema
1. Copy the entire contents of `SUPABASE_SCHEMA.sql`
2. Paste it into the SQL Editor
3. Click **"Run"** (or press Cmd+Enter)
4. Wait for success message: "PulseCheck Supabase schema created successfully!"

**Expected Output:**
- ✅ 8 tables created
- ✅ 17+ RLS policies created
- ✅ 3 views created
- ✅ 3 triggers created
- ✅ 1 storage bucket configured

### Step 3: Verify the Setup
1. Copy the entire contents of `verify_supabase.sql`
2. Paste it into the SQL Editor
3. Click **"Run"**
4. Check the results:

**Expected Results:**
```
✅ Tables: 8/8
✅ RLS Policies: 17+
✅ Views: 3
✅ Storage Bucket: sos-videos
✅ Schema Version: 2.0
```

### Step 4: Create Storage Bucket (Manual)
The SQL script attempts to create the storage bucket, but you may need to verify it manually:

1. Go to: https://supabase.com/dashboard/project/yfpmsaqxxfrqchkduhta/storage/buckets
2. Check if `sos-videos` bucket exists
3. If not, click **"New Bucket"** and configure:
   - **Name:** `sos-videos`
   - **Public:** ❌ Unchecked (MUST be private)
   - **File size limit:** 100 MB
   - **Allowed MIME types:** 
     - `video/mp4`
     - `video/mpeg`
     - `video/quicktime`
     - `video/x-matroska`

### Step 5: Verify Storage Policies
1. Go to storage bucket settings
2. Click on **"Policies"** tab
3. You should see 3 policies:
   - ✅ "Users can upload own videos" (INSERT)
   - ✅ "Users can view own videos" (SELECT)
   - ✅ "Users can delete own videos" (DELETE)

---

## What Each Table Does

### Core Tables (7 total)
1. **user_profiles** - Extended user information beyond auth
2. **emergency_contacts** - User's emergency contacts
3. **sos_alerts** - All SOS alert events
4. **location_updates** - Real-time location tracking during alerts
5. **alert_notifications** - Track notification delivery status
6. **video_recordings** - Video metadata and storage URLs
7. **video_sharing_log** - Track who received each video
8. **schema_version** - Track database migrations

### Views (3 total)
1. **user_alert_stats** - Statistics about user's alerts
2. **video_statistics** - Video storage and usage stats
3. **user_comprehensive_data** - All user data in one place

### Storage
- **sos-videos bucket** - Private storage for encrypted video files

---

## Troubleshooting

### ❌ Error: "relation already exists"
**Solution:** Tables already exist! Run `verify_supabase.sql` to check status.

### ❌ Error: "permission denied"
**Solution:** Make sure you're logged in to the correct Supabase project.

### ❌ Error: "storage bucket not created"
**Solution:** Create the bucket manually (see Step 4 above).

### ⚠️ Warning: "RLS policies missing"
**Solution:** 
1. Go to: https://supabase.com/dashboard/project/yfpmsaqxxfrqchkduhta/database/tables
2. For each table, click **"Enable RLS"**
3. Re-run `SUPABASE_SCHEMA.sql`

### 🔍 Check Current Status
Run this query to see what exists:
```sql
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND table_type = 'BASE TABLE'
ORDER BY table_name;
```

---

## After Setup is Complete

### Test the Connection from Android App
1. Build the app: `./gradlew assembleDebug`
2. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Launch app and register a new user
4. Go to Supabase Dashboard > Authentication > Users
5. Verify new user was created

### Test Video Upload
1. In the app, hold the SOS button for 15 seconds (3 × 5-second segments)
2. Go to Supabase Dashboard > Storage > sos-videos
3. Check for encrypted video files in folder: `{user-uuid}/`
4. Go to SQL Editor and run:
```sql
SELECT * FROM video_recordings ORDER BY recorded_at DESC LIMIT 5;
```

### Test SMS with Video Links
1. Add an emergency contact
2. Trigger SOS alert
3. Check SMS received by contact
4. Verify it contains secure video link (72-hour expiry)

---

## Security Checklist

✅ **Row Level Security (RLS)** enabled on all tables  
✅ **Storage bucket** is private (not public)  
✅ **Video files** are encrypted (AES-256-GCM)  
✅ **Signed URLs** expire after 72 hours  
✅ **Users** can only access their own data  
✅ **Contacts** receive time-limited links only  

---

## Quick Reference

**Project URL:** https://yfpmsaqxxfrqchkduhta.supabase.co  
**Dashboard:** https://supabase.com/dashboard/project/yfpmsaqxxfrqchkduhta  
**SQL Editor:** https://supabase.com/dashboard/project/yfpmsaqxxfrqchkduhta/sql  
**Storage:** https://supabase.com/dashboard/project/yfpmsaqxxfrqchkduhta/storage/buckets  

**Configuration Files:**
- `app/src/main/kotlin/com/example/pulsecheck/SupabaseConfig.kt`
- `app/src/main/kotlin/com/example/pulsecheck/SupabaseClient.kt`

**Documentation:**
- `SUPABASE_SCHEMA.sql` - Complete database schema
- `verify_supabase.sql` - Verification script
- `SUPABASE_VERIFICATION.md` - Detailed verification guide

---

## Need Help?

1. Run `verify_supabase.sql` and share the results
2. Check Supabase logs: Dashboard > Logs
3. Check app logs: `adb logcat | grep PulseCheck`

---

**Status:** ⚠️ Setup Required  
**Next Step:** Run `SUPABASE_SCHEMA.sql` in SQL Editor
