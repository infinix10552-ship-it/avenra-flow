package org.devx.automatedinvoicesystem.Repository;

import org.devx.automatedinvoicesystem.Entity.PasswordResetToken;
import org.devx.automatedinvoicesystem.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepo  extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);

}