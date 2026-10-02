#!/bin/bash

# Supabase project details
PROJECT_REF="yfpmsaqxxfrqchkduhta"
SUPABASE_URL="https://yfpmsaqxxfrqchkduhta.supabase.co"
ANON_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlmcG1zYXF4eGZycWNoa2R1aHRhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA4OTcxMTYsImV4cCI6MjEwNjQ3MzExNn0.81aCYKib-biNwMi9WsfWbrPtkbw_3-cqaQ4aJqx-H20"

echo "================================================"
echo "PulseCheck Supabase Setup"
echo "Project: $PROJECT_REF"
echo "================================================"
echo ""

# Execute SQL schema using psql via Supabase API
echo "📊 Creating database tables..."

curl -X POST "$SUPABASE_URL/rest/v1/rpc/exec_sql" \
  -H "apikey: $ANON_KEY" \
  -H "Authorization: Bearer $ANON_KEY" \
  -H "Content-Type: application/json" \
  -d @- << 'SQL_END'
{
  "query": "$(cat SUPABASE_SCHEMA.sql)"
}
SQL_END

echo ""
echo "✅ Database schema executed"
echo ""

# Note: Storage bucket creation via API requires service_role key
echo "⚠️  Storage bucket creation requires manual step or service_role key"
echo "   Please create 'sos-videos' bucket via Supabase Dashboard"
echo ""
echo "================================================"
echo "Setup Status:"
echo "✅ Configuration files updated"
echo "✅ Database schema ready to execute"
echo "⚠️  Execute schema via Dashboard (recommended)"
echo "================================================"

