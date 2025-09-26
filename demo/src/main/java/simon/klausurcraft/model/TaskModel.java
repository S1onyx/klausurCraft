package simon.klausurcraft.model;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Task/topic root. Holds DOM element for autosave-on-change.
 */
public class TaskModel {
    private final Element dom; // <task>
    private final String id;
    private String title;
    private final List<SubtaskModel> subtasks = new ArrayList<>();

    public TaskModel(Element dom, String id, String title) {
        this.dom = dom;
        this.id = id;
        this.title = title == null ? "" : title;
    }

    public Element getDom() { return dom; }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t == null ? "" : t; }

    public List<SubtaskModel> getSubtasks() { return subtasks; }

    /** Lightweight shallow clone for filtering lists (not copying DOM). */
    public TaskModel cloneShallow() {
        return new TaskModel(dom, id, title);
    }
}
