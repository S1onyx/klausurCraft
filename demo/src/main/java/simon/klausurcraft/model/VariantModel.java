package simon.klausurcraft.model;

import org.w3c.dom.Element;

/**
 * Variant inside a subtask. Holds DOM element for autosave-on-change.
 */
public class VariantModel {
    private final Element dom; // <variant>
    private final String id;
    private String text;
    private String solution;

    public VariantModel(Element dom, String id, String text, String solution) {
        this.dom = dom;
        this.id = id;
        this.text = text == null ? "" : text;
        this.solution = solution == null ? "" : solution;
    }

    public Element getDom() { return dom; }
    public String getId() { return id; }
    public String getText() { return text; }
    public String getSolution() { return solution; }

    public void setText(String text) { this.text = text == null ? "" : text; }
    public void setSolution(String solution) { this.solution = solution == null ? "" : solution; }
}