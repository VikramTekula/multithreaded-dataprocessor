package main

import (
	"bufio"
	"fmt"
	"math/rand"
	"os"
	"sync"
	"time"
)

const (
	numWorkers = 4
	numTasks   = 20
	queueSize  = 10
	outputFile = "results_go.txt"
)

func main() {
	rand.Seed(time.Now().UnixNano())
	fmt.Println("=== Multi-threaded Data Processing System (Go) ===")

	// Channel used as a concurrency-safe bounded queue.
	tasks := make(chan Task, queueSize)
	results := make(chan string, queueSize)

	var wg sync.WaitGroup
	wg.Add(numWorkers)

	// Start workers
	for i := 1; i <= numWorkers; i++ {
		go func(id int) {
			defer wg.Done()
			worker(id, tasks, results)
		}(i)
	}

	// Producer: enqueue tasks then close the channel.
	go func() {
		defer close(tasks)
		for i := 1; i <= numTasks; i++ {
			tasks <- Task{ID: i, Payload: fmt.Sprintf("ride_request_%d", i)}
		}
		fmt.Printf("[Producer] All %d tasks added.\n", numTasks)
	}()

	// Close results once every worker is done.
	go func() {
		wg.Wait()
		close(results)
	}()

	// Collector reads results until the channel is drained.
	collector := NewResultCollector()
	for r := range results {
		collector.Add(r)
	}

	// Write to file with proper error handling via defer.
	if err := writeResults(outputFile, collector.All()); err != nil {
		fmt.Fprintf(os.Stderr, "Error writing results: %v\n", err)
		os.Exit(1)
	}

	fmt.Printf("=== All workers completed. Results in %s ===\n", outputFile)
}

// writeResults writes lines to a file. Any error during write,
// flush, or close is returned to the caller (using a named return
// and a deferred close to guarantee cleanup).
func writeResults(filename string, lines []string) (err error) {
	f, err := os.Create(filename)
	if err != nil {
		return fmt.Errorf("create file: %w", err)
	}
	defer func() {
		if cerr := f.Close(); cerr != nil && err == nil {
			err = fmt.Errorf("close file: %w", cerr)
		}
	}()

	w := bufio.NewWriter(f)
	for _, line := range lines {
		if _, werr := w.WriteString(line + "\n"); werr != nil {
			return fmt.Errorf("write line: %w", werr)
		}
	}
	if ferr := w.Flush(); ferr != nil {
		return fmt.Errorf("flush buffer: %w", ferr)
	}
	return nil
}
