import json, re

def normalize(text):
    return re.sub(r"[^a-zA-Z0-9]", "", text.split("/")[-1]).lower()

with open("data/ai-info/filtered_major_models.json", encoding="utf-8") as f:
    or_data = json.load(f)
with open("data/rankings/models_benchmark_raw.json", encoding="utf-8") as f:
    aa_data = json.load(f)

or_keys = {normalize(m["id"]): m["id"] for m in or_data["data"]}

failed = [m for m in aa_data["data"] if normalize(m["slug"]) not in or_keys
          and normalize(m.get("name","")) not in or_keys]

print(f"총 실패: {len(failed)}개")
print(f"\n=== OR에도 없는 것 (데이터 한계) ===")
for m in failed[:20]:
    print(f"  AA slug: {m['slug']}")