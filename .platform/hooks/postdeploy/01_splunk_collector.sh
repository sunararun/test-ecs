#!/bin/bash
# Installs Splunk OTel Collector and ships /var/log/web.stdout.log to Splunk Enterprise HEC.
# Never fail the deployment because of logging problems.
set +e

HEC_URL=$(/opt/elasticbeanstalk/bin/get-config environment -k SPLUNK_HEC_URL)
HEC_TOKEN=$(/opt/elasticbeanstalk/bin/get-config environment -k SPLUNK_HEC_TOKEN)

if [ -z "$HEC_URL" ] || [ -z "$HEC_TOKEN" ]; then
  echo "SPLUNK_HEC_URL / SPLUNK_HEC_TOKEN not set, skipping collector setup"
  exit 0
fi

# Install once
if ! systemctl list-unit-files | grep -q splunk-otel-collector; then
  curl -sSL https://dl.signalfx.com/splunk-otel-collector.sh -o /tmp/splunk-otel-collector.sh
  sh /tmp/splunk-otel-collector.sh --hec-url "$HEC_URL" --hec-token "$HEC_TOKEN" --without-instrumentation
fi

# Collector config: tail the app log and send to Splunk Enterprise
cat > /etc/otel/collector/eb_config.yaml <<'EOF'
receivers:
  filelog/app:
    include: [ /var/log/web.stdout.log ]
    start_at: end

processors:
  batch:

exporters:
  splunk_hec/enterprise:
    endpoint: ${env:SPLUNK_HEC_URL}
    token: ${env:SPLUNK_HEC_TOKEN}
    index: eb_logs
    source: first-ecs
    sourcetype: _json
    headers:
      ngrok-skip-browser-warning: "true"

service:
  pipelines:
    logs:
      receivers: [filelog/app]
      processors: [batch]
      exporters: [splunk_hec/enterprise]
EOF

CONF=/etc/otel/collector/splunk-otel-collector.conf
sed -i '/^SPLUNK_CONFIG=/d;/^SPLUNK_HEC_URL=/d;/^SPLUNK_HEC_TOKEN=/d' "$CONF"
echo "SPLUNK_CONFIG=/etc/otel/collector/eb_config.yaml" >> "$CONF"
echo "SPLUNK_HEC_URL=$HEC_URL" >> "$CONF"
echo "SPLUNK_HEC_TOKEN=$HEC_TOKEN" >> "$CONF"

# Let the collector read the app log
chmod 644 /var/log/web.stdout.log

systemctl restart splunk-otel-collector
exit 0