package info.frankl.event;

public class MessageEvent {

  private String channel;

  private String message;

  public MessageEvent(final String channel, String message) {
    this.channel = channel;
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public String getChannel() {
    return channel;
  }
}
