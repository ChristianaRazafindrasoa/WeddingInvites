package com.wedding.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.model.Photo;

public interface PhotoRepository extends JpaRepository<Photo, Integer>{
    List<Photo> findByIsApproved(boolean isApproved);
}
