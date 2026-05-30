package com.krisnaajiep.markdownnotetaking.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {
    boolean existsByOriginalFilename(String filename);
}
