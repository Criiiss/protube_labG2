package com.tecnocampus.LS2.protube_back.auth;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface AuthTokens extends JpaRepository<AuthToken, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AuthToken t where t.tokenHash = :hash")
    Optional<AuthToken> lock(@Param("hash") String hash);
    void deleteByUserIdAndPurpose(Long userId, String purpose);
}
