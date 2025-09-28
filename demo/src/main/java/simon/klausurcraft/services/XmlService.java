package simon.klausurcraft.services;

import org.w3c.dom.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import simon.klausurcraft.model.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static javax.xml.transform.OutputKeys.ENCODING;
import static javax.xml.transform.OutputKeys.INDENT;

public class XmlService {

    private Path currentFile;
    private Document doc;

    public record LoadResult(List<TaskModel> tasks) {}

    public LoadResult load(Path xmlFile, Path xsdFile) throws Exception {
        this.currentFile = xmlFile;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(xsdFile.toFile());
        dbf.setSchema(schema);

        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(new ErrorHandler() {
            private String where(SAXParseException e) {
                return "(line " + e.getLineNumber() + ", col " + e.getColumnNumber() + ")";
            }
            @Override public void warning(SAXParseException e) {
                System.err.println("[XML WARNING] " + where(e) + " " + e.getMessage());
            }
            @Override public void error(SAXParseException e) throws SAXException {
                System.err.println("[XML ERROR] " + where(e) + " " + e.getMessage());
                throw e;
            }
            @Override public void fatalError(SAXParseException e) throws SAXException {
                System.err.println("[XML FATAL] " + where(e) + " " + e.getMessage());
                throw e;
            }
        });

        doc = db.parse(Files.newInputStream(xmlFile));
        doc.getDocumentElement().normalize();

        List<TaskModel> tasks = parseTasks(doc);
        return new LoadResult(tasks);
    }

    private List<TaskModel> parseTasks(Document d) {
        List<TaskModel> list = new ArrayList<>();
        NodeList nTasks = d.getElementsByTagName("task");
        for (int i = 0; i < nTasks.getLength(); i++) {
            Element eTask = (Element) nTasks.item(i);
            String id = eTask.getAttribute("id");
            String title = eTask.getAttribute("title");
            TaskModel t = new TaskModel(eTask, id, title);

            NodeList nSubs = eTask.getElementsByTagName("subtask");
            for (int j = 0; j < nSubs.getLength(); j++) {
                Element eSub = (Element) nSubs.item(j);
                if (!eSub.getParentNode().isSameNode(eTask)) continue;

                String sid = eSub.getAttribute("id");
                BigDecimal pts = new BigDecimal(eSub.getAttribute("points"));
                Difficulty diff = Difficulty.from(eSub.getAttribute("difficulty"));
                Eligibility elig = Eligibility.from(eSub.getAttribute("eligibility"));
                SubtaskModel st = new SubtaskModel(eSub, t, sid, pts, diff, elig);

                NodeList nVar = eSub.getElementsByTagName("variant");
                for (int k = 0; k < nVar.getLength(); k++) {
                    Element eVar = (Element) nVar.item(k);
                    Element eText = (Element) eVar.getElementsByTagName("text").item(0);
                    Element eSol = (Element) eVar.getElementsByTagName("solution").item(0);
                    String vid = eVar.getAttribute("id");
                    String text = eText != null ? getText(eText) : "";
                    String sol = eSol != null ? getText(eSol) : "";
                    VariantModel vm = new VariantModel(eVar, vid, text, sol);
                    st.getVariants().add(vm);
                }

                t.getSubtasks().add(st);
            }

            list.add(t);
        }
        return list;
    }

    private String getText(Element el) {
        return el.getTextContent() == null ? "" : el.getTextContent();
    }

    // ----- Helpers to access variants@group (subtask title) -----

    /** Read the variants@group (subtask title) of a subtask. */
    public String readSubtaskGroup(SubtaskModel st) {
        Element eSub = st.getDom();
        NodeList vars = eSub.getElementsByTagName("variants");
        if (vars.getLength() > 0) {
            Element eVars = (Element) vars.item(0);
            return eVars.getAttribute("group");
        }
        return "";
    }

    /** Update the variants@group (subtask title) and autosave. */
    public void updateSubtaskGroup(SubtaskModel st, String group) {
        Element eSub = st.getDom();
        Element eVars;
        NodeList vars = eSub.getElementsByTagName("variants");
        if (vars.getLength() > 0) {
            eVars = (Element) vars.item(0);
        } else {
            eVars = eSub.getOwnerDocument().createElement("variants");
            eSub.appendChild(eVars);
        }
        if (group == null) group = "";
        eVars.setAttribute("group", group);
        save();
    }

    // --- Autosave updates for other fields ---

    public void updateTaskTitle(TaskModel t) {
        t.getDom().setAttribute("title", t.getTitle());
        save();
    }

    public void updateSubtaskMeta(SubtaskModel st) {
        st.getDom().setAttribute("points", st.getPoints().stripTrailingZeros().toPlainString());
        st.getDom().setAttribute("difficulty", st.getDifficulty().toString());
        st.getDom().setAttribute("eligibility", st.getEligibility().toString());
        save();
    }

    public void updateVariant(VariantModel v) {
        Element e = v.getDom();
        NodeList nText = e.getElementsByTagName("text");
        Element eText = (Element) (nText.getLength() == 0 ? e.appendChild(e.getOwnerDocument().createElement("text")) : nText.item(0));
        eText.setTextContent(v.getText());

        NodeList nSol = e.getElementsByTagName("solution");
        Element eSol = (Element) (nSol.getLength() == 0 ? e.appendChild(e.getOwnerDocument().createElement("solution")) : nSol.item(0));
        eSol.setTextContent(v.getSolution() == null ? "" : v.getSolution());

        save();
    }

    // ====== NEW: create/delete APIs for tasks, subtasks, variants ======

    /** Create a new task with a 4-digit id and empty body. Returns the created TaskModel. */
    public Optional<TaskModel> addTask(String title) {
        if (doc == null) return Optional.empty();
        Element root = doc.getDocumentElement(); // <tasks>
        String nextId = nextId4(root, "task");
        Element eTask = doc.createElement("task");
        eTask.setAttribute("id", nextId);
        eTask.setAttribute("title", title == null ? "" : title);
        root.appendChild(eTask);

        TaskModel tm = new TaskModel(eTask, nextId, title);
        save();
        return Optional.of(tm);
    }

    /** Delete a task including all its subtasks. */
    public boolean deleteTask(TaskModel t) {
        try {
            Element eTask = t.getDom();
            eTask.getParentNode().removeChild(eTask);
            save();
            return true;
        } catch (Exception ex) {
            System.err.println("[XML DELETE TASK] " + ex.getMessage());
            return false;
        }
    }

    /** Create a subtask under given task with default meta and one empty variant. */
    public Optional<SubtaskModel> addSubtask(TaskModel task) {
        try {
            Element eTask = task.getDom();
            Document d = eTask.getOwnerDocument();

            String sid = nextId4(eTask, "subtask");
            Element eSub = d.createElement("subtask");
            eSub.setAttribute("id", sid);
            eSub.setAttribute("points", "1");
            eSub.setAttribute("difficulty", Difficulty.EASY.toString());
            eSub.setAttribute("eligibility", Eligibility.BOTH.toString());

            Element eVars = d.createElement("variants");
            eSub.appendChild(eVars);

            String vid = "0001";
            Element eVar = d.createElement("variant");
            eVar.setAttribute("id", vid);
            Element eText = d.createElement("text");
            eText.setTextContent("");
            Element eSol = d.createElement("solution");
            eSol.setTextContent("");
            eVar.appendChild(eText);
            eVar.appendChild(eSol);
            eVars.appendChild(eVar);

            eTask.appendChild(eSub);

            SubtaskModel st = new SubtaskModel(eSub, task, sid, new BigDecimal("1"), Difficulty.EASY, Eligibility.BOTH);
            VariantModel vm = new VariantModel(eVar, vid, "", "");
            st.getVariants().add(vm);
            task.getSubtasks().add(st);

            save();
            return Optional.of(st);
        } catch (Exception ex) {
            System.err.println("[XML ADD SUBTASK] " + ex.getMessage());
            return Optional.empty();
        }
    }

    /** Delete a subtask node. */
    public boolean deleteSubtask(TaskModel task, SubtaskModel st) {
        try {
            Element eSub = st.getDom();
            eSub.getParentNode().removeChild(eSub);
            save();
            return true;
        } catch (Exception ex) {
            System.err.println("[XML DELETE SUBTASK] " + ex.getMessage());
            return false;
        }
    }

    /** Create a new variant under given subtask. */
    public Optional<VariantModel> addVariant(SubtaskModel sub) {
        try {
            Element eSub = sub.getDom();
            Document d = eSub.getOwnerDocument();

            Element eVars = ensureVariants(eSub);

            String vid = nextId4(eVars, "variant");
            Element eVar = d.createElement("variant");
            eVar.setAttribute("id", vid);
            Element eText = d.createElement("text");
            eText.setTextContent("");
            Element eSol = d.createElement("solution");
            eSol.setTextContent("");
            eVar.appendChild(eText);
            eVar.appendChild(eSol);

            eVars.appendChild(eVar);

            VariantModel vm = new VariantModel(eVar, vid, "", "");
            sub.getVariants().add(vm);

            save();
            return Optional.of(vm);
        } catch (Exception ex) {
            System.err.println("[XML ADD VARIANT] " + ex.getMessage());
            return Optional.empty();
        }
    }

    /** Delete a variant node. */
    public boolean deleteVariant(SubtaskModel sub, VariantModel v) {
        try {
            Element eVar = v.getDom();
            eVar.getParentNode().removeChild(eVar);
            save();
            return true;
        } catch (Exception ex) {
            System.err.println("[XML DELETE VARIANT] " + ex.getMessage());
            return false;
        }
    }

    private Element ensureVariants(Element eSub) {
        NodeList vars = eSub.getElementsByTagName("variants");
        if (vars.getLength() > 0) return (Element) vars.item(0);
        Element eVars = eSub.getOwnerDocument().createElement("variants");
        eSub.appendChild(eVars);
        return eVars;
    }

    /** Compute next 4-digit id for direct children with given tag (e.g., "task", "subtask", "variant"). */
    private String nextId4(Element parent, String tag) {
        int max = 0;
        NodeList list = parent.getElementsByTagName(tag);
        for (int i = 0; i < list.getLength(); i++) {
            Element e = (Element) list.item(i);
            if (!e.getParentNode().isSameNode(parent)) continue; // only direct children
            String s = e.getAttribute("id");
            try {
                int n = Integer.parseInt(s);
                if (n > max) max = n;
            } catch (NumberFormatException ignored) {}
        }
        int next = max + 1;
        return String.format("%04d", next);
    }

    private void save() {
        if (doc == null || currentFile == null) return;
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(ENCODING, "UTF-8");
            t.setOutputProperty(INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            t.transform(new DOMSource(doc), new StreamResult(currentFile.toFile()));
        } catch (TransformerException e) {
            System.err.println("[XML SAVE ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }
}