package ru.timchat.permission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "member_roles")
public class MemberRole {

  @Id
  private UUID id;

  @Column(name = "workspace_member_id", nullable = false)
  private UUID workspaceMemberId;

  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  protected MemberRole() {
  }

  public MemberRole(UUID workspaceMemberId, UUID roleId) {
    this.id = UUID.randomUUID();
    this.workspaceMemberId = workspaceMemberId;
    this.roleId = roleId;
  }
}
