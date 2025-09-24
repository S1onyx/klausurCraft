# KlausurCraft

Local, open-source exam generator (Java + JavaFX) that assembles balanced exams from topic-based sub-tasks with difficulty mix and optional solution export.

---

## Features (v1)

- Standalone Java/JavaFX application (OS-independent)  
- Local & offline – no data transfer, fully privacy-friendly  
- Topic → sub-task model with continuous numbering (1a, 1b, …)  
- Difficulty categories: easy / medium / hard, approx. 1/3 mix (with tolerance)  
- Randomized selection of sub-tasks, with multiple text variants supported  
- Points per topic: dropdown with auto-combination suggestions  
- Generate Exam or Practice Exam and export as PDF (solution optional)  
- XML load/save of datasets (`Open`, `Save`, `Save As`)  
- Clean UI with hover tooltips for all buttons  

---

## Planned (v2)

- Confirm-before-save workflow & editable preview (box sizes)  
- Mandatory tasks, configurable topic order  
- Cover page & page numbers (e.g., 2/9)  
- Edit/override solution text directly in the UI  
- HTML-ready task content & multi-language support (EN/DE toggle)  
- Basic correction workflow & versioned improvements  

---

## Technical Overview

- Language/Framework: Java + JavaFX  
- File Format: XML for dataset persistence  
- Output: PDF exams (+ optional solutions)  
- Scope: Local tool for individual lecturers, no multi-user setup  
- License: Open Source  

---

## Getting Started

1. Clone the repository:

   ```bash
   git clone https://github.com/your-org/klausurcraft.git
   cd klausurcraft
   ```

2. Build with Maven/Gradle (depending on setup):

   ```bash
   mvn clean install
   ```

3. Run the application:

   ```bash
   java -jar target/klausurcraft.jar
   ```

4. Create your first dataset (topics + sub-tasks) and start generating exams.

---

## Dataset Structure (XML)

- Topic: corresponds to an exam task (Aufgabe)  
- Sub-task: belongs to exactly one topic, contains:  
  - Text (multiple variants possible)  
  - Points  
  - Difficulty (easy / medium / hard)  
  - Optional solution text  
- Variant group: one sub-task may provide several text variants → one chosen randomly during generation  

---

## Why KlausurCraft?

Balanced, reproducible exams from a transparent task pool:  
- Faster creation process  
- Transparent difficulty distribution  
- Easy to maintain and extend  
- Privacy-friendly (local storage only)

---

## License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.
