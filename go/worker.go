package main

import (
	"fmt"
	"math/rand"
	"strings"
	"time"
)

// worker reads tasks from the tasks channel, processes each one,
// and sends the result string on the results channel.
// Any panic is recovered and logged; the WaitGroup is decremented by the caller.
func worker(id int, tasks <-chan Task, results chan<- string) {
	defer func() {
		if r := recover(); r != nil {
			fmt.Printf("[Worker-%d] Recovered from panic: %v\n", id, r)
		}
	}()

	fmt.Printf("[Worker-%d] STARTED\n", id)

	for task := range tasks {
		// Simulate computational work (200-500 ms)
		time.Sleep(time.Duration(200+rand.Intn(300)) * time.Millisecond)

		// Transform the payload
		processed := fmt.Sprintf("Worker-%d | Task-%d | Processed: %s",
			id, task.ID, strings.ToUpper(task.Payload))

		results <- processed
		fmt.Printf("[Worker-%d] processed task %d\n", id, task.ID)
	}

	fmt.Printf("[Worker-%d] COMPLETED\n", id)
}
