"""
extract_benchmark_records.py — 벤치마크 레코드 추출

Artificial Analysis raw 데이터에서 지표별 (모델, 지표, 값) 레코드를 추출.
calc_benchmark_stats에서 통계 집계 시 이 파일을 사용.

출력 구조:
    [
      {
        "model_api_id": "claude-3-5-sonnet",
        "metric_type":  "INTELLIGENCE",
        "metric_value": 87.3,
        "measured_at":  "2025-01-01 00:00:00",
        "unit":         "points"
      },
      ...
    ]
"""

import logging
import sys

from collectors.scripts.ai_info.config import BENCHMARK_METRICS, RANKINGS_DIR
from collectors.scripts.ai_info.utils import current_timestamp, get_nested, load_json, save_json, setup_logging

logger = logging.getLogger(__name__)

INPUT_FILE  = RANKINGS_DIR / "models_benchmark_raw.json"
OUTPUT_FILE = RANKINGS_DIR / "model_benchmarks_records.json"


def extract_benchmark_records(aa_data: dict) -> list[dict]:
    """각 모델의 지표 값을 평탄화된 레코드 리스트로 변환."""
    records: list[dict] = []
    measured_at = current_timestamp()
    skipped = 0

    for model in aa_data.get("data", []):
        model_slug = model.get("slug", "")

        for metric_type, dot_path, unit in BENCHMARK_METRICS:
            value = get_nested(model, dot_path)

            if value is None or not isinstance(value, (int, float)):
                skipped += 1
                continue

            records.append({
                "model_api_id": model_slug,
                "metric_type":  metric_type,
                "metric_value": float(value),
                "measured_at":  measured_at,
                "unit":         unit,
            })

    if skipped:
        logger.debug("값 없음으로 스킵된 항목: %d개", skipped)

    return records


def main() -> None:
    setup_logging()
    logger.info("벤치마크 레코드 추출 시작")

    raw_data = load_json(INPUT_FILE)
    records  = extract_benchmark_records(raw_data)

    if not records:
        logger.error("추출된 벤치마크 레코드가 없습니다.")
        sys.exit(1)

    save_json(records, OUTPUT_FILE)
    logger.info("추출 완료: 총 %d개 레코드 저장 → %s", len(records), OUTPUT_FILE)


if __name__ == "__main__":
    main()
