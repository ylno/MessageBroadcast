package info.frankl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Channel {

  private String name;

  private UUID id;

  private User user;

  private List<String> targetList = new ArrayList<>();

  private long messageCount;

  public List<String> getTargetList() {
    return targetList;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(final User user) {
    this.user = user;
  }

  public static Channel create() {
    UUID uuid = UUID.randomUUID();
    Channel channel = new Channel();
    channel.setId(uuid);
    return channel;
  }

  public void addTarget(final String target) {
    targetList.add(target);
  }

  public void increaseMessageCount() {
    this.messageCount++;
  }

  public long getMessageCount() {
    return messageCount;
  }

  public void setMessageCount(final long messageCount) {
    this.messageCount = messageCount;
  }
}
