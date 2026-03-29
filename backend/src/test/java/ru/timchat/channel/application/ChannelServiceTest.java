package ru.timchat.channel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.timchat.channel.api.CreateChannelRequest;
import ru.timchat.channel.api.UpdateChannelRequest;
import ru.timchat.channel.domain.Channel;
import ru.timchat.channel.domain.ChannelRepository;
import ru.timchat.channel.domain.ChannelType;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.common.error.ValidationException;
import ru.timchat.permission.application.PermissionResolutionService;
import ru.timchat.permission.domain.PermissionType;
import ru.timchat.workspace.domain.WorkspaceRepository;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

  @Mock
  private ChannelRepository channelRepository;
  @Mock
  private WorkspaceRepository workspaceRepository;
  @Mock
  private PermissionResolutionService permissionResolutionService;

  @InjectMocks
  private ChannelService channelService;

  private static final UUID WORKSPACE_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  @Test
  void create_savesChannelWithCorrectType() {
    when(workspaceRepository.existsById(WORKSPACE_ID)).thenReturn(true);
    when(channelRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(0);
    when(channelRepository.save(any(Channel.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    var request = new CreateChannelRequest("general", ChannelType.TEXT);
    var response = channelService.create(WORKSPACE_ID, request);

    assertThat(response.name()).isEqualTo("general");
    assertThat(response.type()).isEqualTo(ChannelType.TEXT);
    assertThat(response.position()).isZero();
    assertThat(response.workspaceId()).isEqualTo(WORKSPACE_ID);

    var captor = ArgumentCaptor.forClass(Channel.class);
    verify(channelRepository).save(captor.capture());
    assertThat(captor.getValue().getType()).isEqualTo(ChannelType.TEXT);
  }

  @Test
  void create_voiceChannel_setsCorrectType() {
    when(workspaceRepository.existsById(WORKSPACE_ID)).thenReturn(true);
    when(channelRepository.countByWorkspaceId(WORKSPACE_ID)).thenReturn(2);
    when(channelRepository.save(any(Channel.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    var request = new CreateChannelRequest("voice-room", ChannelType.VOICE);
    var response = channelService.create(WORKSPACE_ID, request);

    assertThat(response.type()).isEqualTo(ChannelType.VOICE);
    assertThat(response.position()).isEqualTo(2);
  }

  @Test
  void create_workspaceNotFound_throwsNotFoundException() {
    when(workspaceRepository.existsById(WORKSPACE_ID)).thenReturn(false);

    var request = new CreateChannelRequest("test", ChannelType.TEXT);
    assertThatThrownBy(() -> channelService.create(WORKSPACE_ID, request))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void update_changesName() {
    var channel = new Channel(
        WORKSPACE_ID, "old-name", ChannelType.TEXT, 0);
    when(channelRepository.findById(channel.getId()))
        .thenReturn(Optional.of(channel));
    when(channelRepository.save(any(Channel.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    var request = new UpdateChannelRequest("new-name");
    var response = channelService.update(channel.getId(), request);

    assertThat(response.name()).isEqualTo("new-name");
    assertThat(response.type()).isEqualTo(ChannelType.TEXT);
  }

  @Test
  void update_notFound_throwsNotFoundException() {
    var id = UUID.randomUUID();
    when(channelRepository.findById(id)).thenReturn(Optional.empty());

    var request = new UpdateChannelRequest("name");
    assertThatThrownBy(() -> channelService.update(id, request))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void delete_removesChannel() {
    var channel = new Channel(
        WORKSPACE_ID, "to-delete", ChannelType.TEXT, 0);
    when(channelRepository.findById(channel.getId()))
        .thenReturn(Optional.of(channel));

    channelService.delete(channel.getId());

    verify(channelRepository).delete(channel);
  }

  @Test
  void listVisible_filtersByPermission() {
    when(workspaceRepository.existsById(WORKSPACE_ID)).thenReturn(true);

    var ch1 = new Channel(WORKSPACE_ID, "visible", ChannelType.TEXT, 0);
    var ch2 = new Channel(WORKSPACE_ID, "hidden", ChannelType.VOICE, 1);

    when(channelRepository.findByWorkspaceIdOrderByPositionAsc(
        WORKSPACE_ID))
        .thenReturn(List.of(ch1, ch2));

    when(permissionResolutionService.hasChannelPermission(
        USER_ID, WORKSPACE_ID, ch1.getId(),
        PermissionType.CHANNEL_VIEW))
        .thenReturn(true);
    when(permissionResolutionService.hasChannelPermission(
        USER_ID, WORKSPACE_ID, ch2.getId(),
        PermissionType.CHANNEL_VIEW))
        .thenReturn(false);

    var result = channelService.listVisible(WORKSPACE_ID, USER_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("visible");
  }

  @Test
  void reorder_updatesPositions() {
    when(workspaceRepository.existsById(WORKSPACE_ID)).thenReturn(true);

    var ch1 = new Channel(WORKSPACE_ID, "first", ChannelType.TEXT, 0);
    var ch2 = new Channel(WORKSPACE_ID, "second", ChannelType.TEXT, 1);

    when(channelRepository.findByWorkspaceIdOrderByPositionAsc(
        WORKSPACE_ID))
        .thenReturn(List.of(ch1, ch2))
        .thenReturn(List.of(ch2, ch1));
    when(channelRepository.saveAll(any(Iterable.class)))
        .thenReturn(List.of(ch2, ch1));

    channelService.reorder(
        WORKSPACE_ID, List.of(ch2.getId(), ch1.getId()));

    assertThat(ch2.getPosition()).isZero();
    assertThat(ch1.getPosition()).isEqualTo(1);
  }

  @Test
  void reorder_missingChannel_throwsValidation() {
    when(workspaceRepository.existsById(WORKSPACE_ID)).thenReturn(true);

    var ch1 = new Channel(WORKSPACE_ID, "only", ChannelType.TEXT, 0);
    when(channelRepository.findByWorkspaceIdOrderByPositionAsc(
        WORKSPACE_ID))
        .thenReturn(List.of(ch1));

    assertThatThrownBy(() -> channelService.reorder(
        WORKSPACE_ID, List.of(UUID.randomUUID())))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void reorder_incompleteList_throwsValidation() {
    when(workspaceRepository.existsById(WORKSPACE_ID)).thenReturn(true);

    var ch1 = new Channel(WORKSPACE_ID, "first", ChannelType.TEXT, 0);
    var ch2 = new Channel(WORKSPACE_ID, "second", ChannelType.TEXT, 1);
    when(channelRepository.findByWorkspaceIdOrderByPositionAsc(
        WORKSPACE_ID))
        .thenReturn(List.of(ch1, ch2));

    assertThatThrownBy(() -> channelService.reorder(
        WORKSPACE_ID, List.of(ch1.getId())))
        .isInstanceOf(ValidationException.class);
  }
}
