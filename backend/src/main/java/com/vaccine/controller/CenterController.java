package com.vaccine.controller;

import com.vaccine.dto.CenterDTO;
import com.vaccine.service.CenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/centers")
public class CenterController {
    
    @Autowired
    private CenterService centerService;
    
    @GetMapping
    public ResponseEntity<List<CenterDTO>> getAllCenters() {
        return ResponseEntity.ok(centerService.getAllCenters());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CenterDTO> getCenterById(@PathVariable Long id) {
        return ResponseEntity.ok(centerService.getCenterById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CenterDTO> createCenter(@RequestBody CenterDTO centerDTO) {
        return ResponseEntity.ok(centerService.createCenter(centerDTO));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CenterDTO> updateCenter(@PathVariable Long id, @RequestBody CenterDTO centerDTO) {
        return ResponseEntity.ok(centerService.updateCenter(id, centerDTO));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCenter(@PathVariable Long id) {
        centerService.deleteCenter(id);
        return ResponseEntity.ok().build();
    }
}
