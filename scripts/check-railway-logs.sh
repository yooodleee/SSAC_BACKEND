#!/usr/bin/env bash
# check-railway-logs.sh — Railway 배포 로그 조회 스크립트
#
# 사용법:
#   bash scripts/check-railway-logs.sh          # 전체 배포 로그
#   bash scripts/check-railway-logs.sh error    # 오류 로그만 필터링
#   bash scripts/check-railway-logs.sh env      # 환경 변수 관련 오류만 필터링

set -euo pipefail

MODE="${1:-full}"

# railway CLI 설치 확인
if ! command -v railway &>/dev/null; then
    echo "[ERROR] railway CLI가 설치되어 있지 않습니다."
    echo "  설치: npm install -g @railway/cli"
    exit 1
fi

# 로그인 상태 확인
if ! railway whoami &>/dev/null; then
    echo "[ERROR] Railway에 로그인되어 있지 않습니다."
    echo "  로그인: railway login"
    exit 1
fi

case "$MODE" in
    full)
        echo "=== Railway 배포 로그 (전체) ==="
        railway logs --deployment
        ;;
    error)
        echo "=== Railway 배포 로그 (오류 필터) ==="
        railway logs --deployment 2>&1 | grep -i "error\|failed\|exception\|caused by" | head -20
        ;;
    env)
        echo "=== Railway 배포 로그 (환경 변수 관련 오류) ==="
        railway logs --deployment 2>&1 | grep -i "placeholder\|environment\|variable" | head -10
        ;;
    tail)
        echo "=== Railway 실시간 로그 ==="
        railway logs --tail
        ;;
    *)
        echo "사용법: $0 [full|error|env|tail]"
        echo "  full  — 전체 배포 로그 (기본값)"
        echo "  error — 오류 로그만 필터링"
        echo "  env   — 환경 변수 관련 오류만 필터링"
        echo "  tail  — 실시간 로그 스트리밍"
        exit 1
        ;;
esac
