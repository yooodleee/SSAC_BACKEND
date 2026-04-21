#!/usr/bin/env python3
"""
notify.py
=========
Slack 웹훅 알림 유틸리티.

Webhook URL은 환경 변수 SLACK_WEBHOOK_URL 또는 .env 파일로 관리한다.
절대로 URL을 소스 코드에 하드코딩하지 않는다.

사용법:
  # .env 파일에 SLACK_WEBHOOK_URL=https://hooks.slack.com/... 을 설정한 뒤
  python scripts/notify.py "메시지 내용"

  # 또는 환경 변수로 직접 전달
  SLACK_WEBHOOK_URL=https://hooks.slack.com/... python scripts/notify.py "메시지"
"""

import os
import sys
import json
import urllib.request
from pathlib import Path


def load_env_file() -> None:
    """프로젝트 루트의 .env 파일을 읽어 환경 변수로 등록한다."""
    env_path = Path(__file__).parent.parent / ".env"
    if not env_path.exists():
        return
    with env_path.open(encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            os.environ.setdefault(key.strip(), value.strip())


def send_slack(text: str) -> None:
    """Slack 웹훅으로 메시지를 전송한다."""
    load_env_file()

    webhook_url = os.environ.get("SLACK_WEBHOOK_URL")
    if not webhook_url:
        print(
            "[notify] SLACK_WEBHOOK_URL 환경 변수가 설정되지 않았습니다. "
            ".env 파일 또는 환경 변수를 확인하세요.",
            file=sys.stderr,
        )
        sys.exit(1)

    payload = json.dumps({"text": text}).encode("utf-8")
    req = urllib.request.Request(
        webhook_url,
        data=payload,
        headers={"Content-Type": "application/json; charset=utf-8"},
    )
    urllib.request.urlopen(req)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(f"사용법: python {sys.argv[0]} <메시지>", file=sys.stderr)
        sys.exit(1)
    send_slack(sys.argv[1])
    print("[notify] Slack 전송 완료")
