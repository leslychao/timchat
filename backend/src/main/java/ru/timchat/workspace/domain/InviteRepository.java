package ru.timchat.workspace.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteRepository extends JpaRepository<Invite, UUID> {

  Optional<Invite> findByCode(String code);
}
