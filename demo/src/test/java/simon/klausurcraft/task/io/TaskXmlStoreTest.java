package simon.klausurcraft.task.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.task.Variant;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskXmlStoreTest {

    private static final Path XSD = Path.of("src/main/resources/simon/klausurcraft/exam-tasks.xsd");

    @TempDir
    Path tempDir;

    @Test
    void createNewAndCrud_roundtripPersistsExpectedState() throws Exception {
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
        TaskXmlStore store = new TaskXmlStore();
        assertTrue(store.addTask("No doc").isEmpty());
    }

    @Test
    void addTask_generatesSequentialFourDigitIds() throws Exception {
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
        Path sampleXml = Path.of("src/main/resources/sample/tasks.xml");
        TaskXmlStore store = new TaskXmlStore();

        List<Task> tasks = store.load(sampleXml, XSD).tasks();
        assertEquals(1, tasks.size());
        assertEquals("0001", tasks.getFirst().getId());
        assertEquals(4, tasks.getFirst().getSubtasks().size());
    }
}
