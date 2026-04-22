package simon.klausurcraft.task;

import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Subtask within a task. Holds DOM element for autosave-on-change.
 */
public class Subtask {
    private final Element dom; // <subtask>
    private final Task parent;

    private final String id;
    private BigDecimal points;
    private Difficulty difficulty;
    private Eligibility eligibility;

    private final List<Variant> variants = new ArrayList<>();

    public Subtask(Element dom, Task parent, String id, BigDecimal points,
                   Difficulty difficulty, Eligibility eligibility) {
        this.dom = dom;
        this.parent = parent;
        this.id = id;
        this.points = points;
        this.difficulty = difficulty;
        this.eligibility = eligibility;
    }

    public Element getDom() { return dom; }
    public Task getParent() { return parent; }

    public String getId() { return id; }
    public BigDecimal getPoints() { return points; }
    public Difficulty getDifficulty() { return difficulty; }
    public Eligibility getEligibility() { return eligibility; }

    public void setPoints(BigDecimal points) { this.points = points; }
    public void setDifficulty(Difficulty d) { this.difficulty = d; }
    public void setEligibility(Eligibility e) { this.eligibility = e; }

    public List<Variant> getVariants() { return variants; }

    public boolean isEligibleFor(GenerateScope scope) {
        if (scope == null || eligibility == null) return false;
        if (scope == GenerateScope.BOTH) return true;
        if (scope == GenerateScope.EXAM) {
            return eligibility == Eligibility.EXAM || eligibility == Eligibility.BOTH;
        }
        if (scope == GenerateScope.PRACTICE) {
            return eligibility == Eligibility.PRACTICE || eligibility == Eligibility.BOTH;
        }
        return false;
    }
}
