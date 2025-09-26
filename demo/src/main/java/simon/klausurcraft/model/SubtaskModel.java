package simon.klausurcraft.model;

import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Subtask within a task. Holds DOM element for autosave-on-change.
 */
public class SubtaskModel {
    private final Element dom; // <subtask>
    private final TaskModel parent;

    private final String id;
    private BigDecimal points;
    private Difficulty difficulty;
    private Eligibility eligibility;

    private final List<VariantModel> variants = new ArrayList<>();

    public SubtaskModel(Element dom, TaskModel parent, String id, BigDecimal points,
                        Difficulty difficulty, Eligibility eligibility) {
        this.dom = dom;
        this.parent = parent;
        this.id = id;
        this.points = points;
        this.difficulty = difficulty;
        this.eligibility = eligibility;
    }

    public Element getDom() { return dom; }
    public TaskModel getParent() { return parent; }

    public String getId() { return id; }
    public BigDecimal getPoints() { return points; }
    public Difficulty getDifficulty() { return difficulty; }
    public Eligibility getEligibility() { return eligibility; }

    public void setPoints(BigDecimal points) { this.points = points; }
    public void setDifficulty(Difficulty d) { this.difficulty = d; }
    public void setEligibility(Eligibility e) { this.eligibility = e; }

    public List<VariantModel> getVariants() { return variants; }

    public boolean isEligibleFor(simon.klausurcraft.controller.HomeController.GenerateScope scope) {
        return switch (scope) {
            case EXAM -> eligibility == Eligibility.EXAM || eligibility == Eligibility.BOTH;
            case PRACTICE -> eligibility == Eligibility.PRACTICE || eligibility == Eligibility.BOTH;
            case BOTH -> true;
        };
    }
}