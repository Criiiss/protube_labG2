package com.tecnocampus.LS2.protube_back.studio;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "studio_videos")
public class StudioVideo {
    @Id String id;
    @Column(nullable = false) Long ownerId;
    @Column(nullable = false) String filename;
    @Column(nullable = false, length = 64) String checksum;
    long totalBytes;
    long receivedBytes;
    @Enumerated(EnumType.STRING) @Column(nullable = false) Status status;
    @Column(nullable = false, columnDefinition = "TEXT") String metadata;
    @Column(nullable = false) Instant createdAt;
    Instant finishedAt;
    double duration;
    @Column(length = 500) String failure;
    public enum Status { UPLOADING, QUEUED, PROCESSING, READY, ERROR, CANCELLED }
}
