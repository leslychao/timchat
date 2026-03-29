package ru.timchat.permission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "permission_grants")
public class PermissionGrant {

  @Id
  private UUID id;

  @Column(name = "role_id", nullable = false)
  private UUID roleId;

  @Enumerated(EnumType.STRING)
  @Column(name = "permission_type", nullable = false, length = 50)
  private PermissionType permissionType;

  protected PermissionGrant() {
  }

  public PermissionGrant(UUID roleId, PermissionType permissionType) {
    this.id = UUID.randomUUID();
    this.roleId = roleId;
    this.permissionType = permissionType;
  }
}
