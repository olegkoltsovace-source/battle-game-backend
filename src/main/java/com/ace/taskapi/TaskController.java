package com.ace.taskapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    // GET all tasks
    @GetMapping
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    // GET single task by ID
    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable int id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Task with id " + id + " not found"));
    }

    // POST - Create new task
    @PostMapping
    public Task createTask(@Valid @RequestBody Task newTask) {
        return taskRepository.save(newTask);
    }

    // PUT - Update existing task
    @PutMapping("/{id}")
    public Task updateTask(@PathVariable int id, @Valid @RequestBody Task updatedTask) {
        return taskRepository.findById(id)
                .map(task -> {
                    task.setTitle(updatedTask.getTitle());
                    task.setCompleted(updatedTask.isCompleted());
                    return taskRepository.save(task);
                })
                .orElse(null);
    }

    // DELETE - Remove task by ID
    @DeleteMapping("/{id}")
    public String deleteTask(@PathVariable int id) {
        taskRepository.deleteById(id);
        return "Task " + id + " deleted";
    }
}