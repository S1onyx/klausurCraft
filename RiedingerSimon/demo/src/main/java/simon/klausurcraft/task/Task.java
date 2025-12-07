package simon.klausurcraft.task;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Task/topic root. Holds DOM element for autosave-on-change.
 */
public class Task {
    private final Element dom; // <task>
    private final String id;
    private String title;
    private final List<Subtask> subtasks = new ArrayList<>();

    public Task(Element dom, String id, String title) {
        this.dom = dom;
        this.id = id;
        this.title = title == null ? "" : title;
    }

    public Element getDom() { return dom; }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t == null ? "" : t; }

    public List<Subtask> getSubtasks() { return subtasks; }

    /** Lightweight shallow clone for filtering lists (not copying DOM). */
    public Task cloneShallow() {
        return new Task(dom, id, title);
    }
}
