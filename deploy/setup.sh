#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v java >/dev/null || ! java -version 2>&1 | grep -q '"21'; then
    dnf install -y java-21-amazon-corretto-headless
fi

id dorm3 >/dev/null 2>&1 || useradd --system --home-dir /opt/dorm3 --shell /sbin/nologin dorm3
install -d -o dorm3 -g dorm3 -m 750 /opt/dorm3 /var/lib/dorm3
install -o dorm3 -g dorm3 -m 640 dorm3.jar /opt/dorm3/dorm3.jar
install -m 644 dorm3.service /etc/systemd/system/dorm3.service

systemctl daemon-reload
systemctl enable dorm3
systemctl restart dorm3
sleep 15
systemctl --no-pager status dorm3 | head -n 5
curl -s -o /dev/null -w "local check: HTTP %{http_code}\n" http://localhost/
