package ru.timchat.permission.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionGrantRepository
    extends JpaRepository<PermissionGrant, UUID> {

  List<PermissionGrant> findByRoleId(UUID roleId);

  List<PermissionGrant> findByRoleIdIn(List<UUID> roleIds);
}
