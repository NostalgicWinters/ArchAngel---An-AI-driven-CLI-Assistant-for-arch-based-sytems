# ArchAngel--A-Unified-AI-Driven-Linux-System-Assistant-for-Arch-Based-Systems

<div align="center">
  <img src="ArchAngel.png" width="400">
</div>
---

## ArchAngel 

A Unified AI-Driven Linux System Assistant for Arch-Based Systems

ArchAngel is a local-first, privacy-centric Linux daemon designed for power users on Arch and Arch-based distributions.
It proactively audits system updates, explains low-level errors, and assists users safely in the terminal using an AI-powered multi-agent architecture.

## Why ArchAngel?

Arch Linux is powerful—but fragile if misused. ArchAngel solves three recurring pain points:

### 1. System Fragility

Explains kernel, driver, and system errors in plain English by auditing logs in real time.

### 2. Steep Learning Curve

Acts as a terminal copilot, generating safe, explainable commands instead of copy-paste disasters.

### 3. Rolling-Release Anxiety

Detects breaking updates before you upgrade by correlating:

- Installed packages

- Upcoming updates

- Official Arch News warnings

## Core Idea: Pre-Emptive Audit

ArchAngel doesn’t wait for your system to break.

Example:

```
“NVIDIA 550 is scheduled for update.
Arch News reports this version breaks KDE Plasma on some systems.
Recommendation: delay update or pin version.”
```

This proactive behavior is the project’s key innovation.

---
## Video Showcase


---
## Architecture Overview

ArchAngel follows a Multi-Agent System design.
Each component is lightweight, isolated, and communicates over local REST APIs.
```
+--------------------+
|  Textual TUI       |
|  (Python)          |
+---------+----------+
          |
          v
+--------------------+        +------------------------+
|  AI Brain          | <----> |  State Manager         |
|  Python + FastAPI  |  REST  |  Java (Quarkus/Spring) |
+---------+----------+        +-----------+------------+
          |                               |
          v                               v
     Ollama (Llama-3)              Arch News RSS
```

## Installation Instruction
### The CLI Tool
```
git clone https://github.com/Kernal-Penguins/Archangel-aur.git
cd Archangel-aur
makepkg -si
```

### The daemon service
```
git clone https://github.com/Kernal-Penguins/ArchAngel.git
chmod +x install-service.sh
./install-service.sh

```
### Dependent model
Install the qwen2.5:3b model from https://ollama.com/library/qwen2.5

---

 ## Components
### The Brain (Python)

Framework: FastAPI

LLM Backend: Ollama qwen2.5:3b

Responsibilities:

- Explain logs and errors

- Generate safe terminal commands

- Interface with the TUI

- Run local system inspections via subprocess

### The Orchestrator (Java)

Framework: Quarkus

Responsibilities:

- Scrape Arch Linux News RSS

- Detect breaking update advisories

- Maintain local database:

- - Command history

- - Ignored warnings

- Schedule periodic audits

### The Interface (Python)

Framework: Textual

Responsibilities:

- Display system health

- Show alerts and explanations

- Provide interactive terminal assistance

### Privacy & Security

- 100% local inference

- No telemetry

- No cloud APIs

- No data leaves the machine

- Runs as a system daemon

## License

GNU GPL v2

This project is free software.
You are free to use, study, modify, and distribute it—under the same license.

## Contributing

Contributions are welcome:

- Bug reports

- Performance improvements

- New agents (pacman hooks, GPU monitors, etc.)

Please open an issue or PR.
