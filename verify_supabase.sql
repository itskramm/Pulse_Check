-- ============================================================================
-- PulseCheck Supabase Verification Script
-- ============================================================================
-- Run this script in Supabase SQL Editor to verify your setup
-- Project ID: yfpmsaqxxfrqchkduhta
-- ============================================================================

-- ============================================================================
-- 1. Check all tables exist
-- ============================================================================
SELECT 
    'Tables' as check_type,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) = 8 THEN '✅ PASS'
        ELSE '❌ FAIL - Expected 8 tables'
    END as status
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
  AND table_name IN (
      'user_profiles',
      'emergency_contacts',
      'sos_alerts',
      'location_updates',
      'alert_notifications',
      'video_recordings',
      'video_sharing_log',
      'schema_version'
  );

-- ============================================================================
-- 2. List all tables
-- ============================================================================
SELECT 
    table_name,
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_name = t.table_name) as column_count
FROM information_schema.tables t
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- ============================================================================
-- 3. Check RLS is enabled
-- ============================================================================
SELECT 
    tablename,
    CASE 
        WHEN rowsecurity THEN '✅ Enabled'
        ELSE '❌ Disabled'
    END as rls_status
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN (
      'user_profiles',
      'emergency_contacts',
      'sos_alerts',
      'location_updates',
      'alert_notifications',
      'video_recordings',
      'video_sharing_log'
  )
ORDER BY tablename;

-- ============================================================================
-- 4. Count RLS policies
-- ============================================================================
SELECT 
    'RLS Policies' as check_type,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) >= 17 THEN '✅ PASS'
        ELSE '⚠️ WARNING - Expected 17+ policies'
    END as status
FROM pg_policies
WHERE schemaname = 'public';

-- ============================================================================
-- 5. List all RLS policies by table
-- ============================================================================
SELECT 
    tablename,
    COUNT(*) as policy_count,
    STRING_AGG(policyname, ', ') as policies
FROM pg_policies
WHERE schemaname = 'public'
GROUP BY tablename
ORDER BY tablename;

-- ============================================================================
-- 6. Check views exist
-- ============================================================================
SELECT 
    table_name as view_name,
    CASE 
        WHEN security_type = 'INVOKER' THEN '✅ security_invoker'
        ELSE '⚠️ NOT security_invoker'
    END as security_status
FROM information_schema.views
WHERE table_schema = 'public'
  AND table_name IN (
      'user_alert_stats',
      'video_statistics',
      'user_comprehensive_data'
  )
ORDER BY table_name;

-- ============================================================================
-- 7. Check functions exist
-- ============================================================================
SELECT 
    routine_name as function_name,
    routine_type,
    '✅ Exists' as status
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name IN (
      'update_updated_at_column'
  );

-- ============================================================================
-- 8. Check triggers exist
-- ============================================================================
SELECT 
    trigger_name,
    event_object_table as table_name,
    '✅ Exists' as status
FROM information_schema.triggers
WHERE trigger_schema = 'public'
ORDER BY event_object_table, trigger_name;

-- ============================================================================
-- 9. Verify schema version
-- ============================================================================
SELECT 
    version,
    applied_at,
    notes,
    '✅ PASS' as status
FROM schema_version
ORDER BY applied_at DESC
LIMIT 1;

-- ============================================================================
-- 10. Check video_recordings columns (most important table)
-- ============================================================================
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'video_recordings'
ORDER BY ordinal_position;

-- ============================================================================
-- 11. Check for broken triggers (should return 0 rows)
-- ============================================================================
SELECT 
    trigger_name,
    event_object_table,
    '❌ SHOULD BE REMOVED' as status
FROM information_schema.triggers
WHERE trigger_schema = 'public'
  AND trigger_name = 'video_uploaded_notification';

-- ============================================================================
-- 12. Test sample data insertion (will fail if RLS not working correctly)
-- ============================================================================
-- Uncomment to test (requires authenticated user)
/*
BEGIN;

-- Insert test profile
INSERT INTO user_profiles (user_id, full_name, phone, email)
VALUES (auth.uid(), 'Test User', '+639171234567', 'test@example.com')
ON CONFLICT (user_id) DO NOTHING;

-- Insert test contact
INSERT INTO emergency_contacts (user_id, name, phone, email, affiliation)
VALUES (auth.uid(), 'Test Contact', '+639171234568', 'contact@example.com', 'Friend');

-- Insert test alert
INSERT INTO sos_alerts (user_id, trigger_type, latitude, longitude, status)
VALUES (auth.uid(), 'dead_mans_switch', 14.5995, 120.9842, 'active');

-- Verify inserts
SELECT 'user_profiles' as table_name, COUNT(*) as count FROM user_profiles WHERE user_id = auth.uid()
UNION ALL
SELECT 'emergency_contacts', COUNT(*) FROM emergency_contacts WHERE user_id = auth.uid()
UNION ALL
SELECT 'sos_alerts', COUNT(*) FROM sos_alerts WHERE user_id = auth.uid();

ROLLBACK; -- Remove test data
*/

-- ============================================================================
-- 13. Summary Report
-- ============================================================================
SELECT 
    '════════════════════════════════════════════════' as separator
UNION ALL
SELECT '  VERIFICATION SUMMARY'
UNION ALL
SELECT '════════════════════════════════════════════════'
UNION ALL
SELECT '  Total Tables: ' || (
    SELECT COUNT(*) FROM information_schema.tables 
    WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
)::text
UNION ALL
SELECT '  Total Views: ' || (
    SELECT COUNT(*) FROM information_schema.views 
    WHERE table_schema = 'public'
)::text
UNION ALL
SELECT '  Total RLS Policies: ' || (
    SELECT COUNT(*) FROM pg_policies 
    WHERE schemaname = 'public'
)::text
UNION ALL
SELECT '  Total Triggers: ' || (
    SELECT COUNT(*) FROM information_schema.triggers 
    WHERE trigger_schema = 'public'
)::text
UNION ALL
SELECT '════════════════════════════════════════════════'
UNION ALL
SELECT '  ✅ Run complete - Check results above'
UNION ALL
SELECT '════════════════════════════════════════════════';

-- ============================================================================
-- End of verification script
-- ============================================================================
-- Expected results:
-- - 8 tables
-- - 3 views (all with security_invoker)
-- - 17+ RLS policies
-- - 3 triggers
-- - 1 function
-- - 0 broken triggers
-- - Schema version 2.0
-- ============================================================================
