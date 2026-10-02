-- ============================================================================
-- PulseCheck Supabase Database Schema
-- ============================================================================
-- Instructions:
-- 1. Go to your Supabase project dashboard
-- 2. Navigate to SQL Editor
-- 3. Run this entire script to create the database schema
-- 4. Enable Row Level Security (RLS) for all tables
-- ============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- User Profiles Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL UNIQUE,
    full_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT,
    gender TEXT,
    permanent_address TEXT,
    work_address TEXT,
    other_addresses TEXT,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for faster lookups
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_phone ON user_profiles(phone);

-- Row Level Security
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;

-- Users can only read/update their own profile
CREATE POLICY "Users can view own profile" ON user_profiles
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own profile" ON user_profiles
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own profile" ON user_profiles
    FOR UPDATE USING (auth.uid() = user_id);

-- ============================================================================
-- Emergency Contacts Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS emergency_contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT,
    affiliation TEXT,
    gender TEXT,
    home_address TEXT,
    work_address TEXT,
    other_address TEXT,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_emergency_contacts_user_id ON emergency_contacts(user_id);

-- Row Level Security
ALTER TABLE emergency_contacts ENABLE ROW LEVEL SECURITY;

-- Users can only manage their own contacts
CREATE POLICY "Users can view own contacts" ON emergency_contacts
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own contacts" ON emergency_contacts
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own contacts" ON emergency_contacts
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own contacts" ON emergency_contacts
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================================
-- SOS Alerts Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS sos_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    trigger_type TEXT NOT NULL, -- 'dead_mans_switch', 'volume_button', 'manual'
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    location TEXT,
    contacts_notified INTEGER DEFAULT 0,
    status TEXT DEFAULT 'active', -- 'active', 'resolved', 'cancelled'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT valid_status CHECK (status IN ('active', 'resolved', 'cancelled'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_sos_alerts_user_id ON sos_alerts(user_id);
CREATE INDEX IF NOT EXISTS idx_sos_alerts_status ON sos_alerts(status);
CREATE INDEX IF NOT EXISTS idx_sos_alerts_created_at ON sos_alerts(created_at DESC);

-- Row Level Security
ALTER TABLE sos_alerts ENABLE ROW LEVEL SECURITY;

-- Users can manage their own alerts
CREATE POLICY "Users can view own alerts" ON sos_alerts
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own alerts" ON sos_alerts
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own alerts" ON sos_alerts
    FOR UPDATE USING (auth.uid() = user_id);

-- Emergency contacts can view active alerts (for future guardian feature)
-- CREATE POLICY "Contacts can view user alerts" ON sos_alerts
--     FOR SELECT USING (
--         user_id IN (
--             SELECT user_id FROM emergency_contacts 
--             WHERE phone = (SELECT phone FROM user_profiles WHERE user_id = auth.uid())
--         )
--     );

-- ============================================================================
-- Location Updates Table (for tracking during active alerts)
-- ============================================================================
CREATE TABLE IF NOT EXISTS location_updates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_id UUID REFERENCES sos_alerts(id) ON DELETE CASCADE NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy REAL,
    speed REAL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_location_updates_alert_id ON location_updates(alert_id);
CREATE INDEX IF NOT EXISTS idx_location_updates_created_at ON location_updates(created_at DESC);

-- Row Level Security
ALTER TABLE location_updates ENABLE ROW LEVEL SECURITY;

-- Users can view location updates for their own alerts
CREATE POLICY "Users can view own location updates" ON location_updates
    FOR SELECT USING (
        alert_id IN (SELECT id FROM sos_alerts WHERE user_id = auth.uid())
    );

CREATE POLICY "Users can insert own location updates" ON location_updates
    FOR INSERT WITH CHECK (
        alert_id IN (SELECT id FROM sos_alerts WHERE user_id = auth.uid())
    );

-- ============================================================================
-- Alert Notifications Table (track which contacts were notified)
-- ============================================================================
CREATE TABLE IF NOT EXISTS alert_notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_id UUID REFERENCES sos_alerts(id) ON DELETE CASCADE NOT NULL,
    contact_id UUID REFERENCES emergency_contacts(id) ON DELETE CASCADE,
    contact_name TEXT NOT NULL,
    contact_phone TEXT,
    contact_email TEXT,
    notification_type TEXT NOT NULL, -- 'sms', 'email', 'app'
    status TEXT DEFAULT 'pending', -- 'pending', 'sent', 'failed'
    sent_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_alert_notifications_alert_id ON alert_notifications(alert_id);

-- Row Level Security
ALTER TABLE alert_notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own alert notifications" ON alert_notifications
    FOR SELECT USING (
        alert_id IN (SELECT id FROM sos_alerts WHERE user_id = auth.uid())
    );

CREATE POLICY "Users can insert own alert notifications" ON alert_notifications
    FOR INSERT WITH CHECK (
        alert_id IN (SELECT id FROM sos_alerts WHERE user_id = auth.uid())
    );

-- ============================================================================
-- Functions and Triggers
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for user_profiles
DROP TRIGGER IF EXISTS update_user_profiles_updated_at ON user_profiles;
CREATE TRIGGER update_user_profiles_updated_at
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for emergency_contacts
DROP TRIGGER IF EXISTS update_emergency_contacts_updated_at ON emergency_contacts;
CREATE TRIGGER update_emergency_contacts_updated_at
    BEFORE UPDATE ON emergency_contacts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Views for Analytics (Optional)
-- ============================================================================

-- View for user alert statistics
CREATE OR REPLACE VIEW user_alert_stats AS
SELECT 
    user_id,
    COUNT(*) as total_alerts,
    COUNT(*) FILTER (WHERE status = 'active') as active_alerts,
    COUNT(*) FILTER (WHERE status = 'resolved') as resolved_alerts,
    COUNT(*) FILTER (WHERE status = 'cancelled') as cancelled_alerts,
    MAX(created_at) as last_alert_time
FROM sos_alerts
GROUP BY user_id;

-- ============================================================================
-- Sample Data (Optional - for testing)
-- ============================================================================

-- Uncomment to insert sample data after creating a test user
/*
-- Insert sample user profile (replace USER_ID with actual auth.users.id)
INSERT INTO user_profiles (user_id, full_name, phone, email)
VALUES ('YOUR_USER_ID', 'Test User', '+1234567890', 'test@example.com');

-- Insert sample emergency contacts
INSERT INTO emergency_contacts (user_id, name, phone, email, affiliation)
VALUES 
    ('YOUR_USER_ID', 'John Doe', '+1234567891', 'john@example.com', 'Friend'),
    ('YOUR_USER_ID', 'Jane Smith', '+1234567892', 'jane@example.com', 'Family');
*/

-- ============================================================================
-- Realtime Configuration
-- ============================================================================

-- Enable realtime for location_updates table
-- Run this in Supabase Dashboard > Database > Replication
-- Or use the following command:
-- ALTER PUBLICATION supabase_realtime ADD TABLE location_updates;

-- ============================================================================
-- Storage Buckets (Optional - for profile pictures)
-- ============================================================================

-- Create storage bucket for avatars
-- This must be done via Supabase Dashboard or Storage API
-- Navigate to Storage > Create a new bucket called 'avatars'
-- Set it to public if you want avatars to be publicly accessible

-- ============================================================================
-- Indexes for Performance
-- ============================================================================

-- Additional composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_sos_alerts_user_status 
    ON sos_alerts(user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_emergency_contacts_user_name 
    ON emergency_contacts(user_id, name);

-- ============================================================================
-- Comments for Documentation
-- ============================================================================

COMMENT ON TABLE user_profiles IS 'Stores extended user profile information beyond auth.users';
COMMENT ON TABLE emergency_contacts IS 'Stores emergency contacts for each user';
COMMENT ON TABLE sos_alerts IS 'Records all SOS alert events';
COMMENT ON TABLE location_updates IS 'Real-time location tracking during active alerts';
COMMENT ON TABLE alert_notifications IS 'Tracks notification delivery to emergency contacts';

-- ============================================================================
-- Grant Permissions
-- ============================================================================

-- Grant usage on schema
GRANT USAGE ON SCHEMA public TO anon, authenticated;

-- Grant permissions on tables
GRANT ALL ON ALL TABLES IN SCHEMA public TO authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO authenticated;

-- ============================================================================
-- End of Schema
-- ============================================================================

SELECT 'PulseCheck Supabase schema created successfully!' as status;


-- ============================================================================
-- Video Recordings Table (NEW - v2.0)
-- Stores metadata about recorded SOS videos
-- ============================================================================
CREATE TABLE IF NOT EXISTS video_recordings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    alert_id UUID REFERENCES sos_alerts(id) ON DELETE SET NULL,
    file_name TEXT NOT NULL,
    file_path TEXT,
    storage_url TEXT,
    duration_ms INTEGER DEFAULT 0,
    file_size_bytes BIGINT DEFAULT 0,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    shared_count INTEGER DEFAULT 0,
    notes TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_video_recordings_user_id ON video_recordings(user_id);
CREATE INDEX IF NOT EXISTS idx_video_recordings_alert_id ON video_recordings(alert_id);
CREATE INDEX IF NOT EXISTS idx_video_recordings_recorded_at ON video_recordings(recorded_at DESC);

-- Row Level Security
ALTER TABLE video_recordings ENABLE ROW LEVEL SECURITY;

-- Users can only access their own videos
CREATE POLICY "Users can view own videos" ON video_recordings
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own videos" ON video_recordings
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own videos" ON video_recordings
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own videos" ON video_recordings
    FOR DELETE USING (auth.uid() = user_id);

-- ============================================================================
-- Video Sharing Log Table (NEW - v2.0)
-- Tracks who received each video
-- ============================================================================
CREATE TABLE IF NOT EXISTS video_sharing_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    video_id UUID REFERENCES video_recordings(id) ON DELETE CASCADE NOT NULL,
    contact_id UUID REFERENCES emergency_contacts(id) ON DELETE SET NULL,
    contact_name TEXT NOT NULL,
    contact_email TEXT,
    contact_phone TEXT,
    sharing_method TEXT NOT NULL, -- 'email', 'mms', 'cloud_link'
    shared_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    delivery_status TEXT DEFAULT 'pending', -- 'pending', 'sent', 'failed'
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_video_sharing_log_video_id ON video_sharing_log(video_id);
CREATE INDEX IF NOT EXISTS idx_video_sharing_log_contact_id ON video_sharing_log(contact_id);

-- Row Level Security
ALTER TABLE video_sharing_log ENABLE ROW LEVEL SECURITY;

-- Users can view sharing logs for their own videos
CREATE POLICY "Users can view own video sharing logs" ON video_sharing_log
    FOR SELECT USING (
        video_id IN (
            SELECT id FROM video_recordings WHERE user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert own video sharing logs" ON video_sharing_log
    FOR INSERT WITH CHECK (
        video_id IN (
            SELECT id FROM video_recordings WHERE user_id = auth.uid()
        )
    );

-- ============================================================================
-- Supabase Storage Bucket Configuration
-- ============================================================================
-- Run these commands in the Supabase Storage section:

-- 1. Create 'sos-videos' bucket:
--    Name: sos-videos
--    Public: false
--    File size limit: 100 MB
--    Allowed MIME types: video/mp4, video/mpeg, video/quicktime

-- 2. Set bucket policies:
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'sos-videos',
    'sos-videos',
    false,
    104857600, -- 100 MB
    ARRAY['video/mp4', 'video/mpeg', 'video/quicktime', 'video/x-matroska']
) ON CONFLICT (id) DO NOTHING;

-- 3. Storage policies for sos-videos bucket:
CREATE POLICY "Users can upload own videos" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'sos-videos' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can view own videos" ON storage.objects
    FOR SELECT USING (
        bucket_id = 'sos-videos' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

CREATE POLICY "Users can delete own videos" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'sos-videos' AND
        auth.uid()::text = (storage.foldername(name))[1]
    );

-- ============================================================================
-- Functions and Triggers (Updated for v2.0)
-- ============================================================================

-- Update updated_at timestamp for video_recordings
CREATE OR REPLACE FUNCTION update_video_recordings_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER video_recordings_updated_at
    BEFORE UPDATE ON video_recordings
    FOR EACH ROW
    EXECUTE FUNCTION update_video_recordings_updated_at();

-- ============================================================================
-- Video Statistics View
-- ============================================================================
CREATE OR REPLACE VIEW video_statistics AS
SELECT 
    user_id,
    COUNT(*) as total_videos,
    SUM(file_size_bytes) as total_storage_bytes,
    SUM(duration_ms) as total_duration_ms,
    AVG(duration_ms) as avg_duration_ms,
    SUM(shared_count) as total_shares,
    COUNT(CASE WHEN storage_url IS NOT NULL THEN 1 END) as uploaded_count,
    MAX(recorded_at) as last_recorded_at
FROM video_recordings
GROUP BY user_id;

-- Grant access to view
GRANT SELECT ON video_statistics TO authenticated;

-- ============================================================================
-- Comprehensive User Data View (All user information in one place)
-- ============================================================================
CREATE OR REPLACE VIEW user_comprehensive_data AS
SELECT 
    up.user_id,
    up.full_name,
    up.phone,
    up.email,
    up.gender,
    up.permanent_address,
    up.work_address,
    up.other_addresses,
    up.avatar_url,
    up.created_at as profile_created_at,
    -- Contact counts
    (SELECT COUNT(*) FROM emergency_contacts WHERE user_id = up.user_id) as total_contacts,
    -- Alert counts
    (SELECT COUNT(*) FROM sos_alerts WHERE user_id = up.user_id) as total_alerts,
    (SELECT COUNT(*) FROM sos_alerts WHERE user_id = up.user_id AND resolved_at IS NULL) as active_alerts,
    -- Video counts
    (SELECT COUNT(*) FROM video_recordings WHERE user_id = up.user_id) as total_videos,
    (SELECT SUM(file_size_bytes) FROM video_recordings WHERE user_id = up.user_id) as total_video_storage,
    -- Recent activity
    (SELECT MAX(triggered_at) FROM sos_alerts WHERE user_id = up.user_id) as last_alert_at,
    (SELECT MAX(recorded_at) FROM video_recordings WHERE user_id = up.user_id) as last_video_at
FROM user_profiles up;

-- Grant access to view
GRANT SELECT ON user_comprehensive_data TO authenticated;

-- ============================================================================
-- Video Upload Webhook Function (Optional - for real-time notifications)
-- ============================================================================
CREATE OR REPLACE FUNCTION notify_video_uploaded()
RETURNS TRIGGER AS $$
BEGIN
    -- Insert notification record
    INSERT INTO alert_notifications (
        alert_id,
        notification_type,
        message,
        sent_at
    ) VALUES (
        NEW.alert_id,
        'video_uploaded',
        'Video evidence uploaded: ' || NEW.file_name,
        NOW()
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER video_uploaded_notification
    AFTER INSERT ON video_recordings
    FOR EACH ROW
    WHEN (NEW.storage_url IS NOT NULL)
    EXECUTE FUNCTION notify_video_uploaded();

-- ============================================================================
-- Schema Version
-- ============================================================================
CREATE TABLE IF NOT EXISTS schema_version (
    version TEXT PRIMARY KEY,
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    notes TEXT
);

INSERT INTO schema_version (version, notes) 
VALUES ('2.0', 'Added video recording tables and storage buckets')
ON CONFLICT (version) DO NOTHING;

-- ============================================================================
-- End of Schema
-- ============================================================================
-- Total Tables: 7
-- - user_profiles
-- - emergency_contacts
-- - sos_alerts
-- - location_updates
-- - alert_notifications
-- - video_recordings (NEW)
-- - video_sharing_log (NEW)
--
-- Total Views: 2
-- - video_statistics (NEW)
-- - user_comprehensive_data (NEW)
--
-- Storage Buckets: 1
-- - sos-videos (NEW)
-- ============================================================================
