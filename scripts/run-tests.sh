#!/bin/bash
# run-tests.sh — compileJava → test → 커버리지 검증 자동화 스크립트
# 사용법: bash scripts/run-tests.sh

set -e

echo "======================================"
echo " SSAC Backend — 빌드 / 테스트 검증"
echo "======================================"

echo ""
echo "🔨 STEP 1. compileJava 실행 중..."
./gradlew compileJava
echo "✅ compileJava 성공"

echo ""
echo "🧪 STEP 2. test 실행 중..."
./gradlew test
echo "✅ test 성공"

echo ""
echo "📊 STEP 3. 커버리지 검증 중..."
./gradlew jacocoTestReport jacocoTestCoverageVerification
echo "✅ 커버리지 검증 성공 (서비스 레이어 70% 이상)"

echo ""
echo "======================================"
echo "🎉 모든 검증 통과! 구현이 완료되었습니다."
echo "======================================"
echo ""
echo "리포트 위치:"
echo "  테스트    : build/reports/tests/test/index.html"
echo "  커버리지  : build/reports/jacoco/test/html/index.html"
