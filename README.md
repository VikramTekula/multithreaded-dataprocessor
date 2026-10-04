# Multi-threaded Data Processing System

A concurrent ride-sharing task processor implemented in **Java** and **Go**.
Both versions simulate multiple worker threads/goroutines pulling tasks from
a shared queue, processing them in parallel, and writing results to a shared
output file.

---

## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Java Implementation](#java-implementation)
  - [Architecture](#java-architecture)
  - [How to Compile and Run](#how-to-compile-and-run-java)
  - [Sample Output](#java-sample-output)
- [Go Implementation](#go-implementation)
  - [Architecture](#go-architecture)
  - [How to Build and Run](#how-to-build-and-run-go)
  - [Sample Output](#go-sample-output)
- [Concurrency Comparison](#concurrency-comparison)
- [Error Handling Summary](#error-handling-summary)
- [Testing](#testing)
- [Output Files](#output-files)
- [Author](#author)

---

## Overview

This project implements a producer–consumer system that processes simulated
ride-sharing requests using concurrent workers. The system demonstrates:

- A bounded, thread-safe shared queue
- Multiple workers retrieving tasks concurrently
- Simulated processing delay
- Thread-safe writing to a shared results file
- Graceful shutdown of all workers
- Robust exception/error handling and logging

Two independent implementations are provided:

| Language | Concurrency Model | Queue Mechanism |
|----------|-------------------|-----------------|
| Java     | Shared-memory with locks | `ReentrantLock` + `Condition` |
| Go       | Communicating Sequential Processes (CSP) | Buffered channels |

Both produce identical workloads and outcomes.

---

## Project Structure

```
multithreaded-dataprocessor/
├── README.md
├── java/
│   ├── Task.java                  # Immutable task record
│   ├── TaskQueue.java             # Bounded thread-safe queue
│   ├── ResultWriter.java          # Thread-safe file writer
│   ├── Worker.java                # Worker runnable
│   └── DataProcessingSystem.java  # Main entry point
└── go/
    ├── go.mod                     # Go module file
    ├── main.go                    # Entry point + producer + collector
    ├── task.go                    # Task struct
    ├── queue.go                   # Thread-safe ResultCollector
    └── worker.go                  # Worker goroutine
```

---

## Requirements

- **Java 11 or newer** (`javac` and `java` on PATH)
- **Go 1.21 or newer**
- Any terminal: Linux, macOS, WSL, or Git Bash

Verify installations:

```bash
java -version
javac -version
go version
```

---

## Java Implementation

### Java Architecture

- `TaskQueue` — bounded queue using `ReentrantLock` and two `Condition`
  objects (`notEmpty`, `notFull`). Producers block when full; consumers
  block when empty. A `finished` flag signals end-of-stream.
- `Worker` — implements `Runnable`. Pulls tasks, processes them with a
  random 200–500 ms delay, writes result, logs events. Catches
  `InterruptedException`, `IOException`, and generic `Exception`.
- `ResultWriter` — serializes file writes with a `synchronized` block.
- `DataProcessingSystem` — starts a producer thread, submits workers to a
  fixed thread pool via `Executors.newFixedThreadPool(4)`, waits for
  completion, and closes resources in a `finally` block.

### How to Compile and Run (Java)

```bash
cd multithreaded-dataprocessor/java
javac *.java
java DataProcessingSystem
```

View the generated results:

```bash
cat results_java.txt
```

### Java Sample Output

```
=== Multi-threaded Data Processing System (Java) ===
[Worker-1] STARTED
[Worker-2] STARTED
[Worker-3] STARTED
[Worker-4] STARTED
[Worker-2] processed Task{id=2, payload='ride_request_2'}
[Worker-4] processed Task{id=4, payload='ride_request_4'}
...
[Producer] All 20 tasks added.
...
[Worker-4] COMPLETED
=== All workers completed. Results in results_java.txt ===
System shutdown complete.
```

---

## Go Implementation

### Go Architecture

- `Task` — simple struct holding `ID` and `Payload`.
- `tasks` — buffered channel (capacity 10) used as the thread-safe queue.
- `results` — buffered channel receiving processed result strings.
- `worker()` — reads from `tasks` until the channel is closed, simulates
  work with `time.Sleep`, sends result to `results`. Uses `defer recover()`
  for panic protection.
- `ResultCollector` — mutex-protected slice of results.
- `main()` — starts workers with a `sync.WaitGroup`, launches the producer
  goroutine (closes `tasks` when done), and a closer goroutine (closes
  `results` after `wg.Wait()`). Writes to file via `writeResults` with
  deferred `Close()` and error wrapping.

### How to Build and Run (Go)

```bash
cd multithreaded-dataprocessor/go
go mod tidy
go run .
```

Or build a standalone binary:

```bash
go build -o dataproc .
./dataproc
```

View the generated results:

```bash
cat results_go.txt
```

### Go Sample Output

```
=== Multi-threaded Data Processing System (Go) ===
[Worker-2] STARTED
[Worker-3] STARTED
[Worker-4] STARTED
[Worker-1] STARTED
[Worker-1] processed task 4
[Worker-2] processed task 1
...
[Producer] All 20 tasks added.
...
[Worker-1] COMPLETED
=== All workers completed. Results in results_go.txt ===
```

---

## Concurrency Comparison

| Aspect | Java | Go |
|--------|------|-----|
| Primitive | Threads | Goroutines |
| Shared state | Explicit locks (`ReentrantLock`, `synchronized`) | Channels + `sync.Mutex` |
| Queue | Custom class with `Condition` | Buffered channel |
| Termination | Poison-pill (`null`) + `finished` flag | `close(tasks)` |
| Thread pool | `ExecutorService` | Runtime scheduler |
| Coordination | `ExecutorService.awaitTermination()` | `sync.WaitGroup` |
| Memory model | Shared heap | Share by communicating |

Java relies on the shared-memory model, requiring careful locking.
Go follows CSP, where channels make data flow explicit and reduce the
risk of forgotten unlocks or missed signals.

---

## Error Handling Summary

**Java**

- `try-catch-finally` in `Worker` and `DataProcessingSystem`.
- `InterruptedException` → restore interrupt flag.
- `IOException` → log and continue shutdown.
- Generic `Exception` → logged to stderr.
- Resources closed in `finally` blocks.

**Go**

- Functions return `error`; callers must check.
- `defer` used for guaranteed cleanup (`f.Close()`).
- Errors wrapped with `%w` for context.
- `defer recover()` inside workers catches panics.
- File is closed and flushed before program exits.

---

## Testing

Run the Java program and check:

```bash
java DataProcessingSystem
grep -c "Task-" results_java.txt      # should be 20
wc -l results_java.txt                 # should be 20
```

Run the Go program and check:

```bash
go run .
grep -c "Task-" results_go.txt        # should be 20
wc -l results_go.txt                   # should be 20
```

Expected outcomes:

- 20 processed entries in each file
- No duplicate task IDs
- All four workers log `STARTED` and `COMPLETED`
- Clean termination without deadlock

---

## Output Files

| File | Description |
|------|-------------|
| `java/results_java.txt` | Processed results from the Java run |
| `go/results_go.txt`     | Processed results from the Go run |

Each line has the form:

```
Worker-N | Task-M | Processed: RIDE_REQUEST_M
```
