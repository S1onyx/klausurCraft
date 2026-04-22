package simon.klausurcraft.ui.home;

import simon.klausurcraft.task.GenerateScope;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Variant;
import simon.klausurcraft.task.planning.PointDistributionPlanner;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Extracted non-UI planning logic for the generate wizard.
 */
final class HomeGeneratePlanner {

    private HomeGeneratePlanner() {}

    /**
     * Picks one random variant from a non-empty variant list.
     *
     * @param variants available variants of one subtask
     * @return random variant or {@code null} if none is available
     */
    static Variant pickVariant(List<Variant> variants) {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
    }

    /**
     * Moves one task selection from pool to selected list.
     *
     * @param item task selection to move
     * @param pool source list
     * @param selected target list
     * @param scope current generate scope
     * @return {@code true} if the task was moved
     */
    static boolean moveToSelected(TaskSelection item,
                                  List<TaskSelection> pool,
                                  List<TaskSelection> selected,
                                  GenerateScope scope) {
        if (item == null) {
            return false;
        }

        suggestPointForTaskSelection(scope, item);
        if (item.getAchievable().isEmpty()) {
            return false;
        }

        item.setEnabled(true);
        pool.remove(item);
        if (!selected.contains(item)) {
            selected.add(item);
        }
        return true;
    }

    /**
     * Moves one task selection back from selected to pool.
     *
     * @param item task selection to move
     * @param selected source list
     * @param pool target list
     * @return {@code true} if processed
     */
    static boolean moveToPool(TaskSelection item,
                              List<TaskSelection> selected,
                              List<TaskSelection> pool) {
        if (item == null) {
            return false;
        }

        item.setEnabled(false);
        selected.remove(item);
        if (!pool.contains(item)) {
            pool.add(item);
        }
        return true;
    }

    /**
     * Recomputes achievable values and applies one suggested target sum.
     *
     * @param scope current generate scope
     * @param taskSelection selection entry to tune
     */
    static void suggestPointForTaskSelection(GenerateScope scope, TaskSelection taskSelection) {
        if (taskSelection == null) {
            return;
        }

        taskSelection.recomputeAchievable(scope);
        if (taskSelection.getAchievable().isEmpty()) {
            taskSelection.chosenPointsProperty().set(Points.ZERO);
            return;
        }

        BigDecimal fallback = taskSelection.getAchievable().get(0);
        BigDecimal suggested = PointDistributionPlanner
                .suggestBestPointSum(taskSelection.getTask(), scope)
                .orElse(fallback);
        taskSelection.chosenPointsProperty().set(suggested);
    }

    /**
     * Applies suggestions for all currently selected tasks.
     *
     * @param scope current generate scope
     * @param selected selected tasks
     * @return number of rows where the chosen points changed
     */
    static int suggestPointsForAll(GenerateScope scope, List<TaskSelection> selected) {
        int changedCount = 0;
        for (TaskSelection taskSelection : selected) {
            BigDecimal before = taskSelection.getChosenPoints();
            suggestPointForTaskSelection(scope, taskSelection);
            if (!taskSelection.getChosenPoints().equals(before)) {
                changedCount++;
            }
        }
        return changedCount;
    }

    /**
     * Picks a starter set of tasks and tunes points for each selected row.
     *
     * @param scope current generate scope
     * @param selected currently selected tasks
     * @param pool available pool tasks
     * @return number of tuned tasks ({@code 0} if no candidate exists)
     */
    static int applySmartFill(GenerateScope scope,
                              List<TaskSelection> selected,
                              List<TaskSelection> pool) {
        List<TaskSelection> candidates = pool.stream()
                .filter(taskSelection -> !taskSelection.getAchievable().isEmpty())
                .sorted((a, b) -> Integer.compare(b.getAchievable().size(), a.getAchievable().size()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return 0;
        }

        int desired = Math.min(4, Math.max(1, selected.size() + candidates.size()));
        int missing = Math.max(0, desired - selected.size());

        for (TaskSelection candidate : candidates) {
            if (missing <= 0) {
                break;
            }
            if (!pool.contains(candidate)) {
                continue;
            }
            moveToSelected(candidate, pool, selected, scope);
            missing--;
        }

        int tuned = 0;
        for (TaskSelection taskSelection : selected) {
            taskSelection.recomputeAchievable(scope);
            if (taskSelection.getAchievable().isEmpty()) {
                continue;
            }

            BigDecimal fallback = taskSelection.getAchievable().get(0);
            BigDecimal target = PointDistributionPlanner
                    .suggestBestPointSum(taskSelection.getTask(), scope)
                    .orElse(fallback);
            taskSelection.chosenPointsProperty().set(target);
            tuned++;
        }

        return tuned;
    }
}
