package back.domain.mcp.recommendation.ranker;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.recommendation.dto.McpRecommendedSkillResponse;
import back.domain.mcp.recommendation.dto.McpRecommendationScoreBreakdown;

@Component
public class McpRecommendationRankerImpl implements McpRecommendationRanker {
    private static final Logger log = LoggerFactory.getLogger(McpRecommendationRankerImpl.class);

    private static final double FRESHNESS_DECAY_DAYS = 180.0;
    private static final int MAX_SELECTED_SKILLS = 10;
    private static final int DEBUG_TOP_CANDIDATE_COUNT = 5;
    private static final String UNCATEGORIZED = "uncategorized";

    private static final Comparator<ScoredCandidate> CANDIDATE_ORDER = Comparator
            .comparingDouble(ScoredCandidate::finalScore).reversed()
            .thenComparingDouble(ScoredCandidate::primaryScore).reversed()
            .thenComparingDouble(ScoredCandidate::freshnessNorm).reversed()
            .thenComparingInt(ScoredCandidate::stars).reversed()
            .thenComparingInt(ScoredCandidate::forks).reversed();

    @Value("${app.mcp.recommendation.ranking.weight.primary:0.60}")
    private double primaryScoreWeight = 0.60;

    @Value("${app.mcp.recommendation.ranking.weight.stars:0.20}")
    private double starsWeight = 0.20;

    @Value("${app.mcp.recommendation.ranking.weight.forks:0.10}")
    private double forksWeight = 0.10;

    @Value("${app.mcp.recommendation.ranking.weight.freshness:0.10}")
    private double freshnessWeight = 0.10;

    @Value("${app.mcp.recommendation.ranking.final-score-threshold:0.45}")
    private double finalScoreThreshold = 0.45;

    @Override
    public List<McpRecommendedSkillResponse> rank(List<McpRecommendationCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            log.info("[McpRecommendationRanker] ranking skipped. candidateCount=0");
            return List.of();
        }

        double maxStarsLog = candidates.stream()
                .mapToDouble(candidate -> Math.log1p(extractStars(candidate)))
                .max()
                .orElse(0.0);
        double maxForksLog = candidates.stream()
                .mapToDouble(candidate -> Math.log1p(extractForks(candidate)))
                .max()
                .orElse(0.0);

        List<ScoredCandidate> scoredCandidates = scoreCandidates(candidates, maxStarsLog, maxForksLog);
        List<ScoredCandidate> passedThreshold = scoredCandidates.stream()
                .filter(candidate -> candidate.finalScore() >= finalScoreThreshold)
                .toList();

        Map<String, ScoredCandidate> bestByCategory = selectBestByCategory(passedThreshold);
        List<ScoredCandidate> selectedScored = bestByCategory.values().stream()
                .sorted(CANDIDATE_ORDER)
                .limit(MAX_SELECTED_SKILLS)
                .toList();

        List<McpRecommendedSkillResponse> selected = selectedScored.stream()
                .map(ScoredCandidate::toResponse)
                .toList();

        int rejectedByThresholdCount = scoredCandidates.size() - passedThreshold.size();
        log.info(
                "[McpRecommendationRanker] ranking finished. candidateCount={}, threshold={}, "
                        + "passedThresholdCount={}, rejectedByThresholdCount={}, "
                        + "categoryWinnerCount={}, selectedCount={}",
                scoredCandidates.size(),
                finalScoreThreshold,
                passedThreshold.size(),
                rejectedByThresholdCount,
                bestByCategory.size(),
                selected.size());

        logDebugSummary(maxStarsLog, maxForksLog, passedThreshold, selectedScored);
        return selected;
    }

    private List<ScoredCandidate> scoreCandidates(
            List<McpRecommendationCandidate> candidates, double maxStarsLog, double maxForksLog) {
        List<ScoredCandidate> scoredCandidates = new ArrayList<>(candidates.size());

        for (int index = 0; index < candidates.size(); index++) {
            McpRecommendationCandidate candidate = candidates.get(index);
            ScoredCandidate scoredCandidate = scoreCandidate(candidate, maxStarsLog, maxForksLog);
            scoredCandidates.add(scoredCandidate);

            if (log.isTraceEnabled()) {
                logCandidateScoringTrace(index, candidate, scoredCandidate);
            }
        }

        return scoredCandidates;
    }

    private Map<String, ScoredCandidate> selectBestByCategory(List<ScoredCandidate> passedThreshold) {
        Map<String, ScoredCandidate> bestByCategory = new HashMap<>();

        for (ScoredCandidate candidate : passedThreshold) {
            ScoredCandidate previous = bestByCategory.get(candidate.category());
            if (previous == null) {
                bestByCategory.put(candidate.category(), candidate);
                if (log.isTraceEnabled()) {
                    log.trace(
                            "[McpRecommendationRanker] category winner selected. "
                                    + "category={}, skillId={}, finalScore={}",
                            candidate.category(),
                            candidate.skillId(),
                            candidate.finalScore());
                }
                continue;
            }

            ScoredCandidate winner = pickHigher(previous, candidate);
            bestByCategory.put(candidate.category(), winner);
            if (log.isTraceEnabled() && winner != previous) {
                log.trace(
                        "[McpRecommendationRanker] category winner replaced. category={}, prevSkillId={}, "
                                + "prevFinalScore={}, newSkillId={}, newFinalScore={}",
                        candidate.category(),
                        previous.skillId(),
                        previous.finalScore(),
                        winner.skillId(),
                        winner.finalScore());
            }
        }

        return bestByCategory;
    }

    private ScoredCandidate scoreCandidate(
            McpRecommendationCandidate candidate, double maxStarsLog, double maxForksLog) {
        double primaryScore = normalizePrimaryScore(candidate.primaryScore());
        int stars = extractStars(candidate);
        int forks = extractForks(candidate);

        double starsNorm = normalizeLogMetric(stars, maxStarsLog);
        double forksNorm = normalizeLogMetric(forks, maxForksLog);
        double freshnessNorm = calculateFreshnessNorm(candidate);
        double finalScore = (primaryScoreWeight * primaryScore)
                + (starsWeight * starsNorm)
                + (forksWeight * forksNorm)
                + (freshnessWeight * freshnessNorm);

        return new ScoredCandidate(
                candidate.skillId() == null ? 0L : candidate.skillId(),
                normalizeCategory(candidate.category()),
                finalScore,
                primaryScore,
                starsNorm,
                forksNorm,
                freshnessNorm,
                stars,
                forks,
                resolveSourceRepo(candidate),
                resolveSkillMdRaw(candidate));
    }

    private void logDebugSummary(
            double maxStarsLog,
            double maxForksLog,
            List<ScoredCandidate> passedThreshold,
            List<ScoredCandidate> selectedScored) {
        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug(
                "[McpRecommendationRanker] ranking policy. weights(primary={}, stars={}, forks={}, freshness={}), "
                        + "normalization(maxStarsLog={}, maxForksLog={})",
                primaryScoreWeight,
                starsWeight,
                forksWeight,
                freshnessWeight,
                maxStarsLog,
                maxForksLog);

        logTopCandidatesDebug("passed-threshold", passedThreshold);
        logTopCandidatesDebug("selected", selectedScored);
    }

    private void logTopCandidatesDebug(String label, List<ScoredCandidate> candidates) {
        List<ScoredCandidate> topCandidates = candidates.stream()
                .sorted(CANDIDATE_ORDER)
                .limit(DEBUG_TOP_CANDIDATE_COUNT)
                .toList();

        for (int index = 0; index < topCandidates.size(); index++) {
            ScoredCandidate candidate = topCandidates.get(index);
            log.debug(
                    "[McpRecommendationRanker] {}[{}] skillId={}, category={}, finalScore={}, "
                            + "score(primary={}, starsNorm={}, forksNorm={}, freshnessNorm={})",
                    label,
                    index,
                    candidate.skillId(),
                    candidate.category(),
                    candidate.finalScore(),
                    candidate.primaryScore(),
                    candidate.starsNorm(),
                    candidate.forksNorm(),
                    candidate.freshnessNorm());
        }
    }

    private ScoredCandidate pickHigher(ScoredCandidate left, ScoredCandidate right) {
        return CANDIDATE_ORDER.compare(left, right) <= 0 ? left : right;
    }

    private double normalizePrimaryScore(Double primaryScore) {
        if (primaryScore == null) {
            return 0.0;
        }
        if (primaryScore < 0.0) {
            return 0.0;
        }
        if (primaryScore > 1.0) {
            return 1.0;
        }
        return primaryScore;
    }

    private int extractStars(McpRecommendationCandidate candidate) {
        if (candidate.metadata() == null || candidate.metadata().stars() == null) {
            return 0;
        }
        return Math.max(candidate.metadata().stars(), 0);
    }

    private int extractForks(McpRecommendationCandidate candidate) {
        if (candidate.metadata() == null || candidate.metadata().forks() == null) {
            return 0;
        }
        return Math.max(candidate.metadata().forks(), 0);
    }

    private double normalizeLogMetric(int rawValue, double maxLogValue) {
        if (maxLogValue <= 0.0) {
            return 0.0;
        }
        return Math.log1p(rawValue) / maxLogValue;
    }

    private double calculateFreshnessNorm(McpRecommendationCandidate candidate) {
        if (candidate.metadata() == null || candidate.metadata().updatedAt() == null) {
            return 0.0;
        }

        try {
            OffsetDateTime updatedAt = OffsetDateTime.parse(candidate.metadata().updatedAt());
            long ageDays = ChronoUnit.DAYS.between(updatedAt.toLocalDate(), OffsetDateTime.now().toLocalDate());
            if (ageDays < 0) {
                ageDays = 0;
            }
            return Math.exp(-(ageDays / FRESHNESS_DECAY_DAYS));
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return UNCATEGORIZED;
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveSourceRepo(McpRecommendationCandidate candidate) {
        String repositoryUrl = candidate.repositoryUrl();
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return candidate.repositoryName() == null ? "" : candidate.repositoryName();
        }

        try {
            URI uri = URI.create(repositoryUrl);
            String[] segments = uri.getPath().split("/");
            if (segments.length >= 3) {
                String owner = segments[1];
                String repo = segments[2].replace(".git", "");
                return owner + "/" + repo;
            }
        } catch (RuntimeException ignored) {
            // URL 파싱 실패 시 repository_name으로 fallback
        }

        return candidate.repositoryName() == null ? "" : candidate.repositoryName();
    }

    private String resolveSkillMdRaw(McpRecommendationCandidate candidate) {
        if (candidate.contentMd() != null && !candidate.contentMd().isBlank()) {
            return candidate.contentMd();
        }

        if (candidate.summary() != null && !candidate.summary().isBlank()) {
            return candidate.summary();
        }

        return "";
    }

    private void logCandidateScoringTrace(int index, McpRecommendationCandidate source, ScoredCandidate scored) {
        String updatedAt = source.metadata() == null ? null : source.metadata().updatedAt();

        log.trace(
                "[McpRecommendationRanker] candidate[{}] skillId={}, category={}, primaryRaw={}, primaryNorm={}, "
                        + "stars={}, starsNorm={}, forks={}, forksNorm={}, updatedAt={}, freshnessNorm={}, "
                        + "finalScore={}",
                index,
                scored.skillId(),
                scored.category(),
                source.primaryScore(),
                scored.primaryScore(),
                scored.stars(),
                scored.starsNorm(),
                scored.forks(),
                scored.forksNorm(),
                updatedAt,
                scored.freshnessNorm(),
                scored.finalScore());
    }

    private record ScoredCandidate(
            long skillId,
            String category,
            double finalScore,
            double primaryScore,
            double starsNorm,
            double forksNorm,
            double freshnessNorm,
            int stars,
            int forks,
            String sourceRepo,
            String skillMdRaw) {
        private McpRecommendedSkillResponse toResponse() {
            return new McpRecommendedSkillResponse(
                    skillId,
                    category,
                    finalScore,
                    new McpRecommendationScoreBreakdown(primaryScore, starsNorm, forksNorm, freshnessNorm),
                    sourceRepo,
                    skillMdRaw);
        }
    }
}
