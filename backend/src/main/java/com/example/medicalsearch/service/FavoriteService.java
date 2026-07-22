package com.example.medicalsearch.service;

import com.example.medicalsearch.entity.AppUser;
import com.example.medicalsearch.entity.UserFavorite;
import com.example.medicalsearch.repository.AppUserRepository;
import com.example.medicalsearch.repository.MedicalInstitutionRepository;
import com.example.medicalsearch.repository.UserFavoriteRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {

    private final UserFavoriteRepository favoriteRepository;
    private final AppUserRepository userRepository;
    private final MedicalInstitutionRepository institutionRepository;

    public FavoriteService(
            UserFavoriteRepository favoriteRepository,
            AppUserRepository userRepository,
            MedicalInstitutionRepository institutionRepository
    ) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
    }

    @Transactional(readOnly = true)
    public List<Long> getFavoriteInstitutionIds(String username) {
        AppUser user = findUser(username);
        return favoriteRepository.findInstitutionIdsByUserId(user.getId());
    }

    @Transactional
    public void addFavorite(String username, Long institutionId) {
        AppUser user = findUser(username);
        if (!institutionRepository.existsById(institutionId)) {
            throw new IllegalArgumentException("존재하지 않는 의료기관입니다.");
        }
        if (favoriteRepository.existsByUserIdAndInstitutionId(user.getId(), institutionId)) {
            return;
        }
        favoriteRepository.save(new UserFavorite(user.getId(), institutionId, LocalDateTime.now()));
    }

    @Transactional
    public void removeFavorite(String username, Long institutionId) {
        AppUser user = findUser(username);
        favoriteRepository.deleteByUserIdAndInstitutionId(user.getId(), institutionId);
    }

    private AppUser findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
