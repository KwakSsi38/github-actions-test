"""
calc_benchmark_stats.py — 지표별 요약 통계 생성

extract_benchmark_records에서 생성한 벤치마크 레코드를
지표 유형별로 집계하여 AVG / MAX / MIN / COUNT 통계를 저장.

출력 구조:
    [
      {
        "category":     "INTELLIGENCE",
        "avg_value":    72.4,
        "max_value":    95.1,
        "min_value":    40.2,
        "sample_count": 38,
        "last_updated": "2025-01-01 00:00:00"
      },
      ...
    ]
"""

import logging
import sys
from collections import defaultdict

from collectors.scripts.ai_info.config import RANKINGS_DIR
from collectors.scripts.ai_info.utils import current_timestamp, load_json, save_json, setup_logging

logger = logging.getLogger(__name__)

INPUT_FILE  = RANKINGS_DIR / "model_benchmarks_records.json"
OUTPUT_FILE = RANKINGS_DIR / "category_stats.json"


def calculate_stats(records: list[dict]) -> list[dict]:
    """레코드를 metric_type별로 집계하여 통계 반환."""
    grouped: dict[str, list[float]] = defaultdict(list)

    for record in records:
        metric_type = record.get("metric_type")
        value       = record.get("metric_value")
        if metric_type and isinstance(value, (int, float)):
            grouped[metric_type].append(float(value))

    now    = current_timestamp()
    stats: list[dict] = []

    for category, values in sorted(grouped.items()):
        if not values:
            continue
        stats.append({
            "category":     category,
            "avg_value":    round(sum(values) / len(values), 4),
            "max_value":    max(values),
            "min_value":    min(values),
            "sample_count": len(values),
            "last_updated": now,
        })

    return stats


def main() -> None:
    setup_logging()
    logger.info("지표별 요약 통계 생성 시작")

    records = load_json(INPUT_FILE)

    if not isinstance(records, list):
        logger.error("입력 파일 형식 오류 (list 기대, %s 수신).", type(records).__name__)
        sys.exit(1)

    stats = calculate_stats(records)

    if not stats:
        logger.error("통계 결과가 비어있습니다. 입력 데이터를 확인하세요.")
        sys.exit(1)

    save_json(stats, OUTPUT_FILE)
    logger.info("통계 생성 완료: %d개 지표 → %s", len(stats), OUTPUT_FILE)
    for s in stats:
        logger.info(
            "  %-15s 샘플 %3d개  평균: %8.4f  범위: [%.4f, %.4f]",
            s["category"], s["sample_count"], s["avg_value"], s["min_value"], s["max_value"],
        )


if __name__ == "__main__":
    main()
