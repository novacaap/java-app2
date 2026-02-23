package com.dockerforjavadevelopers.hello.controller;

import com.dockerforjavadevelopers.hello.model.Item;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/items")
@Tag(name = "Items", description = "Sample CRUD operations for items")
public class ItemController {

    private final List<Item> items = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public ItemController() {
        items.add(new Item(idGenerator.getAndIncrement(), "First Item", "A sample item"));
        items.add(new Item(idGenerator.getAndIncrement(), "Second Item", "Another sample"));
    }

    @GetMapping
    @Operation(summary = "List all items", description = "Returns all items")
    public ResponseEntity<List<Item>> list() {
        return ResponseEntity.ok(new ArrayList<>(items));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID", description = "Returns a single item by its ID")
    public ResponseEntity<Item> getById(
            @Parameter(description = "Item ID") @PathVariable Long id) {
        return items.stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create item", description = "Creates a new item")
    public ResponseEntity<Item> create(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "Unnamed");
        String description = body.getOrDefault("description", "");
        Item item = new Item(idGenerator.getAndIncrement(), name, description);
        items.add(item);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete item", description = "Deletes an item by ID")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Item ID") @PathVariable Long id) {
        boolean removed = items.removeIf(item -> item.id().equals(id));
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
