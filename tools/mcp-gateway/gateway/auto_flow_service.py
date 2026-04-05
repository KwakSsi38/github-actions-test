from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any

from gateway.input_normalizer import (
    GatewayValidationError,
    normalize_agent_type,
    normalize_finalize_decision,
    normalize_flow_step,
    normalize_keywords,
)
from gateway.spring_proxy_client import SpringProxyClient


@dataclass(frozen=True)
class SelectedSkill:
    category: str
    skill_id: int
    final_score: float
    source_repo: str
    skill_md_raw: str


class AutoFlowService:
    DEFAULT_AGENT_TYPE = "CODEX"

    def __init__(self, spring_proxy_client: SpringProxyClient):
        self.spring_proxy_client = spring_proxy_client

    def run(
        self,
        *,
        step: str,
        mcp_personal_token: str | None,
        agent_type: str | None,
        keywords: str | None,
        decision: str | None,
        customization_notes: str | None,
    ) -> dict[str, Any]:
        normalized_step = normalize_flow_step(step)

        if normalized_step == "START":
            return self._start(
                mcp_personal_token=mcp_personal_token,
                agent_type=agent_type,
            )

        if normalized_step == "COLLECTED":
            return self._collected(
                mcp_personal_token=mcp_personal_token,
                keywords=keywords,
            )

        return self._finalize(
            mcp_personal_token=mcp_personal_token,
            keywords=keywords,
            decision=decision,
            customization_notes=customization_notes,
        )

    def _start(self, *, mcp_personal_token: str | None, agent_type: str | None) -> dict[str, Any]:
        normalized_agent_type = self._resolve_agent_type(agent_type)

        template_response = self.spring_proxy_client.get_start_agent_template(
            mcp_personal_token=mcp_personal_token,
            agent_type=normalized_agent_type,
        )
        template_name, version, template_markdown = self._extract_template(template_response)

        return {
            "success": True,
            "flowStep": "START",
            "message": "start-agent 템플릿 조회 및 파일 생성 준비 완료",
            "templateMeta": {
                "templateName": template_name,
                "version": version,
            },
            "actions": {
                "writeFiles": [
                    {
                        "path": "start.agent.md",
                        "content": template_markdown,
                        "reason": "자동 플로우 시작 템플릿 생성",
                    }
                ],
                "askUser": [
                    "만들고 싶은 프로젝트 목표가 무엇인가요?",
                    "사용할 기술 스택/언어는 무엇인가요?",
                    "필수 제약(예: OCI, Docker, 기간, 비용)이 있나요?",
                    "기획 입력이 끝났으면 '기획 입력 끝'이라고 알려주세요.",
                ],
                "nextStep": "COLLECTED",
            },
        }

    def _collected(self, *, mcp_personal_token: str | None, keywords: str | None) -> dict[str, Any]:
        normalized_keywords = normalize_keywords(keywords)

        recommendation_response = self.spring_proxy_client.recommend_skills(
            mcp_personal_token=mcp_personal_token,
            keywords=normalized_keywords,
        )

        selected_skills = self._extract_selected_skills(recommendation_response)
        write_files = [self._build_skill_file_action(skill) for skill in selected_skills]

        return {
            "success": True,
            "flowStep": "COLLECTED",
            "message": "추천 스킬 조회 및 skills 파일 생성 준비 완료",
            "recommendation": {
                "keywords": normalized_keywords,
                "selectedSkills": [
                    {
                        "category": skill.category,
                        "skillId": skill.skill_id,
                        "finalScore": skill.final_score,
                        "sourceRepo": skill.source_repo,
                    }
                    for skill in selected_skills
                ],
            },
            "actions": {
                "writeFiles": write_files,
                "askUser": [
                    "생성된 skills 파일로 진행할까요?",
                    "아니면 사용자 맞춤형으로 더 보정할까요?",
                    "맞춤 보정(CUSTOMIZE)을 선택하면 기존 skills 파일을 기반으로 필요한 부분만 수정합니다.",
                    "처음부터 새로 작성하지 말고 기존 구조/코드/경로/식별자는 최대한 유지하세요.",
                ],
                "nextStep": "FINALIZE",
                "nextStepParamsExample": {
                    "step": "FINALIZE",
                    "keywords": normalized_keywords,
                    "decision": "ACCEPT",
                    "customizationNotes": "optional",
                },
            },
        }

    def _finalize(
        self,
        *,
        mcp_personal_token: str | None,
        keywords: str | None,
        decision: str | None,
        customization_notes: str | None,
    ) -> dict[str, Any]:
        normalized_keywords = normalize_keywords(keywords)
        normalized_decision = normalize_finalize_decision(decision)

        agents_markdown = self._build_agents_markdown(
            selected_skills=None,
            keywords=normalized_keywords,
            decision=normalized_decision,
            customization_notes=customization_notes,
        )

        return {
            "success": True,
            "flowStep": "FINALIZE",
            "message": "최종 agents.md 생성 준비 완료",
            "finalize": {
                "decision": normalized_decision,
                "keywords": normalized_keywords,
                "customizationNotes": customization_notes or "",
                "customizationPolicy": (
                    "CUSTOMIZE 선택 시 기존 skills 파일을 기반으로 필요한 부분만 보정하고, "
                    "새 문서를 처음부터 작성하지 않습니다."
                ),
            },
            "actions": {
                "writeFiles": [
                    {
                        "path": "agents.md",
                        "content": agents_markdown,
                        "reason": "최종 라우터 파일 덮어쓰기",
                    },
                ],
                "deleteFiles": ["start.agent.md"],
            },
        }

    def _extract_template(self, response: dict[str, Any]) -> tuple[str, str, str]:
        payload = response.get("data", response)
        if not isinstance(payload, dict):
            raise GatewayValidationError("start-agent response data must be a JSON object.")

        template_name = payload.get("templateName", "start.agent.md")
        version = payload.get("version", "v1")
        template_markdown = payload.get("templateMarkdown")

        if not isinstance(template_markdown, str) or not template_markdown.strip():
            raise GatewayValidationError("templateMarkdown is missing in start-agent response.")

        return str(template_name), str(version), template_markdown

    def _extract_selected_skills(self, response: dict[str, Any]) -> list[SelectedSkill]:
        payload = response.get("data", response)
        if not isinstance(payload, dict):
            raise GatewayValidationError("recommendation response data must be a JSON object.")

        raw_selected_skills = payload.get("selectedSkills")
        if not isinstance(raw_selected_skills, list) or not raw_selected_skills:
            raise GatewayValidationError("selectedSkills is missing or empty in recommendation response.")

        selected_skills: list[SelectedSkill] = []
        for raw_skill in raw_selected_skills:
            if not isinstance(raw_skill, dict):
                raise GatewayValidationError("selectedSkills item must be a JSON object.")

            category = str(raw_skill.get("category", "unknown")).strip() or "unknown"
            skill_id = self._parse_skill_id(raw_skill.get("skillId"))
            source_repo = str(raw_skill.get("sourceRepo", "unknown")).strip() or "unknown"
            skill_md_raw = str(raw_skill.get("skillMdRaw", "")).strip()

            if not skill_md_raw:
                raise GatewayValidationError("skillMdRaw is missing in selectedSkills item.")

            try:
                final_score = float(raw_skill.get("finalScore", 0.0))
            except (TypeError, ValueError):
                final_score = 0.0

            selected_skills.append(
                SelectedSkill(
                    category=category,
                    skill_id=skill_id,
                    final_score=final_score,
                    source_repo=source_repo,
                    skill_md_raw=skill_md_raw,
                )
            )

        return selected_skills

    def _parse_skill_id(self, raw_skill_id: Any) -> int:
        if isinstance(raw_skill_id, bool):
            return 0

        if isinstance(raw_skill_id, int):
            return max(raw_skill_id, 0)

        if isinstance(raw_skill_id, float):
            if raw_skill_id.is_integer():
                return max(int(raw_skill_id), 0)
            return 0

        if isinstance(raw_skill_id, str):
            normalized = raw_skill_id.strip()
            if normalized.lstrip("-").isdigit():
                return max(int(normalized), 0)

        return 0

    def _build_skill_file_action(self, selected_skill: SelectedSkill) -> dict[str, str]:
        safe_category = self._slug(selected_skill.category)
        path = f"skills/{safe_category}.md"
        content = self._build_skill_markdown(selected_skill)

        return {
            "path": path,
            "content": content,
            "reason": "추천 스킬 원문 파일 생성",
        }

    def _build_skill_markdown(self, selected_skill: SelectedSkill) -> str:
        return (
            f"# Skill: {selected_skill.category} ({selected_skill.skill_id})\n\n"
            f"- category: {selected_skill.category}\n"
            f"- skillId: {selected_skill.skill_id}\n"
            f"- finalScore: {selected_skill.final_score:.4f}\n"
            f"- sourceRepo: {selected_skill.source_repo}\n\n"
            "## Skill Markdown\n"
            f"{selected_skill.skill_md_raw}\n"
        )

    def _build_agents_markdown(
        self,
        *,
        selected_skills: list[SelectedSkill] | None,
        keywords: str,
        decision: str,
        customization_notes: str | None,
    ) -> str:
        lines = [
            "# AGENTS Routing",
            "",
            "## Generation Context",
            f"- decision: {decision}",
            f"- keywords: {keywords}",
            f"- customizationNotes: {customization_notes or ''}",
            "",
            "## Selected Skills",
        ]

        if selected_skills:
            for selected_skill in selected_skills:
                safe_category = self._slug(selected_skill.category)
                lines.append(
                    f"- skills/{safe_category}.md "
                    f"(category={selected_skill.category}, finalScore={selected_skill.final_score:.4f})"
                )
        else:
            lines.append("- skills/*.md (COLLECTED 단계에서 이미 생성된 파일 기준)")

        lines.extend(
            [
                "",
                "## Routing Rule",
                "- 사용자 요청 맥락에 따라 위 skills 파일 중 가장 적합한 항목을 선택해 사용한다.",
                "- 여러 카테고리가 동시에 필요한 경우 관련 skills를 조합해 응답한다.",
            ]
        )

        if decision == "CUSTOMIZE":
            lines.extend(
                [
                    "",
                    "## Customize Rule",
                    "- 기존 `skills/*.md`를 기준으로 필요한 섹션만 부분 보정한다.",
                    "- 기존 문서 구조, 코드 블록, 파일 경로, 클래스/함수명은 최대한 유지한다.",
                    "- 새 문서를 처음부터 다시 작성하지 않는다.",
                ]
            )

        return "\n".join(lines) + "\n"

    def _slug(self, raw_value: str) -> str:
        normalized = re.sub(r"[^a-zA-Z0-9_-]+", "-", raw_value.strip().lower())
        normalized = normalized.strip("-")
        return normalized or "unknown"

    def _resolve_agent_type(self, request_agent_type: str | None) -> str:
        return normalize_agent_type(request_agent_type or self.DEFAULT_AGENT_TYPE)
