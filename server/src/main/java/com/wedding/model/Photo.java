package com.wedding.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "photo")
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Integer id;
    @Column(name = "wedding_id")
    private Integer weddingId = 1;
    @Column(name = "s3_key")
    private String s3Key;
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    @Column(name = "uploaded_by")
    private String uploadedBy;
    @Column(name = "is_approved")
    private boolean isApproved;

    public Photo(){}

    public Photo(String s3Key, LocalDateTime uploadedAt, String uploadedBy, boolean isApproved) {
        this.s3Key = s3Key;
        this.uploadedAt = uploadedAt;
        this.uploadedBy = uploadedBy;
        this.isApproved = isApproved;
    }

    public int getId() {
        return id;
    }

    public String getS3Key() {
        return s3Key;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean isApproved) {
        this.isApproved = isApproved;
    }
}
