"""
shared/utils.py — 두 파이프라인 공통 유틸리티

파일 I/O, 로깅 설정, 시간 유틸 등 양쪽에서 공유하는 함수 모음.
정규화/통계 등 도메인 특화 함수는 각 파이프라인의 utils.py에 위치.
"""

import json
import logging
import os
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path

from collectors.scripts.shared.config import LOG_DATE_FORMAT, LOG_FORMAT

logger = logging.getLogger(__name__)


# ── 로깅 ──────────────────────────────────────────────────────────────────────
def setup_logging(level: int = logging.INFO) -> None:
    logging.basicConfig(
        level=level,
        format=LOG_FORMAT,
        datefmt=LOG_DATE_FORMAT,
        handlers=[logging.StreamHandler(sys.stdout)],
    )


# ── 파일 I/O (원자적 저장) ────────────────────────────────────────────────────
def save_json(data: object, path: Path) -> None:
    """
    임시 파일에 먼저 쓴 뒤 rename하는 원자적 저장.
    쓰기 도중 실패해도 기존 파일이 손상되지 않음.
    """
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp_fd, tmp_path = tempfile.mkstemp(dir=path.parent, suffix=".tmp")
    try:
        with os.fdopen(tmp_fd, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        Path(tmp_path).replace(path)
        logger.debug("저장 완료: %s", path)
    except Exception:
        Path(tmp_path).unlink(missing_ok=True)
        raise


def load_json(path: Path) -> object:
    """JSON 파일 로드. 파일 없음 / 파싱 오류 시 즉시 예외."""
    if not path.exists():
        raise FileNotFoundError(f"파일 없음: {path}")
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def save_json_str(data: object) -> str:
    """JSON 직렬화 문자열 반환 (OCI 업로드용)."""
    return json.dumps(data, ensure_ascii=False, indent=2)


# ── 시간 ──────────────────────────────────────────────────────────────────────
def now_iso() -> str:
    """UTC 기준 현재 시각을 ISO 8601 형식으로 반환."""
    return datetime.now(timezone.utc).isoformat()


def now_str() -> str:
    """UTC 기준 현재 시각을 'YYYY-MM-DD HH:MM:SS' 형식으로 반환."""
    return datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")


def now_display() -> str:
    """Discord 알림용 시각 표시."""
    return datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
