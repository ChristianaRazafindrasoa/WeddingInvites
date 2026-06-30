package com.wedding.data;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.model.Note;

public interface NoteRepository extends JpaRepository<Note, Integer> {
}
