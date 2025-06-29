# Parallel Mandelbrot Set Renderer (Java)

A multithreaded Swing application that computes and displays the Mandelbrot set, reaching almost **3 × speed‑up on four cores**. The project began with the single‑threaded *Mandel3.java* Applet by Rubikscube.info and was refactored into a modern desktop program that distributes the workload across a configurable thread pool.

---

## Table of Contents
1. [Getting Started](#getting-started)
2. [How to Run](#how-to-run)
3. [Usage & Keyboard Controls](#usage--keyboard-controls)
4. [Implementation Approach](#implementation-approach)
5. [Performance Results](#performance-results)
6. [Project Structure](#project-structure)
7. [References](#references)

---

## Getting Started

| Requirement | Version                                 |
|-------------|-----------------------------------------|
| **JDK**     | 11+ (tested with 21)                     |
| **Build tool** | *Optional* – plain `javac` works        |
| **OS** | Any OS with Java (tested on Windows 11) |

---

## How to Run

```bash
git clone https://github.com/gunakarchalla/Parallel-Mandelbrot-Set-Renderer.git
cd Parallel Mandelbrot Set Renderer/src           # source folder inside the repo
javac Main.java        # compile
java Main              # launch
```

At start‑up the program asks for the **number of threads** to use.  
Alternatively, open the project in IntelliJ IDEA or VS Code and run **Main**.

---

## Usage & Keyboard Controls

| Action | Mouse / Key |
|--------|-------------|
| Zoom into a region | **Left‑drag** |
| Pan (move view)    | **Shift + Left‑drag** |
| Reset view         | **Esc** |
| Zoom out      | **O** |
| Switch colour palette | **P**  |
| Toggle 5‑point antialias | **A** |
| Toggle smooth colouring | **S** |

The status bar at the bottom shows the render time for the most recent frame.

---

## Implementation Approach

1. **Applet ➜ Swing JFrame**  
   *Mandel3.java*’s `init()` was converted into a constructor; a `main()` method boots a standard desktop window.

2. **Task Decomposition**  
   The image is split into **4 × N** rectangular tiles (`N` = selected thread count). This finer granularity minimises load imbalance when zooming into detail‑heavy regions.

3. **ExecutorService Thread Pool**  
   A fixed `ExecutorService` holds **N worker threads**. Each tile becomes a `Callable`; workers pull tiles until the queue is empty, ensuring all cores stay busy.

4. **SwingWorker Bridge**  
   Because Swing painting must occur on the Event‑Dispatch Thread (EDT), each tile’s RGB data is first written into a shared `BufferedImage`. A final `SwingWorker` triggers `repaint()` once all tasks finish.

5. **Colouring & Post‑processing**  
   Five predefined palettes are linearly interpolated into 192 colours for smooth gradients. Optional antialiasing averages the five nearest samples per pixel, and smoothing blends adjacent iteration bands.

6. **Thread‑safe Counters & Shutdown**  
   An `AtomicInteger` tracks remaining tiles; when it hits 0, the render time is printed and the GUI updated. The pool stays alive so further zooms reuse the same threads, and is gracefully shut down on window close.

---

## Performance Results

| Threads | Avg. time (s) | Speed‑up |
|---------|---------------|----------|
| 1       | 0.288 | 1.0 × |
| 4       | 0.098 | **2.94 ×** |

Measured on an Intel i7‑1260P @ 2.1 GHz, 800 × 600 image, 192 iterations.  

---

## Project Structure

```
Parallel Mandelbrot Set Renderer/
├── src/                                   # ↳ Main.java  (source code)
├── out/production/                        # compiled .class files (IDE artefact)
├── .idea/                                 # IntelliJ project settings
├── README.md                              # Readme file
└── Parallel Mandelbrot Set Renderer.iml   # module file
```

Only **src/Main.java** is needed for compilation; other folders are optional IDE metadata.

---

## References

1. Original single‑threaded source: <https://web.archive.org/web/20221218100830/http://java.rubikscube.info/s0urce/Mandel3.java>
2. “Applet to Application” notes (TCU) – guidelines for refactoring AWT applets.
---

*Happy exploring the infinite beauty of the Mandelbrot set!*
