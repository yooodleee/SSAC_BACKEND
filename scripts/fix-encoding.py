#!/usr/bin/env python3
"""
fix-encoding.py
===============
이중 인코딩(double-encoding)된 .properties 파일의 한글을 복구합니다.

문제 원인:
  UTF-8 바이트를 ISO-8859-1(Latin-1)로 잘못 읽은 뒤 다시 UTF-8로 저장하면
  각 바이트가 개별 Unicode 코드포인트로 저장되어 한글이 깨집니다.

복구 원리:
  깨진 UTF-8 문자열.encode('latin-1') → 원본 UTF-8 바이트 복원
  복원된 바이트.decode('utf-8')       → 정상 한글 문자열

사용법:
  python scripts/fix-encoding.py [대상 파일 또는 디렉터리]

  # 특정 파일 복구 (미리보기)
  python scripts/fix-encoding.py src/main/resources/application.properties --dry-run

  # src/main/resources 하위 모든 .properties 파일 복구
  python scripts/fix-encoding.py src/main/resources

  # 백업 없이 즉시 덮어쓰기
  python scripts/fix-encoding.py src/main/resources --no-backup
"""

import sys
import os
import shutil
import argparse
from pathlib import Path


def is_double_encoded(text: str) -> bool:
    """이중 인코딩 여부를 휴리스틱으로 판별합니다."""
    try:
        recovered = text.encode("latin-1").decode("utf-8")
        # 복구 결과에 한글 범위(AC00-D7A3, 가~힣)가 있으면 이중 인코딩으로 판단
        return any("\uAC00" <= ch <= "\uD7A3" for ch in recovered)
    except (UnicodeEncodeError, UnicodeDecodeError):
        return False


def fix_line(line: str) -> tuple[str, bool]:
    """
    한 줄을 분석하여 이중 인코딩된 값 부분만 복구합니다.
    주석(#)과 키는 건드리지 않습니다.
    반환: (복구된 줄, 변경 여부)
    """
    stripped = line.rstrip("\n\r")

    # 주석 줄 — 값 없이 통째로 판별
    if stripped.lstrip().startswith("#"):
        if is_double_encoded(stripped):
            try:
                fixed = stripped.encode("latin-1").decode("utf-8")
                return fixed + "\n", True
            except (UnicodeEncodeError, UnicodeDecodeError):
                pass
        return line, False

    # key=value 형태
    if "=" in stripped:
        key, sep, value = stripped.partition("=")
        if is_double_encoded(value):
            try:
                fixed_value = value.encode("latin-1").decode("utf-8")
                return key + sep + fixed_value + "\n", True
            except (UnicodeEncodeError, UnicodeDecodeError):
                pass

    return line, False


def fix_file(path: Path, dry_run: bool, backup: bool) -> dict:
    """파일을 복구하고 결과를 반환합니다."""
    result = {"path": str(path), "changed_lines": 0, "status": "ok"}

    try:
        original = path.read_text(encoding="utf-8", errors="replace")
    except Exception as e:
        result["status"] = f"read_error: {e}"
        return result

    lines = original.splitlines(keepends=True)
    fixed_lines = []
    changed = 0

    for i, line in enumerate(lines, start=1):
        fixed_line, was_changed = fix_line(line)
        fixed_lines.append(fixed_line)
        if was_changed:
            changed += 1
            print(f"  [{path.name}:{i}] 복구됨")
            print(f"    전: {line.rstrip()!r}")
            print(f"    후: {fixed_line.rstrip()!r}")

    result["changed_lines"] = changed

    if changed == 0:
        result["status"] = "no_change"
        return result

    if dry_run:
        result["status"] = "dry_run"
        return result

    if backup:
        backup_path = path.with_suffix(path.suffix + ".bak")
        shutil.copy2(path, backup_path)
        print(f"  백업 생성: {backup_path}")

    path.write_text("".join(fixed_lines), encoding="utf-8")
    return result


def collect_files(target: Path) -> list[Path]:
    """대상 경로에서 .properties 파일 목록을 수집합니다."""
    if target.is_file():
        return [target]
    return sorted(target.rglob("*.properties"))


def fallback_manual_guide(path: Path):
    """자동 복구 실패 시 수동 복구 가이드를 출력합니다."""
    print(f"""
[FALLBACK 안내] {path} 자동 복구 실패
──────────────────────────────────────────────
수동 복구 방법:
  1. IntelliJ IDEA → File > File Properties > File Encoding
     - 현재 인코딩: ISO-8859-1 (또는 잘못 표시된 인코딩)
     - 변경할 인코딩: UTF-8 선택 후 [Convert] (Reload 아님!)

  2. VS Code → 파일 우클릭 → Reopen with Encoding → Latin-1(ISO 8859-1)
     저장: Ctrl+Shift+P → "Save with Encoding" → UTF-8

  3. iconv 커맨드라인:
     iconv -f UTF-8 -t LATIN1 broken.properties | iconv -f UTF-8 -t UTF-8

  4. 원본 텍스트를 알고 있다면 직접 properties 파일을 UTF-8로 다시 작성.
""")


def main():
    parser = argparse.ArgumentParser(description="이중 인코딩된 .properties 파일 한글 복구")
    parser.add_argument("target", nargs="?", default="src/main/resources",
                        help="복구할 파일 또는 디렉터리 (기본값: src/main/resources)")
    parser.add_argument("--dry-run", action="store_true",
                        help="실제 파일을 수정하지 않고 변경 내용만 출력")
    parser.add_argument("--no-backup", action="store_true",
                        help="백업 파일(.bak)을 생성하지 않음")
    args = parser.parse_args()

    target = Path(args.target)
    if not target.exists():
        print(f"오류: 경로를 찾을 수 없습니다 — {target}", file=sys.stderr)
        sys.exit(1)

    files = collect_files(target)
    if not files:
        print("대상 .properties 파일이 없습니다.")
        return

    print(f"대상 파일 {len(files)}개 {'(dry-run)' if args.dry_run else ''}")
    print("=" * 60)

    total_changed = 0
    for f in files:
        print(f"\n검사 중: {f}")
        result = fix_file(f, dry_run=args.dry_run, backup=not args.no_backup)
        if result["status"] in ("read_error",):
            fallback_manual_guide(f)
        total_changed += result["changed_lines"]

    print("\n" + "=" * 60)
    if args.dry_run:
        print(f"[dry-run] 총 {total_changed}개 줄을 복구할 수 있습니다. --dry-run 없이 실행하면 적용됩니다.")
    else:
        print(f"완료: 총 {total_changed}개 줄 복구됨.")


if __name__ == "__main__":
    main()
