"""
ai_info/utils.py — ai_info 파이프라인 전용 유틸리티

shared/utils.py의 공통 함수(save_json, load_json, setup_logging 등)를 재사용하고,
ai_info 도메인 특화 함수(정규화, 패밀리명 추출 등)만 추가로 정의.
"""

import re

from collectors.scripts.ai_info.config import TIER_KEYWORDS
from collectors.scripts.shared.utils import (  # noqa: F401 — re-export
    load_json,
    now_str as current_timestamp,
    save_json,
    setup_logging,
)


def normalize_for_match(text: str) -> str:
    text = text.lower()
    text = re.sub(r"[^a-z0-9\s]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def extract_family_name(model_name: str) -> str:
    version_match = re.search(r"\d+(?:\.\d+)?", model_name)
    version = version_match.group() if version_match else ""
    tokens = re.sub(r"[^a-zA-Z0-9\s]", " ", model_name).split()
    tokens = [
        t for t in tokens
        if t.lower() not in TIER_KEYWORDS and not re.fullmatch(r"\d+(\.\d+)?", t)
    ]
    base = tokens[0].upper() if tokens else "UNKNOWN"
    return f"{base}-{version}" if version else base


def get_nested(obj: dict, dot_path: str) -> object:
    for key in dot_path.split("."):
        if not isinstance(obj, dict):
            return None
        obj = obj.get(key)
        if obj is None:
            return None
    return obj
