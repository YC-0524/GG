package com.group17.game.Models;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogGameRepository extends JpaRepository<CatalogGame, Long> {

    Optional<CatalogGame> findByGameNameIgnoreCase(String gameName);

    boolean existsByGameNameIgnoreCase(String gameName);

    Optional<CatalogGame> findByRawgId(Long rawgId);

    List<CatalogGame> findTop20ByGameNameContainingIgnoreCase(String query);
}
