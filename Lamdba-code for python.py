import json
import gzip
import base64
import re
import os
import urllib.request
import urllib.error

SPLUNK_URL = os.environ['SPLUNK_URL']
SPLUNK_TOKEN = os.environ['SPLUNK_TOKEN']
SPLUNK_INDEX = os.environ.get('SPLUNK_INDEX', 'elasticbeanstalk-logs')

# strips "Oct 10 03:17:18 ip-10-0-13-64 web[2733]: " before the JSON
PREFIX = re.compile(r'^\w{3}\s+\d+\s+[\d:]+\s+\S+\s+\S+?:\s+(?=\{)')


def parse(msg):
    cleaned = PREFIX.sub('', msg.strip())
    if cleaned.startswith('{'):
        try:
            return json.loads(cleaned), True
        except ValueError:
            pass
    return msg, False


def lambda_handler(event, context):
    cw_data = base64.b64decode(event['awslogs']['data'])
    cw_logs = json.loads(gzip.decompress(cw_data).decode('utf-8'))

    if cw_logs.get('messageType') == 'CONTROL_MESSAGE':
        return 'control message ignored'

    payload = ""
    for e in cw_logs['logEvents']:
        body, is_json = parse(e['message'])
        item = {
            "time": e['timestamp'] / 1000.0,
            "host": cw_logs['logStream'],
            "source": cw_logs['logGroup'],
            "index": SPLUNK_INDEX,
            "event": body,
        }
        if is_json:
            item["sourcetype"] = "_json"
        payload += json.dumps(item) + "\n"

    req = urllib.request.Request(SPLUNK_URL, data=payload.encode('utf-8'))
    req.add_header('Authorization', 'Splunk ' + SPLUNK_TOKEN)
    req.add_header('Content-Type', 'application/json')
    req.add_header('ngrok-skip-browser-warning', 'true')

    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.read().decode('utf-8')
    except urllib.error.HTTPError as ex:
        print("Splunk returned HTTP", ex.code, ex.read().decode('utf-8'))
        raise
    except Exception as ex:
        print("Error sending logs to Splunk:", str(ex))
        raise