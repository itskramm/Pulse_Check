# What You Need from Supabase

This document explains exactly what information you need to get from Supabase to complete the PulseCheck integration.

---

## 📋 Quick Checklist

You need to obtain **2 values** from Supabase:

- [ ] **Project URL** (format: `https://xxxxxxxxxxxxx.supabase.co`)
- [ ] **Anon/Public API Key** (long string starting with `eyJ...`)

---

## 🎯 Step-by-Step Instructions

### Step 1: Create/Open Your Supabase Project

1. Go to **https://supabase.com**
2. Click **"Sign In"** (or **"Start your project"** if new)
3. After signing in, you'll see your dashboard

**If you DON'T have a project yet:**
- Click **"New Project"**
- Fill in:
  - **Name**: `pulsecheck-prod` (or any name you prefer)
  - **Database Password**: Create a strong password and **SAVE IT**
  - **Region**: Choose **"Southeast Asia (Singapore)"** (closest to Philippines)
- Click **"Create new project"**
- Wait 2-3 minutes for setup to complete

**If you ALREADY have a project:**
- Click on your project from the dashboard
- Continue to Step 2

---

### Step 2: Get Your Project URL

1. In your Supabase project, look at the **left sidebar**
2. Click the **⚙️ Settings icon** (near the bottom)
3. Click **"API"** in the Settings menu
4. Look for the section labeled **"Project URL"**
5. Copy the URL (it looks like this):

```
https://abcdefghijklmnop.supabase.co
```

**Example:**
```
https://xyzproject123456.supabase.co
```

✅ **This is your `SUPABASE_URL`**

---

### Step 3: Get Your Anon/Public Key

1. Still on the **Settings → API** page
2. Scroll down to the section labeled **"Project API keys"**
3. You'll see two keys:
   - ✅ **`anon` `public`** - This is what you need
   - ❌ **`service_role` `secret`** - DO NOT USE THIS IN YOUR APP

4. Click the **copy icon** next to the **`anon` `public`** key
5. The key looks like this (very long string):

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InByb2plY3RpZCIsInJvbGUiOiJhbm9uIiwiaWF0IjoxNjQwMDAwMDAwLCJleHAiOjE5NTU1NzYwMDB9.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

✅ **This is your `SUPABASE_ANON_KEY`**

---

## ⚠️ IMPORTANT SECURITY NOTES

### ✅ SAFE to use in your Android app:
- ✅ **Project URL**
- ✅ **`anon` / `public` key**

These are safe because **Row Level Security (RLS)** protects your data.

### ❌ NEVER use in your Android app:
- ❌ **`service_role` key** (this bypasses all security!)
- ❌ **Database password** (only for admin access)

---

## 📝 What to Do with These Values

Once you have both values, update **TWO files** in your project:

### File 1: `app/src/main/kotlin/com/example/pulsecheck/SupabaseConfig.kt`

Find these lines (around line 19-23):
```kotlin
const val SUPABASE_URL = "https://your-project.supabase.co"
const val SUPABASE_ANON_KEY = "your-anon-key-here"
```

Replace with YOUR values:
```kotlin
const val SUPABASE_URL = "https://xyzproject123456.supabase.co"
const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### File 2: `app/src/main/kotlin/com/example/pulsecheck/SupabaseClient.kt`

Find these lines (around line 27-28):
```kotlin
private const val SUPABASE_URL = "https://your-project.supabase.co"
private const val SUPABASE_ANON_KEY = "your-anon-key-here"
```

Replace with YOUR values (same as above):
```kotlin
private const val SUPABASE_URL = "https://xyzproject123456.supabase.co"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## ✅ How to Verify You Got It Right

### Check 1: URL Format
Your `SUPABASE_URL` should:
- ✅ Start with `https://`
- ✅ End with `.supabase.co`
- ✅ Have 16-20 characters between `https://` and `.supabase.co`
- ❌ NOT have trailing slash `/`

**Good examples:**
```
https://abcdefghij123456.supabase.co
https://myproject99999.supabase.co
```

**Bad examples:**
```
https://your-project.supabase.co          ❌ (placeholder text)
https://abcdefghij123456.supabase.co/     ❌ (trailing slash)
abcdefghij123456.supabase.co              ❌ (missing https://)
```

### Check 2: Anon Key Format
Your `SUPABASE_ANON_KEY` should:
- ✅ Start with `eyJ`
- ✅ Be very long (200-500+ characters)
- ✅ Contain dots (.) separating three parts
- ✅ Only contain letters, numbers, dots, hyphens, underscores

**Good example:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InByb2plY3RpZCIsInJvbGUiOiJhbm9uIiwiaWF0IjoxNjQwMDAwMDAwLCJleHAiOjE5NTU1NzYwMDB9.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**Bad examples:**
```
your-anon-key-here                        ❌ (placeholder text)
service_role key                          ❌ (wrong key!)
1234567890                                ❌ (too short)
```

---

## 🧪 Test Your Credentials

After updating both files, build and run your app. Check Android Studio Logcat:

**✅ Success:**
```
✅ Supabase initialized successfully
✅ Supabase configured: https://xyzproject123456.supabase.co
```

**❌ Warning (means you need to update credentials):**
```
⚠️ WARNING: Supabase credentials not configured!
⚠️ Please update SupabaseConfig.kt with your actual credentials
```

**❌ Error:**
```
❌ Failed to initialize Supabase
Network request failed / Invalid API key
```
If you see this, double-check:
- URL format (no trailing slash)
- Key is the `anon` key (not `service_role`)
- No extra spaces or quotes

---

## 📸 Visual Guide

Here's what you're looking for in Supabase Dashboard:

```
┌─────────────────────────────────────────────────┐
│  ⚙️ Settings → API                              │
├─────────────────────────────────────────────────┤
│                                                  │
│  Project URL                                     │
│  ┌──────────────────────────────────────────┐   │
│  │ https://xyzproject123456.supabase.co     │   │
│  └──────────────────────────────────────────┘   │
│                                                  │
│  Project API keys                                │
│  ┌──────────────────────────────────────────┐   │
│  │ anon  public                             │   │
│  │ eyJhbGciOiJIUzI1NiIsInR5c...          📋│   │
│  └──────────────────────────────────────────┘   │
│                                                  │
│  ┌──────────────────────────────────────────┐   │
│  │ service_role  secret  ⚠️ DO NOT USE     │   │
│  │ eyJhbGciOiJIUzI1NiIsInR5c...          📋│   │
│  └──────────────────────────────────────────┘   │
│                                                  │
└─────────────────────────────────────────────────┘
```

---

## 🆘 Need Help?

### Common Issues:

**"I can't find the Settings icon"**
- Look at the bottom of the left sidebar
- It's a gear/cog icon ⚙️
- If you still can't find it, make sure you're logged into a project

**"I see multiple keys, which one?"**
- Use the one labeled **`anon`** or **`public`**
- The label usually says both: `anon public`
- DO NOT use `service_role` or `secret`

**"My key doesn't start with 'eyJ'"**
- You might have copied the wrong key
- Make sure you're copying the `anon` key, not the label
- Click the copy icon 📋 instead of selecting text

**"I get 'Invalid API key' error"**
- Make sure you copied the entire key (it's very long)
- Check there are no extra spaces at the beginning or end
- Make sure it's the `anon` key, not `service_role`

---

## 📞 Support

If you're still stuck:

1. **Check Supabase Docs**: https://supabase.com/docs/guides/api
2. **Supabase Discord**: https://discord.supabase.com
3. **Re-read this guide** - the answer is usually here! 😊

---

## ✅ Summary

You need **2 things** from Supabase:

1. **Project URL** from Settings → API
   - Format: `https://xxxxx.supabase.co`
   
2. **Anon/Public Key** from Settings → API
   - Starts with: `eyJ...`
   - Very long string

Then update **2 files**:
- `SupabaseConfig.kt`
- `SupabaseClient.kt`

That's it! 🎉

---

*Last updated: 2026-09-30*
