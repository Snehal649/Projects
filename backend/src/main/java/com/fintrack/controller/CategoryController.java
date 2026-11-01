package com.fintrack.controller;

import com.fintrack.common.ApiException;
import com.fintrack.dto.Dtos.CategoryRequest;
import com.fintrack.model.Category;
import com.fintrack.repo.CategoryRepository;
import com.fintrack.repo.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryRepository repo;
    private final UserRepository users;

    @GetMapping
    public List<Category> list(@AuthenticationPrincipal Long uid) { return repo.findByUserIdOrderByNameAsc(uid); }

    @PostMapping
    public Category create(@AuthenticationPrincipal Long uid, @Valid @RequestBody CategoryRequest r) {
        Category c = new Category();
        c.setUser(users.getReferenceById(uid));
        c.setName(r.name());
        c.setType(r.type());
        c.setIcon(r.icon());
        return repo.save(c);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal Long uid, @PathVariable Long id) {
        repo.delete(repo.findByIdAndUserId(id, uid).orElseThrow(() -> ApiException.notFound("Category")));
    }
}
