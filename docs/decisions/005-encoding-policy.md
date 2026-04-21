# ADR 005 — 프로젝트 인코딩 정책: UTF-8 표준화

## 상태

채택 (2026-04-21)

---

## 배경

Windows 환경에서 Spring Boot `.properties` 파일의 한글이 이중 인코딩
(UTF-8 바이트를 ISO-8859-1로 읽은 후 다시 UTF-8로 저장)되어 깨지는 문제가 발생했다.
원인은 IDE·Git·JVM·Gradle 각 레이어가 서로 다른 기본 인코딩을 사용했기 때문이다.

---

## 결정

**프로젝트의 모든 텍스트 파일은 UTF-8, 줄바꿈은 LF** 를 표준으로 한다.

---

## 적용 레이어

| 레이어 | 설정 위치 | 핵심 내용 |
|---|---|---|
| Spring HTTP | `application.properties` | `server.servlet.encoding.*=UTF-8` |
| Spring i18n | `EncodingConfig.java` | `MessageSource` defaultEncoding UTF-8 |
| Gradle 컴파일 | `build.gradle` | `options.encoding = 'UTF-8'` |
| Gradle 데몬 | `gradle.properties` | `-Dfile.encoding=UTF-8` |
| bootRun / test JVM | `build.gradle` | `jvmArgs '-Dfile.encoding=UTF-8'` |
| Git | `.gitattributes` | `* text=auto eol=lf` |
| IDE | `.editorconfig` | `charset = utf-8`, `end_of_line = lf` |

---

## application.properties vs application.yml

### 현재 프로젝트 선택: `.properties` 유지

| 항목 | `.properties` | `.yml` |
|---|---|---|
| 인코딩 취약성 | Spring Boot 2.x까지 ISO-8859-1 기본값 (3.x부터 UTF-8) | YAML 파서는 기본 UTF-8 |
| 가독성 | 중첩 구조 반복이 많아 장황 | 들여쓰기로 계층 표현이 깔끔 |
| 프로파일 분리 | 파일을 나눠야 함(`-local`, `-test`) | 같은 파일에 `---` 구분자 사용 가능 |
| 복잡한 리스트 | 불편 | 자연스러움 |
| 오타 위험 | 낮음 | 들여쓰기 오류에 취약 |
| IDE 지원 | 완전 | 완전 |

**결론:** Spring Boot 3.x(현재 4.x)에서는 `.properties`도 UTF-8 기본값이므로
기존 파일을 유지한다. 신규 기능 추가 시 복잡한 구조가 필요하면 `.yml` 도입을 재검토한다.

---

## 인코딩 깨짐 복구 절차

```bash
# 1. 미리보기 (파일 변경 없음)
python scripts/fix-encoding.py src/main/resources --dry-run

# 2. 복구 실행 (자동 .bak 백업 생성)
python scripts/fix-encoding.py src/main/resources

# 3. 결과 확인 후 백업 삭제
find src/main/resources -name "*.bak" -delete
```

### Fallback (자동 복구 불가 시)

1. IntelliJ: `File > File Properties > File Encoding` → ISO-8859-1로 **Reload** →
   내용 확인 후 UTF-8로 **Convert**
2. VS Code: 우하단 인코딩 클릭 → `Reopen with Encoding: Latin-1` →
   `Save with Encoding: UTF-8`
3. 원본 텍스트를 알고 있다면 직접 재작성

---

## 협업 체크리스트

- [ ] IntelliJ: `Settings > Editor > File Encodings` → Global/Project UTF-8, `Transparent native-to-ascii conversion` **OFF**
- [ ] VS Code: `"files.encoding": "utf8"`, `"files.eol": "\n"` in `settings.json`
- [ ] Git 전역 설정: `git config --global core.autocrlf false`
- [ ] Windows 터미널: `chcp 65001` (PowerShell UTF-8 코드페이지)
- [ ] IntelliJ Run Configuration: VM options에 `-Dfile.encoding=UTF-8` 추가

---

## 재발 방지 체크리스트

- [ ] `gradle.properties`에 `-Dfile.encoding=UTF-8` 포함 확인
- [ ] `.editorconfig`가 프로젝트 루트에 존재하고 `charset = utf-8` 확인
- [ ] `.gitattributes`에 `*.properties text eol=lf` 확인
- [ ] CI 파이프라인(GitHub Actions) 환경변수에 `JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF-8` 추가 검토
- [ ] PR 리뷰 시 `.properties`/`.yml` 파일의 한글이 정상 표시되는지 확인
