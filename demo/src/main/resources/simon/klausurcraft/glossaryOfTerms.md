# Glossary of Terms

### Application
A standalone desktop software built with **Java** and **JavaFX**, designed for local use by a single user. It is **open source** and **platform-independent**, requiring no internet connection or data transfer.

### Task
A high-level unit in the exam generation tool, representing a **topic** or **chapter**.
- Each Assignment can contain multiple **Subtasks**.
- Assignments can be created, edited, deleted, and ordered manually.
- Identified by a title and associated with one or more Subtasks.

### Subtask
A concrete question belonging to exactly one Task (topic).
Attributes:
- Text
- Points (integer > 0)
- Difficulty Level (easy, medium, hard)
- Scope: **Exam** or **Practice** or **Both**
- Optional **Solution**
- Optional **Variants**

### Variant
A specific alternative version of a Subtask.
- During exam generation, one variant is selected randomly.
- Multiple variants share the same Group Name.
- Each variant can have its own solution.

### Group Name (Variant Group)
A label used when a Subtask contains multiple **Variants**. The Variants are grouped under the same Group Name, and one is randomly chosen during exam generation.


### Difficulty Level
A classification of each Subtask by complexity:
- **Easy (green)**
- **Medium (orange)**
- **Hard (red)**

### Exam
A generated Exam with **Tasks** and **Subtasks** from the Scope **Exam** or **Both**

### Practice Exam
A generated Exam with **Tasks** and **Subtasks** from the Scope **Practice** or **Both**

### Sample Solution
A generated solution sheet corresponding to the generated exam. Includes all available solutions; missing ones remain blank.