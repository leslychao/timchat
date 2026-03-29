package ru.timchat.permission.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

  List<Role> findByWorkspaceId(UUID workspaceId);

  Optional<Role> findByWorkspaceIdAndName(UUID workspaceId, String name);
}
