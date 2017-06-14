package info.frankl.model;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ChannelTest {

  @Test
  public void hasTarget() throws Exception {
    Channel channel = new Channel();
    assertThat(channel.hasTarget("123"), is(false));
    channel.addTarget("123");
    assertThat(channel.hasTarget("123"), is(true));
  }

  @Test
  public void addTarget() throws Exception {
    Channel channel = new Channel();
    channel.addTarget("123");
    channel.addTarget("123");
    assertThat(channel.getTargetList().size(), is(1));

  }

}
