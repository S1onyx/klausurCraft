package simon.klausurcraft.task.planning;

import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.task.GenerateScope;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes achievable point sums under eligibility and selects combinations
 * that (approximately) respect the 1/3 difficulty distribution rule.
 *
 * Rule:
 *  - Use points in half-step units (0.5).
 *  - Distribution target per category = round(N/3), tolerance ±1 per category.
 */
public final class PointDistributionPlanner {

    private PointDistributionPlanner() {}

    /** Return all achievable sums for a task respecting eligibility and distribution (non-empty). */
    public static List<BigDecimal> achievablePointSums(Task task, GenerateScope scope) {
        List<Subtask> eligible = task.getSubtasks().stream()
            .filter(st -> st.isEligibleFor(scope))
            .collect(Collectors.toList());

        if (eligible.isEmpty()) return List.of();

        Set<Integer> sums = new TreeSet<>();

        Map<Integer, FeasibleDist> dp = new HashMap<>();
        dp.put(0, new FeasibleDist(0,0,0,0));

        for (Subtask st : eligible) {
            int pts = Points.toHalfSteps(st.getPoints());
            Difficulty d = st.getDifficulty();
            Map<Integer, FeasibleDist> next = new HashMap<>(dp);
            for (Map.Entry<Integer, FeasibleDist> e : dp.entrySet()) {
                int ns = e.getKey() + pts;
                FeasibleDist fd = e.getValue().add(d);
                FeasibleDist prev = next.get(ns);
                if (prev == null || fd.count > prev.count) {
                    next.put(ns, fd);
                }
            }
            dp = next;
        }

        for (Map.Entry<Integer, FeasibleDist> e : dp.entrySet()) {
            if (e.getKey() == 0) continue;
            if (e.getValue().isDistributionOk()) {
                sums.add(e.getKey());
            }
        }

        return sums.stream()
            .map(Points::fromHalfSteps)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Pick an actual combination hitting the sum with near-1/3 distribution; returns null if impossible. */
    public static List<Subtask> pickSubtasksWithDistribution(List<Subtask> eligible, BigDecimal targetSum) {
        eligible = new ArrayList<>(eligible);
        eligible.sort(Comparator.comparingInt(st -> -Points.toHalfSteps(st.getPoints()))); // big first to reduce branching

        int targetHalfSteps = Points.toHalfSteps(targetSum);

        List<Subtask> best = new ArrayList<>();
        backtrack(eligible, 0, targetHalfSteps, new ArrayList<>(), new int[3], best);
        return best.isEmpty() ? null : best;
    }

    private static void backtrack(List<Subtask> arr, int idx, int remaining,
                                  List<Subtask> cur, int[] dist, List<Subtask> best) {
        if (remaining == 0) {
            if (distributionOk(dist)) {
                if (cur.size() > best.size()) {
                    best.clear();
                    best.addAll(cur);
                }
            }
            return;
        }
        if (remaining < 0 || idx >= arr.size()) return;

        int maxPossible = 0;
        for (int i = idx; i < arr.size(); i++) maxPossible += Points.toHalfSteps(arr.get(i).getPoints());
        if (maxPossible < remaining) return;

        Subtask st = arr.get(idx);
        int dIdx = switch (st.getDifficulty()) {
            case EASY -> 0; case MEDIUM -> 1; case HARD -> 2;
        };
        cur.add(st);
        dist[dIdx]++;
        backtrack(arr, idx + 1, remaining - Points.toHalfSteps(st.getPoints()), cur, dist, best);
        cur.remove(cur.size() - 1);
        dist[dIdx]--;

        backtrack(arr, idx + 1, remaining, cur, dist, best);
    }

    /**
     * Suggests the best point sum for a task in the given scope.
     * Ranking:
     *  1) minimal distribution deviation to exact 1/3 split
     *  2) higher total points
     */
    public static Optional<BigDecimal> suggestBestPointSum(Task task, GenerateScope scope) {
        List<Subtask> eligible = task.getSubtasks().stream()
                .filter(st -> st.isEligibleFor(scope))
                .collect(Collectors.toList());
        if (eligible.isEmpty()) return Optional.empty();

        List<BigDecimal> sums = achievablePointSums(task, scope);
        if (sums.isEmpty()) return Optional.empty();

        BigDecimal bestSum = null;
        double bestDeviation = Double.POSITIVE_INFINITY;
        int bestHalfSteps = -1;

        for (BigDecimal sum : sums) {
            List<Subtask> chosen = pickSubtasksWithDistribution(eligible, sum);
            if (chosen == null || chosen.isEmpty()) continue;

            double deviation = distributionDeviation(chosen);
            int halfSteps = Points.toHalfSteps(sum);

            if (deviation < bestDeviation || (Double.compare(deviation, bestDeviation) == 0 && halfSteps > bestHalfSteps)) {
                bestDeviation = deviation;
                bestHalfSteps = halfSteps;
                bestSum = sum;
            }
        }

        return Optional.ofNullable(bestSum);
    }

    private static boolean distributionOk(int[] dist) {
        int n = dist[0] + dist[1] + dist[2];
        if (n == 0) return false;
        int target = Math.round(n / 3f);
        return Math.abs(dist[0] - target) <= 1 &&
               Math.abs(dist[1] - target) <= 1 &&
               Math.abs(dist[2] - target) <= 1;
    }

    private static double distributionDeviation(List<Subtask> subtasks) {
        int easy = 0;
        int med = 0;
        int hard = 0;
        for (Subtask st : subtasks) {
            switch (st.getDifficulty()) {
                case EASY -> easy++;
                case MEDIUM -> med++;
                case HARD -> hard++;
            }
        }

        int n = easy + med + hard;
        if (n == 0) return Double.POSITIVE_INFINITY;

        double target = n / 3.0;
        return Math.abs(easy - target) + Math.abs(med - target) + Math.abs(hard - target);
    }

    private record FeasibleDist(int easy, int med, int hard, int count) {
        FeasibleDist add(Difficulty d) {
            return switch (d) {
                case EASY -> new FeasibleDist(easy + 1, med, hard, count + 1);
                case MEDIUM -> new FeasibleDist(easy, med + 1, hard, count + 1);
                case HARD -> new FeasibleDist(easy, med, hard + 1, count + 1);
            };
        }
        boolean isDistributionOk() {
            return distributionOk(new int[]{easy, med, hard});
        }
    }
}
