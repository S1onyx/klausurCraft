package simon.klausurcraft.task.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.task.Variant;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskXmlStoreTest {

    private static final Path XSD = Path.of("src/main/resources/simon/klausurcraft/exam-tasks.xsd");

    @TempDir
    Path tempDir;

    @Test
    void createNewAndCrud_roundtripPersistsExpectedState() throws Exception {
        // End-to-end use case sample: create -> edit -> save -> reload -> delete.
        Path xml = tempDir.resolve("tasks.xml");
        TaskXmlStore store = new TaskXmlStore();

        assertTrue(Files.exists(XSD), "XSD file must exist for integration tests.");
        assertTrue(store.createNew(xml, XSD).tasks().isEmpty());
        assertTrue(Files.exists(xml));

        Task task = store.addTask("Databases").orElseThrow();
        assertEquals("0001", task.getId());

        Subtask first = store.addSubtask(task).orElseThrow();
        assertEquals("0001", first.getId());
        first.setPoints(new BigDecimal("1.5"));
        first.setDifficulty(Difficulty.HARD);
        first.setEligibility(Eligibility.EXAM);
        store.updateSubtaskMeta(first);
        store.updateSubtaskGroup(first, "Sharding");

        Variant v1 = first.getVariants().getFirst();
        v1.setText("Explain CAP");
        v1.setSolution("Consistency, Availability, Partition tolerance.");
        store.updateVariant(v1);

        Variant v2 = store.addVariant(first).orElseThrow();
        v2.setText("Explain quorum");
        v2.setSolution("Read/write with majority intersection.");
        store.updateVariant(v2);
        assertTrue(store.deleteVariant(first, v2));

        Subtask toDelete = store.addSubtask(task).orElseThrow();
        assertTrue(store.deleteSubtask(task, toDelete));

        task.setTitle("Databases Updated");
        store.updateTaskTitle(task);

        TaskXmlStore verify = new TaskXmlStore();
        List<Task> loaded = verify.load(xml, XSD).tasks();
        assertEquals(1, loaded.size());

        Task reloadedTask = loaded.getFirst();
        assertEquals("Databases Updated", reloadedTask.getTitle());
        assertEquals(1, reloadedTask.getSubtasks().size());

        Subtask reloadedSubtask = reloadedTask.getSubtasks().getFirst();
        assertEquals(new BigDecimal("1.5"), reloadedSubtask.getPoints());
        assertEquals(Difficulty.HARD, reloadedSubtask.getDifficulty());
        assertEquals(Eligibility.EXAM, reloadedSubtask.getEligibility());
        assertEquals("Sharding", verify.readSubtaskGroup(reloadedSubtask));
        assertEquals(1, reloadedSubtask.getVariants().size());
        assertEquals("Explain CAP", reloadedSubtask.getVariants().getFirst().getText());
        assertEquals("Consistency, Availability, Partition tolerance.", reloadedSubtask.getVariants().getFirst().getSolution());

        assertTrue(verify.deleteTask(reloadedTask));
        List<Task> afterDelete = new TaskXmlStore().load(xml, XSD).tasks();
        assertTrue(afterDelete.isEmpty());
    }

    @Test
    void addTaskWithoutLoadedDocument_returnsEmptyOptional() {
        // Error case: repository is not initialized with a document.
        TaskXmlStore store = new TaskXmlStore();
        assertTrue(store.addTask("No doc").isEmpty());
    }

    @Test
    void addTask_generatesSequentialFourDigitIds() throws Exception {
        // Equivalence class for id generation: multiple consecutive creations.
        Path xml = tempDir.resolve("ids.xml");
        TaskXmlStore store = new TaskXmlStore();
        store.createNew(xml, XSD);

        Task t1 = store.addTask("A").orElseThrow();
        Task t2 = store.addTask("B").orElseThrow();
        Task t3 = store.addTask("C").orElseThrow();

        assertEquals("0001", t1.getId());
        assertEquals("0002", t2.getId());
        assertEquals("0003", t3.getId());
    }

    @Test
    void load_rejectsPointsThatAreNotWholeOrHalfSteps() throws Exception {
        // Error case: semantic rule violation not covered by XSD (quarter points).
        Path xml = tempDir.resolve("invalid-points.xml");
        String invalidXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <tasks>
                  <task id="1" title="Invalid">
                    <subtask id="1" points="1.25" difficulty="easy" eligibility="both">
                      <variants group="g">
                        <variant id="1">
                          <text>a</text>
                          <solution>b</solution>
                        </variant>
                      </variants>
                    </subtask>
                  </task>
                </tasks>
                """;
        Files.writeString(xml, invalidXml);

        TaskXmlStore store = new TaskXmlStore();
        assertThrows(IllegalArgumentException.class, () -> store.load(xml, XSD));
    }

    @Test
    void load_sampleFile_parsesExistingRepositoryData() throws Exception {
        // Regression sample from repository resources.
        Path sampleXml = Path.of("src/main/resources/sample/tasks.xml");
        TaskXmlStore store = new TaskXmlStore();

        List<Task> tasks = store.load(sampleXml, XSD).tasks();
        assertEquals(1, tasks.size());
        assertEquals("0001", tasks.getFirst().getId());
        assertEquals(4, tasks.getFirst().getSubtasks().size());
    }

    @Test
    void load_rejectsDuplicateTaskIds_violatingXsdKeyConstraint() throws Exception {
        // Error case: duplicate task IDs violate xsd:key uniqueness.
        Path xml = tempDir.resolve("duplicate-task-id.xml");
        String invalidXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <tasks>
                  <task id="1" title="T1">
                    <subtask id="1" points="1" difficulty="easy" eligibility="both">
                      <variants>
                        <variant id="1">
                          <text>a</text>
                          <solution>b</solution>
                        </variant>
                      </variants>
                    </subtask>
                  </task>
                  <task id="1" title="T2">
                    <subtask id="1" points="1" difficulty="easy" eligibility="both">
                      <variants>
                        <variant id="1">
                          <text>c</text>
                          <solution>d</solution>
                        </variant>
                      </variants>
                    </subtask>
                  </task>
                </tasks>
                """;
        Files.writeString(xml, invalidXml);

        TaskXmlStore store = new TaskXmlStore();
        assertThrows(Exception.class, () -> store.load(xml, XSD));
    }

    @Test
    void createNew_withoutParentPath_handlesNullParentBranch() throws Exception {
        // Special case: relative file path has no parent component.
        Path relative = Path.of("tmp-klausurcraft-createNew-no-parent.xml");
        TaskXmlStore store = new TaskXmlStore();
        try {
            assertTrue(store.createNew(relative, XSD).tasks().isEmpty());
            assertTrue(Files.exists(relative));
        } finally {
            Files.deleteIfExists(relative);
        }
    }

    @Test
    void readAndUpdateSubtaskGroup_handleMissingVariantsAndNullGroup() throws Exception {
        // Covers missing variants node + null group normalization branch.
        Path xml = tempDir.resolve("group-branches.xml");
        TaskXmlStore store = new TaskXmlStore();
        store.createNew(xml, XSD);

        Task task = store.addTask("Group").orElseThrow();
        Subtask subtask = store.addSubtask(task).orElseThrow();

        Element subtaskElement = subtask.getDom();
        NodeList variantsBefore = subtaskElement.getElementsByTagName("variants");
        assertEquals(1, variantsBefore.getLength());
        subtaskElement.removeChild(variantsBefore.item(0));
        assertEquals("", store.readSubtaskGroup(subtask));

        store.updateSubtaskGroup(subtask, null);
        NodeList variantsAfter = subtaskElement.getElementsByTagName("variants");
        assertEquals(1, variantsAfter.getLength());
        assertEquals("", ((Element) variantsAfter.item(0)).getAttribute("group"));
    }

    @Test
    void updateVariant_createsTextAndSolutionNodesWhenMissing() throws Exception {
        // Covers updateVariant branches where text/solution elements are absent.
        Path xml = tempDir.resolve("variant-branches.xml");
        TaskXmlStore store = new TaskXmlStore();
        store.createNew(xml, XSD);

        Task task = store.addTask("Variant").orElseThrow();
        Subtask subtask = store.addSubtask(task).orElseThrow();
        Variant variant = subtask.getVariants().getFirst();
        Element variantElement = variant.getDom();

        removeChildren(variantElement, "text");
        removeChildren(variantElement, "solution");
        assertEquals(0, variantElement.getElementsByTagName("text").getLength());
        assertEquals(0, variantElement.getElementsByTagName("solution").getLength());

        variant.setText("Neue Frage");
        variant.setSolution(null);
        store.updateVariant(variant);

        assertEquals(1, variantElement.getElementsByTagName("text").getLength());
        assertEquals(1, variantElement.getElementsByTagName("solution").getLength());
        assertEquals("Neue Frage", ((Element) variantElement.getElementsByTagName("text").item(0)).getTextContent());
        assertEquals("", ((Element) variantElement.getElementsByTagName("solution").item(0)).getTextContent());
    }

    @Test
    void addAndDelete_methodsReturnGracefulFallbackForInvalidInput() throws Exception {
        // Error-path branches: public API should fail gracefully instead of throwing.
        Path xml = tempDir.resolve("error-paths.xml");
        TaskXmlStore store = new TaskXmlStore();
        store.createNew(xml, XSD);

        assertTrue(store.addSubtask(null).isEmpty());
        assertTrue(store.addVariant(null).isEmpty());
        assertFalse(store.deleteTask(null));
        assertFalse(store.deleteSubtask(null, null));
        assertFalse(store.deleteVariant(null, null));
    }

    @Test
    void addTask_ignoresNonNumericIdsWhenComputingNextId() throws Exception {
        // Covers NumberFormatException branch in nextId4 helper.
        Path xml = tempDir.resolve("non-numeric-id.xml");
        TaskXmlStore store = new TaskXmlStore();
        store.createNew(xml, XSD);

        Task first = store.addTask("A").orElseThrow();
        Document document = first.getDom().getOwnerDocument();
        Element malformed = document.createElement("task");
        malformed.setAttribute("id", "X-INVALID");
        malformed.setAttribute("title", "Broken");
        document.getDocumentElement().appendChild(malformed);

        Task second = store.addTask("B").orElseThrow();
        assertEquals("0002", second.getId());
    }

    @Test
    void addVariant_createsVariantsContainerWhenMissing() throws Exception {
        // Covers ensureVariants branch that must create a missing container node.
        Path xml = tempDir.resolve("ensure-variants.xml");
        TaskXmlStore store = new TaskXmlStore();
        store.createNew(xml, XSD);

        Task task = store.addTask("Ensure").orElseThrow();
        Subtask subtask = store.addSubtask(task).orElseThrow();
        Element subtaskElement = subtask.getDom();
        removeChildren(subtaskElement, "variants");

        Variant created = store.addVariant(subtask).orElseThrow();
        assertNotNull(created);
        assertEquals(1, subtaskElement.getElementsByTagName("variants").getLength());
    }

    @Test
    void parseTasks_supportsMissingTextAndSolutionNodes() throws Exception {
        // Covers parseTasks fallback branches where <text> / <solution> are missing.
        TaskXmlStore store = new TaskXmlStore();
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = document.createElement("tasks");
        document.appendChild(root);

        Element task = document.createElement("task");
        task.setAttribute("id", "0001");
        task.setAttribute("title", "Fallback");
        root.appendChild(task);

        Element subtask = document.createElement("subtask");
        subtask.setAttribute("id", "0001");
        subtask.setAttribute("points", "1");
        subtask.setAttribute("difficulty", "easy");
        subtask.setAttribute("eligibility", "both");
        task.appendChild(subtask);

        Element variants = document.createElement("variants");
        variants.setAttribute("group", "G");
        subtask.appendChild(variants);

        Element variant = document.createElement("variant");
        variant.setAttribute("id", "0001");
        variants.appendChild(variant);

        Method parseTasks = TaskXmlStore.class.getDeclaredMethod("parseTasks", Document.class);
        parseTasks.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Task> tasks = (List<Task>) parseTasks.invoke(store, document);

        assertEquals(1, tasks.size());
        Variant parsedVariant = tasks.getFirst().getSubtasks().getFirst().getVariants().getFirst();
        assertEquals("", parsedVariant.getText());
        assertEquals("", parsedVariant.getSolution());
    }

    @Test
    void save_handlesNullStateAndTransformerFailureWithoutThrowing() throws Exception {
        // Covers save() early-return branch and transformer-exception catch path.
        TaskXmlStore store = new TaskXmlStore();
        invokeSave(store);

        Path xml = tempDir.resolve("save-state.xml");
        store.createNew(xml, XSD);
        setField(store, "currentFile", tempDir); // directory path -> transform target is invalid

        assertDoesNotThrow(() -> invokeSave(store));
    }

    @Test
    void load_malformedXml_triggersFatalParserPath() throws Exception {
        // Error case: malformed XML should fail during parsing.
        Path xml = tempDir.resolve("malformed.xml");
        Files.writeString(xml, "<tasks><task id=\"1\" title=\"broken\"><subtask>");

        TaskXmlStore store = new TaskXmlStore();
        assertThrows(Exception.class, () -> store.load(xml, XSD));
    }

    private static void removeChildren(Element parent, String tagName) {
        while (true) {
            NodeList list = parent.getElementsByTagName(tagName);
            if (list.getLength() == 0) {
                return;
            }
            parent.removeChild(list.item(0));
        }
    }

    private static void invokeSave(TaskXmlStore store) throws Exception {
        Method save = TaskXmlStore.class.getDeclaredMethod("save");
        save.setAccessible(true);
        save.invoke(store);
    }

    private static void setField(TaskXmlStore store, String fieldName, Object value) throws Exception {
        Field field = TaskXmlStore.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(store, value);
    }
}
