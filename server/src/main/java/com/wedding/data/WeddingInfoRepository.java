package com.wedding.data;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.model.WeddingInfo;

public interface WeddingInfoRepository extends JpaRepository<WeddingInfo, Integer>{
}
