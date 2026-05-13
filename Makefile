.PHONY: logs-railway logs-railway-error logs-railway-env logs-railway-tail

## Railway 배포 로그 전체 조회
logs-railway:
	bash scripts/check-railway-logs.sh full

## Railway 오류 로그만 필터링
logs-railway-error:
	bash scripts/check-railway-logs.sh error

## Railway 환경 변수 관련 오류만 필터링
logs-railway-env:
	bash scripts/check-railway-logs.sh env

## Railway 실시간 로그 스트리밍
logs-railway-tail:
	bash scripts/check-railway-logs.sh tail
