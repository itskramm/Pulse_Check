# Supabase Integration for PulseCheck

## Overview

This document describes the Supabase integration for PulseCheck, providing cloud backup and synchronization capabilities for emergency contacts, SOS alerts, and real-time location tracking.

---

## Features

### ✅ Implemented Features

1. **User Authentication**
   - Email/password signup and signin
   - Session management
   - Secure authentication with Supabase Auth

2. **Emergency Contacts Sync**
   - Upload local contacts to cloud
   - Download contacts from cloud
   - Automatic sync on changes
   - Bi-directional synchronization

3. **SOS Alert Tracking**
   - Create cloud records of SOS alerts
   - Track alert status (active/resolved/cancelled)
   - Store trigger type and location
   - Contact notification tracking

4. **Real-time Location Updates**
   - Stream location updates during active alerts
   - Store location history in cloud
   - Real-time subscribers can track user location

5. **Alert History**
   - View complete alert history from cloud
   - Filter by date, status, trigger type
   - Export functionality

6. **Configuration UI**
   - Easy setup screen for Supabase credentials
   - Enable/disable cloud sync toggle
   - Connection testing
   - Sync status indicators

---

## Setup Instructions

### 1. Create Supabase Project

1. Go to [https://supabase.com](https://supabase.com)
2. Sign up or log in
3. Click "New Project"
4. Choose your organization
5. Enter project details:
   - **Name**: PulseCheck (or your preferred name)
   - **Database Password**: Create a strong password (save it!)
   - **Region**: Choose closest to your users
6. Click "Create new project"
7. Wait for project to be ready (~2 minutes)

### 2. Run Database Schema

1. In your Supabase dashboard, go to **SQL Editor**
2. Click "New Query"
3. Copy the entire contents of `SUPABASE_SCHEMA.sql`
4. Paste into the SQL editor
5. Click "Run" or press Cmd/Ctrl + Enter
6. Verify you see: `PulseCheck Supabase schema created successfully!`

### 3. Get API Credentials

1. In Supabase dashboard, go to **Settings** → **API**
2. Find these two values:
   - **Project URL**: `https://xxxxx.supabase.co`
   - **anon public key**: Long string starting with `eyJ...`
3. Copy both values (you'll need them next)

### 4. Configure PulseCheck App

#### Option A: Via App Settings (Recommended)

1. Open PulseCheck app
2. Go to **Home** → **Settings** → **Cloud Sync** (or **Supabase Settings**)
3. Paste your **Project URL** in the URL field
4. Paste your **anon key** in the Key field
5. Tap "Save Credentials"
6. Tap "Test Connection" to verify
7. Toggle "Enable Cloud Sync" to ON

#### Option B: Via Code (For Developers)

Edit `app/src/main/kotlin/com/example/pulsecheck/SupabaseConfig.kt`:

```kotlin
private const val SUPABASE_URL = "https://xxxxx.supabase.co"
private const val SUPABASE_KEY = "eyJhbGc..."
```

Then rebuild the app.

### 5. Enable Real-time (Optional)

For live location tracking during SOS alerts:

1. In Supabase dashboard, go to **Database** → **Replication**
2. Click on `location_updates` table
3. Enable replication
4. Select "Insert" events
5. Save changes

---

## Usage

### User Registration with Supabase

```kotlin
lifecycleScope.launch {
    supabaseManager.signUp(
        email = "user@example.com",
        password = "securePassword123",
        fullName = "John Doe",
        phone = "+1234567890",
        callback = object : SupabaseManager.AuthCallback {
            override fun onSuccess(userId: String?) {
                // User created successfully
                Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
            }
            
            override fun onFailure(error: String) {
                // Handle error
                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
            }
        }
    )
}
```

### Sign In

```kotlin
lifecycleScope.launch {
    supabaseManager.signIn(
        email = "user@example.com",
        password = "securePassword123",
        callback = object : SupabaseManager.AuthCallback {
            override fun onSuccess(userId: String?) {
                // Signed in successfully
            }
            
            override fun onFailure(error: String) {
                // Handle error
            }
        }
    )
}
```

### Sync Contacts to Cloud

```kotlin
lifecycleScope.launch {
    val localContacts = contactDbHelper.getAllContacts()
    
    supabaseManager.syncContacts(
        contacts = localContacts,
        callback = object : SupabaseManager.SyncCallback {
            override fun onSuccess(count: Int) {
                Toast.makeText(context, "$count contacts synced", Toast.LENGTH_SHORT).show()
            }
            
            override fun onFailure(error: String) {
                Toast.makeText(context, "Sync failed: $error", Toast.LENGTH_LONG).show()
            }
        }
    )
}
```

### Create SOS Alert in Cloud

```kotlin
lifecycleScope.launch {
    supabaseManager.createSOSAlert(
        triggerType = "dead_mans_switch",
        latitude = 40.7128,
        longitude = -74.0060,
        location = "New York, NY",
        contactsNotified = 5,
        callback = object : SupabaseManager.AlertCallback {
            override fun onSuccess(alertId: String?) {
                // Alert created with ID: alertId
            }
            
            override fun onFailure(error: String) {
                // Handle error
            }
        }
    )
}
```

### Update Location During SOS

```kotlin
lifecycleScope.launch {
    supabaseManager.updateLocation(
        latitude = 40.7128,
        longitude = -74.0060,
        accuracy = 10.5f,
        speed = 2.3f,
        callback = object : SupabaseManager.LocationCallback {
            override fun onSuccess() {
                // Location updated
            }
            
            override fun onFailure(error: String) {
                // Handle error
            }
        }
    )
}
```

### Subscribe to Real-time Location Updates

```kotlin
supabaseManager.subscribeToLocationUpdates(
    alertId = "alert-uuid",
    callback = object : SupabaseManager.RealtimeLocationCallback {
        override fun onLocationUpdate(data: String) {
            // New location received
            Log.d("Location", "Update: $data")
        }
        
        override fun onError(error: String) {
            Log.e("Location", "Error: $error")
        }
    }
)
```

---

## Database Schema

### Tables

1. **user_profiles**
   - Extended user profile information
   - Links to Supabase Auth users
   - Stores name, phone, email, addresses

2. **emergency_contacts**
   - Emergency contacts for each user
   - Phone, email, affiliation
   - Avatar URL support

3. **sos_alerts**
   - Records of all SOS alerts
   - Trigger type, location, status
   - Timestamps for created/resolved

4. **location_updates**
   - Real-time location tracking
   - Links to active SOS alerts
   - GPS coordinates, accuracy, speed

5. **alert_notifications**
   - Tracks which contacts were notified
   - Notification type (SMS/Email/App)
   - Delivery status

### Row Level Security (RLS)

All tables have RLS enabled:
- Users can only access their own data
- No user can see another user's contacts or alerts
- Authentication required for all operations

---

## Security

### Data Protection

1. **Authentication**: Supabase Auth with JWT tokens
2. **Row Level Security**: PostgreSQL RLS policies
3. **HTTPS Only**: All API calls encrypted in transit
4. **Local + Cloud**: Data stored locally first, synced to cloud
5. **Graceful Degradation**: App works offline if cloud unavailable

### Best Practices

- Never commit API keys to Git
- Use environment variables or secure storage
- Rotate keys if compromised
- Enable MFA on Supabase account
- Monitor usage in Supabase dashboard

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    PulseCheck App                        │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────┐         ┌──────────────┐              │
│  │   Local DB   │◄────────┤SupabaseManager│              │
│  │   (SQLite)   │         │    (Sync)     │              │
│  └──────────────┘         └───────┬───────┘              │
│                                    │                      │
└────────────────────────────────────┼──────────────────────┘
                                     │
                                     │ HTTPS / WSS
                                     │
                         ┌───────────▼────────────┐
                         │   Supabase Cloud       │
                         ├────────────────────────┤
                         │  • PostgreSQL DB       │
                         │  • Auth Service        │
                         │  • Realtime Server     │
                         │  • Storage (optional)  │
                         └────────────────────────┘
```

### Data Flow

1. **Local First**: All data stored locally in SQLite
2. **Background Sync**: Periodically sync to Supabase when online
3. **Conflict Resolution**: Last-write-wins strategy
4. **Real-time Updates**: WebSocket for live location tracking
5. **Offline Support**: Full functionality without internet

---

## Troubleshooting

### Connection Issues

**Problem**: "Failed to connect to Supabase"

**Solutions**:
- Verify Project URL is correct (starts with `https://`)
- Check anon key is valid (long string starting with `eyJ`)
- Ensure internet connection is active
- Verify Supabase project is active (not paused)

### Authentication Errors

**Problem**: "User not authenticated"

**Solutions**:
- Sign in to Supabase account first
- Check email confirmation if required
- Verify password is correct
- Try signing out and back in

### Sync Failures

**Problem**: "Failed to sync contacts"

**Solutions**:
- Check you're authenticated
- Verify RLS policies are set correctly
- Run SQL schema if tables don't exist
- Check Supabase logs for errors

### Real-time Not Working

**Problem**: Location updates not received

**Solutions**:
- Enable replication for `location_updates` table
- Check WebSocket connection in network tab
- Verify alert is active
- Restart the channel subscription

---

## Performance Considerations

### Optimization Tips

1. **Batch Operations**: Sync multiple contacts at once
2. **Debouncing**: Limit location update frequency (60s recommended)
3. **Pagination**: Load alert history in pages
4. **Caching**: Cache frequently accessed data locally
5. **Connection Pooling**: Reuse Supabase client instance

### Rate Limits

Supabase free tier limits:
- 500 MB database storage
- 1 GB file storage
- 2 GB bandwidth per month
- 50,000 monthly active users

For production, upgrade to Pro plan.

---

## Future Enhancements

### Planned Features

- [ ] Automatic conflict resolution
- [ ] Profile picture uploads to Supabase Storage
- [ ] Push notifications via FCM
- [ ] Guardian/trusted contact features
- [ ] Family/group accounts
- [ ] Web dashboard for viewing alerts
- [ ] Data export API
- [ ] Advanced analytics

### Migration Path

To migrate existing users:

1. Add Supabase signup to registration flow
2. Prompt existing users to link account
3. Bulk sync local data to cloud on first link
4. Mark sync status per record
5. Gradually phase out local-only mode

---

## API Reference

### SupabaseConfig

```kotlin
// Initialize Supabase
SupabaseConfig.initialize(context)

// Check if configured
val isConfigured = SupabaseConfig.isConfigured(context)

// Check if enabled
val isEnabled = SupabaseConfig.isEnabled(context)

// Save credentials
SupabaseConfig.saveCredentials(context, url, anonKey)
```

### SupabaseManager

```kotlin
// Get instance
val manager = SupabaseManager.getInstance(context)

// Authentication
manager.signUp(email, password, fullName, phone, callback)
manager.signIn(email, password, callback)
manager.signOut(callback)

// Contacts
manager.syncContacts(contacts, callback)
manager.getContacts(callback)

// Alerts
manager.createSOSAlert(triggerType, lat, lng, location, count, callback)
manager.updateLocation(lat, lng, accuracy, speed, callback)
manager.stopAlert(callback)
manager.getAlertHistory(callback)

// Real-time
manager.subscribeToLocationUpdates(alertId, callback)
```

---

## Support

### Getting Help

- **Documentation**: [Supabase Docs](https://supabase.com/docs)
- **Community**: [Supabase Discord](https://discord.supabase.com)
- **Issues**: Check Supabase logs and app Logcat
- **Email**: Contact your Supabase project support

### Useful Resources

- [Supabase Android Guide](https://supabase.com/docs/guides/getting-started/tutorials/with-kotlin)
- [Row Level Security](https://supabase.com/docs/guides/auth/row-level-security)
- [Realtime Documentation](https://supabase.com/docs/guides/realtime)
- [Storage Guide](https://supabase.com/docs/guides/storage)

---

## License

This integration is part of the PulseCheck project. Supabase is © Supabase Inc.

---

**Last Updated**: 2024
**Version**: 1.0.0
**Author**: PulseCheck Development Team
