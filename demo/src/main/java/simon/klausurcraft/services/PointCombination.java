package simon.klausurcraft.services;

import simon.klausurcraft.model.Difficulty;
import simon.klausurcraft.model.SubtaskModel;
import simon.klausurcraft.model.TaskModel;
import simon.klausurcraft.model.GenerateScope;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes achievable point sums under eligibility and selects combinations
 * that (approximately) respect the 1/3 difficulty distribution rule.
 *
 * Rule:
 *  - Use integer points only.
 *  - Distribution target per category = round(N/3), tolerance ±1 per category.
 */
public final class PointCombination {

    private PointCombination() {}

    /** Return all achievable integer sums for a task respecting eligibility and distribution (non-empty). */
    public static List<Integer> achievablePointSums(TaskModel task, GenerateScope scope) {
        List<SubtaskModel> eligible = task.getSubtasks().stream()
            .filter(st -> st.isEligibleFor(scope))
            .collect(Collectors.toList());

        if (eligible.isEmpty()) return List.of();

        int max = eligible.stream().mapToInt(st -> st.getPoints().intValue()).sum();
        Set<Integer> sums = new TreeSet<>();

        Map<Integer, FeasibleDist> dp = new HashMap<>();
        dp.put(0, new FeasibleDist(0,0,0,0));

        for (SubtaskModel st : eligible) {
            int pts = st.getPoints().intValue();
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

        return new ArrayList<>(sums);
    }

    /** Pick an actual combination hitting the sum with near-1/3 distribution; returns null if impossible. */
    public static List<SubtaskModel> pickSubtasksWithDistribution(List<SubtaskModel> eligible, int targetSum) {
        eligible = new ArrayList<>(eligible);
        eligible.sort(Comparator.comparingInt(st -> -st.getPoints().intValue())); // big first to reduce branching

        List<SubtaskModel> best = new ArrayList<>();
        backtrack(eligible, 0, targetSum, new ArrayList<>(), new int[3], best);
        return best.isEmpty() ? null : best;
    }

    private static void backtrack(List<SubtaskModel> arr, int idx, int remaining,
                                  List<SubtaskModel> cur, int[] dist, List<SubtaskModel> best) {
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
        for (int i = idx; i < arr.size(); i++) maxPossible += arr.get(i).getPoints().intValue();
        if (maxPossible < remaining) return;

        SubtaskModel st = arr.get(idx);
        int dIdx = switch (st.getDifficulty()) {
            case EASY -> 0; case MEDIUM -> 1; case HARD -> 2;
        };
        cur.add(st);
        dist[dIdx]++;
        backtrack(arr, idx + 1, remaining - st.getPoints().intValue(), cur, dist, best);
        cur.remove(cur.size() - 1);
        dist[dIdx]--;

        backtrack(arr, idx + 1, remaining, cur, dist, best);
    }

    private static boolean distributionOk(int[] dist) {
        int n = dist[0] + dist[1] + dist[2];
        if (n == 0) return false;
        int target = Math.round(n / 3f);
        return Math.abs(dist[0] - target) <= 1 &&
               Math.abs(dist[1] - target) <= 1 &&
               Math.abs(dist[2] - target) <= 1;
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