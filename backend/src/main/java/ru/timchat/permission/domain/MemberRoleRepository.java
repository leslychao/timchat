package ru.timchat.permission.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRoleRepository
    extends JpaRepository<MemberRole, UUID> {

  List<MemberRole> findByWorkspaceMemberId(UUID workspaceMemberId);

  Optional<MemberRole> findByWorkspaceMemberIdAndRoleId(
      UUID workspaceMemberId, UUID roleId);

  boolean existsByWorkspaceMemberIdAndRoleId(
      UUID workspaceMemberId, UUID roleId);
}
