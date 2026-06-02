package com.krisnaajiep.markdownnotetaking.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {
    boolean existsByOriginalFilename(String filename);
    Optional<Note> findByOriginalFilename(String filename);
}
