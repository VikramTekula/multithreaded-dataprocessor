package main

import "sync"

// ResultCollector safely appends processed results from multiple goroutines.
type ResultCollector struct {
	mu      sync.Mutex
	results []string
}

func NewResultCollector() *ResultCollector {
	return &ResultCollector{results: make([]string, 0)}
}

func (rc *ResultCollector) Add(result string) {
	rc.mu.Lock()
	defer rc.mu.Unlock()
	rc.results = append(rc.results, result)
}

func (rc *ResultCollector) All() []string {
	rc.mu.Lock()
	defer rc.mu.Unlock()
	out := make([]string, len(rc.results))
	copy(out, rc.results)
	return out
}
