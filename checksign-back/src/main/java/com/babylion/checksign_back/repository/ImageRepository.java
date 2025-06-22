package com.babylion.checksign_back.repository;

import com.babylion.checksign_back.model.Image;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, String> {
    Optional<Image> findByFileHash(String fileHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Image i WHERE i.id = :id")
    Optional<Image> findWithLockById(@Param("id") String id);
} 