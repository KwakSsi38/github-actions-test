package back.domain.prompt.prompt.provider;

import back.domain.prompt.prompt.enums.Category;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "정적 규칙 맵으로 외부 수정이 없음")
@Component
public class KeywordRuleProvider {

    private static final Map<String, List<String>> TAG_RULES = Map.ofEntries(

            // Backend Framework
            Map.entry("spring", List.of("spring", "spring boot", "spring mvc", "spring data")),
            Map.entry("node", List.of("node", "nodejs", "express", "nestjs", "koa")),
            Map.entry("django", List.of("django")),
            Map.entry("flask", List.of("flask")),
            Map.entry("fastapi", List.of("fastapi")),

            // Frontend Framework
            Map.entry("react", List.of("react", "next", "next.js")),
            Map.entry("vue", List.of("vue", "nuxt", "nuxt.js")),
            Map.entry("angular", List.of("angular")),
            Map.entry("frontend", List.of("ui", "ux", "frontend", "component")),

            // DevOps / Infra
            Map.entry("docker", List.of("docker", "dockerfile", "container")),
            Map.entry("kubernetes", List.of("kubernetes", "k8s")),
            Map.entry("terraform", List.of("terraform")),
            Map.entry("aws", List.of("aws", "amazon web services", "ec2", "s3", "rds")),
            Map.entry("gcp", List.of("gcp", "google cloud")),
            Map.entry("ci-cd", List.of("ci/cd", "ci cd", "pipeline", "github actions", "jenkins")),
            Map.entry("monitoring", List.of("prometheus", "grafana", "monitoring", "logging")),

            // Database
            Map.entry("sql", List.of("sql", "query", "join")),
            Map.entry("postgres", List.of("postgres", "postgresql")),
            Map.entry("mysql", List.of("mysql", "mariadb")),
            Map.entry("mongodb", List.of("mongodb")),
            Map.entry("redis", List.of("redis", "cache")),

            // Security
            Map.entry("jwt", List.of("jwt")),
            Map.entry("oauth", List.of("oauth", "oauth2")),
            Map.entry("auth", List.of("authentication", "authorization")),
            Map.entry("security", List.of("security", "xss", "csrf", "cors")),

            // AI / Data
            Map.entry("llm", List.of("llm", "gpt")),
            Map.entry("rag", List.of("rag", "retrieval augmented generation")),
            Map.entry("embedding", List.of("embedding", "vector")),
            Map.entry("ml", List.of("machine learning", "deep learning", "model")),
            Map.entry("data", List.of("data pipeline", "dataset", "etl")),

            // Documentation
            Map.entry("swagger", List.of("swagger", "openapi")),
            Map.entry("docs", List.of("readme", "documentation", "docs", "adr")),

            // Testing
            Map.entry("testing", List.of("test", "testing", "junit", "pytest", "jest")),
            Map.entry("e2e", List.of("e2e", "cypress", "playwright")),
            Map.entry("tdd", List.of("tdd", "bdd")),

            // Code Quality
            Map.entry("refactoring", List.of("refactor", "refactoring")),
            Map.entry("clean-code", List.of("clean code", "clean architecture")),
            Map.entry("lint", List.of("eslint", "prettier", "lint")),
            Map.entry("design-pattern", List.of("design pattern", "solid")),

            // Automation
            Map.entry("automation", List.of("automation", "automate", "workflow")),
            Map.entry("script", List.of("script", "scripting")),
            Map.entry("cli", List.of("cli", "command line")),
            Map.entry("generator", List.of("generate", "generator", "codegen")),

            // Language (보조 태그)
            Map.entry("java", List.of("java")),
            Map.entry("python", List.of("python")),
            Map.entry("javascript", List.of("javascript", "js")),
            Map.entry("typescript", List.of("typescript", "ts")),
            Map.entry("go", List.of("golang", "go")),
            Map.entry("csharp", List.of("c#", "csharp")),
            Map.entry("kotlin", List.of("kotlin"))
    );


private static final Map<Category, List<String>> CATEGORY_RULES = Map.ofEntries(

        Map.entry(Category.BACKEND, List.of(
                // 공통 backend 개념
                "backend", "back-end", "server", "api", "rest", "rest api", "graphql",
                "endpoint", "controller", "service", "repository", "middleware",
                "authentication", "authorization", "session", "cookie",

                // Java / Spring
                "spring", "spring boot", "spring mvc", "spring web", "spring data jpa",
                "jpa", "hibernate", "mybatis", "servlet",

                // Node.js backend
                "node", "nodejs", "node.js", "express", "nestjs", "koa",

                // Python backend
                "django", "flask", "fastapi", "sqlalchemy", "wsgi", "asgi",

                // PHP backend
                "laravel", "symfony", "php-fpm",

                // Ruby backend
                "rails", "ruby on rails",

                // Go backend
                "gin", "echo", "fiber",

                // C# backend
                "asp.net", "asp.net core", ".net web api", "entity framework",

                // 기타 backend 문맥
                "business logic", "server-side", "web server"
        )),

        Map.entry(Category.FRONTEND, List.of(
                // 공통 frontend 개념
                "frontend", "front-end", "ui", "ux", "client", "client-side",
                "web app", "single page application", "spa",

                // 기본 웹 기술
                "html", "css", "scss", "sass", "javascript", "typescript",

                // 주요 프레임워크/라이브러리
                "react", "vue", "angular", "svelte",
                "next", "next.js", "nuxt", "nuxt.js",

                // 상태관리 / UI 라이브러리
                "redux", "zustand", "recoil", "mobx",
                "tailwind", "styled-components", "emotion",
                "material ui", "mui", "chakra ui",

                // 프론트 문맥
                "component", "hook", "routing", "responsive", "browser"
        )),

        Map.entry(Category.FULLSTACK, List.of(
                "fullstack", "full-stack",
                "end to end web app", "full application", "entire stack",
                "frontend and backend", "front and back",
                "monolith web app", "next fullstack", "bff"
        )),

        Map.entry(Category.DEVOPS, List.of(
                // 컨테이너 / 오케스트레이션
                "docker", "dockerfile", "container", "containerization",
                "kubernetes", "k8s", "helm",

                // CI/CD
                "ci/cd", "ci cd", "continuous integration", "continuous deployment",
                "pipeline", "github actions", "gitlab ci", "jenkins", "argocd",

                // 배포/운영
                "deploy", "deployment", "rollout", "release", "blue green", "canary",
                "devops", "sre", "observability", "monitoring", "logging",
                "prometheus", "grafana"
        )),

        Map.entry(Category.INFRA, List.of(
                // 클라우드
                "aws", "amazon web services", "gcp", "google cloud", "azure",
                "cloud", "infra", "infrastructure",

                // IaC / 네트워크
                "terraform", "iac", "vpc", "subnet", "route table", "security group",
                "load balancer", "elb", "alb", "network", "dns",

                // 컴퓨팅/스토리지
                "ec2", "s3", "rds", "cloudfront", "lambda", "eks", "ecs",

                // 서버/리소스 구성
                "server provisioning", "resource provisioning", "instance setup"
        )),

        Map.entry(Category.DATABASE, List.of(
                // 공통 DB 개념
                "database", "db", "sql", "query", "join", "subquery",
                "index", "execution plan", "optimization", "query tuning",
                "transaction", "lock", "deadlock",

                // 모델링 / 설계
                "schema", "erd", "normalization", "denormalization",
                "data modeling", "migration",

                // RDB
                "postgres", "postgresql", "mysql", "mariadb", "oracle", "sqlite",

                // NoSQL / 캐시
                "mongodb", "redis", "nosql"
        )),

        Map.entry(Category.CODE_QUALITY, List.of(
                // 코드 품질
                "code quality", "clean code", "readability", "maintainability",
                "best practice", "anti pattern", "anti-pattern",

                // 리팩토링 / 리뷰
                "refactor", "refactoring", "code review", "review", "naming",

                // 설계
                "solid", "design pattern", "clean architecture", "architecture improvement",

                // 정적 분석 / 스타일
                "lint", "eslint", "prettier", "static analysis", "sonarqube",
                "style guide", "code style"
        )),

        Map.entry(Category.TESTING_QA, List.of(
                // 테스트 개념
                "test", "testing", "qa", "quality assurance",
                "unit test", "integration test", "e2e", "end-to-end",
                "test strategy", "test case", "test coverage",

                // 테스트 도구
                "junit", "mockito", "pytest", "jest", "vitest", "cypress", "playwright",
                "mock", "stub", "fixture",

                // 방법론
                "tdd", "bdd"
        )),

        Map.entry(Category.DOCUMENTATION, List.of(
                // 문서화
                "documentation", "docs", "technical docs", "developer guide",
                "readme", "wiki", "manual",

                // API 문서
                "swagger", "openapi", "api docs", "api documentation",

                // 설계 문서
                "adr", "architecture decision record", "spec", "specification",
                "design doc"
        )),

        Map.entry(Category.AUTOMATION, List.of(
                // 자동화 일반
                "automation", "automate", "workflow", "workflow automation",
                "script", "scripting", "batch", "task runner",

                // 생성 / 툴링
                "generate", "generator", "codegen", "scaffold", "boilerplate",
                "cli", "tooling", "template",

                // 스케줄링
                "cron", "scheduler", "scheduled job"
        )),

        Map.entry(Category.DATA_AI, List.of(
                // AI / ML
                "ai", "ml", "machine learning", "deep learning", "model",
                "training", "inference", "fine tuning", "fine-tuning",

                // LLM / RAG
                "llm", "gpt", "prompt", "rag", "retrieval augmented generation",
                "embedding", "vector", "vector db", "vector search",

                // 데이터 처리
                "dataset", "data pipeline", "data processing",
                "pandas", "numpy", "scikit-learn", "tensorflow", "pytorch"
        )),

        Map.entry(Category.SECURITY, List.of(
                // 보안 일반
                "security", "secure coding", "vulnerability", "threat",

                // 인증/인가
                "auth", "authentication", "authorization", "jwt", "oauth", "oauth2",

                // 암호화 / 웹보안
                "encrypt", "encryption", "decrypt", "hash", "bcrypt",
                "csrf", "xss", "cors", "sql injection"
        ))
    );

    public Map<String, List<String>> getTagRules() {
        return TAG_RULES;
    }

    public Map<Category, List<String>> getCategoryRules() {
        return CATEGORY_RULES;
    }

}

