package simon.klausurcraft.ui.home;

import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;

import java.math.BigDecimal;
import java.util.List;

/**
 * Status aggregation helper used by the Home bottom status bar.
 */
final class HomeStatusService {

    private HomeStatusService() {}

    /**
     * Aggregates task/subtask counters and summary values for the status bar.
     *
     * @param tasks all loaded tasks
     * @return aggregated status object
     */
    static StatusStats computeStatusStats(List<Task> tasks) {
        StatusStats stats = new StatusStats();
        if (tasks == null) {
            return stats;
        }

        stats.tasks = tasks.size();
        for (Task task : tasks) {
            for (Subtask subtask : task.getSubtasks()) {
                stats.subtasks++;
                stats.variants += subtask.getVariants().size();

                if (subtask.getPoints() != null) {
                    stats.points = stats.points.add(subtask.getPoints());
                }

                Difficulty difficulty = subtask.getDifficulty();
                if (difficulty == null) {
                    stats.withoutDifficulty++;
                } else {
                    switch (difficulty) {
                        case EASY -> stats.easy++;
                        case MEDIUM -> stats.medium++;
                        case HARD -> stats.hard++;
                    }
                }

                Eligibility eligibility = subtask.getEligibility();
                if (eligibility == null) {
                    stats.withoutEligibility++;
                } else {
                    switch (eligibility) {
                        case EXAM -> stats.exam++;
                        case PRACTICE -> stats.practice++;
                        case BOTH -> stats.both++;
                    }
                }
            }
        }

        return stats;
    }

    /**
     * Builds the multi-line tooltip text shown over the status counters.
     *
     * @param stats precomputed status values
     * @return formatted tooltip content
     */
    static String buildStatusTooltip(StatusStats stats) {
        return "Unten angezeigt: Tasks / Subtasks\n\n"
                + "Tasks: " + stats.tasks + "\n"
                + "Subtasks: " + stats.subtasks + "\n"
                + "Varianten: " + stats.variants + "\n"
                + "Punkte gesamt: " + Points.toDisplayString(stats.points) + "\n\n"
                + "Schwierigkeit (Subtasks):\n"
                + "easy: " + stats.easy + "\n"
                + "medium: " + stats.medium + "\n"
                + "hard: " + stats.hard
                + (stats.withoutDifficulty > 0 ? "\nohne Schwierigkeit: " + stats.withoutDifficulty : "")
                + "\n\n"
                + "Eignung (Subtasks):\n"
                + "exam: " + stats.exam + "\n"
                + "practice: " + stats.practice + "\n"
                + "both: " + stats.both
                + (stats.withoutEligibility > 0 ? "\nohne Eignung: " + stats.withoutEligibility : "");
    }

    /**
     * Value container for status-bar aggregation.
     */
    static final class StatusStats {
        int tasks;
        int subtasks;
        int variants;
        BigDecimal points = Points.ZERO;

        int easy;
        int medium;
        int hard;
        int withoutDifficulty;

        int exam;
        int practice;
        int both;
        int withoutEligibility;
    }
}
