package simon.klausurcraft.testutil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.task.Variant;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigDecimal;

public final class TaskFixtures {

    private TaskFixtures() {}

    public static Task newTask(String id, String title) {
        Document doc = newDocument();
        Element root = doc.createElement("tasks");
        doc.appendChild(root);

        Element taskEl = doc.createElement("task");
        taskEl.setAttribute("id", id);
        taskEl.setAttribute("title", title);
        root.appendChild(taskEl);

        return new Task(taskEl, id, title);
    }

    public static Subtask addSubtask(Task task, String id, String points,
                                     Difficulty difficulty, Eligibility eligibility) {
        Document d = task.getDom().getOwnerDocument();
        Element sub = d.createElement("subtask");
        sub.setAttribute("id", id);
        sub.setAttribute("points", points);
        sub.setAttribute("difficulty", difficulty.toString());
        sub.setAttribute("eligibility", eligibility.toString());

        Element vars = d.createElement("variants");
        vars.setAttribute("group", "Group " + id);
        sub.appendChild(vars);

        Element var = d.createElement("variant");
        var.setAttribute("id", "0001");
        Element text = d.createElement("text");
        text.setTextContent("Text " + id);
        Element solution = d.createElement("solution");
        solution.setTextContent("Solution " + id);
        var.appendChild(text);
        var.appendChild(solution);
        vars.appendChild(var);

        task.getDom().appendChild(sub);

        Subtask st = new Subtask(sub, task, id, new BigDecimal(points), difficulty, eligibility);
        st.getVariants().add(new Variant(var, "0001", text.getTextContent(), solution.getTextContent()));
        task.getSubtasks().add(st);
        return st;
    }

    private static Document newDocument() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.newDocument();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build test DOM document", e);
        }
    }
}
