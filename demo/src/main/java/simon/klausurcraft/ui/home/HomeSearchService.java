package simon.klausurcraft.ui.home;

import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;

/**
 * Search/filter helper for task/subtask matching in the center view.
 */
final class HomeSearchService {

    private HomeSearchService() {}

    /**
     * Checks whether a query matches the task header fields.
     *
     * @param task task candidate
     * @param query user query string
     * @return {@code true} if id or title matches
     */
    static boolean matchesTaskHeader(Task task, String query) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        if (task.getId().toLowerCase().contains(normalizedQuery)) {
            return true;
        }
        return task.getTitle().toLowerCase().contains(normalizedQuery);
    }

    /**
     * Checks whether a query matches a subtask row.
     *
     * @param subtask subtask candidate
     * @param subtaskGroup human-readable subtask group
     * @param query user query string
     * @return {@code true} if id, variant text, solution or group matches
     */
    static boolean matchesSubtask(Subtask subtask, String subtaskGroup, String query) {
        String normalizedQuery = normalizeQuery(query);
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        if (subtask.getId().toLowerCase().contains(normalizedQuery)) {
            return true;
        }

        String group = subtaskGroup == null ? "" : subtaskGroup.toLowerCase();
        return subtask.getVariants().stream().anyMatch(variant ->
            (variant.getText() != null && variant.getText().toLowerCase().contains(normalizedQuery))
            || (variant.getSolution() != null && variant.getSolution().toLowerCase().contains(normalizedQuery))
            || group.contains(normalizedQuery)
        );
    }

    /**
     * Formats one task title in the sidebar/tree style.
     *
     * @param task task to format
     * @return task label ({@code ID — title})
     */
    static String formatTaskTitle(Task task) {
        return String.format("%s \u2014 %s", task.getId(), task.getTitle());
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase();
    }
}
