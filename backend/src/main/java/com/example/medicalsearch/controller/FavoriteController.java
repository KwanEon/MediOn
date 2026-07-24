package com.example.medicalsearch.controller;

import com.example.medicalsearch.service.FavoriteService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    List<Long> getFavorites(Authentication authentication) {
        return favoriteService.getFavoriteInstitutionIds(authentication.getName());
    }

    @PutMapping("/{institutionId}")
    ResponseEntity<Void> addFavorite(
            Authentication authentication,
            @PathVariable Long institutionId
    ) {
        favoriteService.addFavorite(authentication.getName(), institutionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{institutionId}")
    ResponseEntity<Void> removeFavorite(
            Authentication authentication,
            @PathVariable Long institutionId
    ) {
        favoriteService.removeFavorite(authentication.getName(), institutionId);
        return ResponseEntity.noContent().build();
    }
}
