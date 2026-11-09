package com.tecnocampus.LS2.protube_back.studio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface StudioVideos extends JpaRepository<StudioVideo, String> {
    List<StudioVideo> findByOwnerIdAndStatusNotOrderByCreatedAtDesc(Long ownerId, StudioVideo.Status status);
    List<StudioVideo> findByStatusOrderByCreatedAtDesc(StudioVideo.Status status);
    Optional<StudioVideo> findFirstByOwnerIdAndChecksumAndStatus(Long ownerId, String checksum, StudioVideo.Status status);
    Optional<StudioVideo> findFirstByStatusOrderByCreatedAtAsc(StudioVideo.Status status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from StudioVideo v where v.id = :id")
    Optional<StudioVideo> lock(@Param("id") String id);
    @Modifying
    @Query("update StudioVideo v set v.status = :next where v.status = :previous")
    void recover(@Param("previous") StudioVideo.Status previous, @Param("next") StudioVideo.Status next);
}
