package info.frankl.dao;

import info.frankl.CreateChannelException;
import info.frankl.bots.KonvBot;
import info.frankl.model.Channel;
import info.frankl.model.User;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatDAO {

  public static final String BOTKEY = "konvbot";

  private final Jedis jedis;

  public ChatDAO(final String redisHost) {
    jedis = new Jedis(redisHost);

  }

  public User getUser(final Integer id) {
    User user = new User();
    user.setId(String.valueOf(id));
    return user;
  }

  public String getAndDeleteWaitFor(final String chatId, final KonvBot konvBot) {
    final String waitfor = jedis.get(getKeyWaitFor(chatId));
    jedis.del(getKeyWaitFor(chatId));
    return waitfor;
  }

  private String getKeyWaitFor(final String chatId) {
    return BOTKEY + ".chat." + chatId + ".waitfor";
  }

  public Channel createChannel(final User user, final String channelName) throws CreateChannelException {
    List<Channel> channelsForUser = getChannelsForUser(user);
    for (Channel presentChannel : channelsForUser) {
      if (presentChannel.getName().equals(channelName)) {
        throw new CreateChannelException("Channelname already present");
      }
    }
    Channel channel = Channel.create();
    channel.setName(channelName);

    persistChannel(user, channel);

    return channel;
  }

  public void persistChannel(final Channel channel) {
    String channelID = channel.getId().toString();
    HashMap<String, String> channelData = new HashMap<>();
    channelData.put("name", channel.getName());
    channelData.put("messagecount", String.valueOf(channel.getMessageCount()));
    jedis.hmset(BOTKEY + ".channel." + channelID, channelData);

    // liste loeschen
    jedis.ltrim(BOTKEY + ".channeltarget." + channelID, -1, 0);
    jedis.del(BOTKEY + ".channeltarget." + channelID);

    for (String target : channel.getTargetList()) {
      jedis.lpush(BOTKEY + ".channeltarget." + channelID, target);
    }
  }

  public void persistChannel(final User user, final Channel channel) {
    String channelID = channel.getId().toString();
    boolean present = false;
    for (Channel storedChannel : this.getChannelsForUser(user)) {
      if (storedChannel.getId().toString().equals(channelID)) {
        present = true;
      }
    }

    if (!present) {
      jedis.lpush(BOTKEY + ".user." + user.getId() + ".channellist", channelID);
    }
    persistChannel(channel);

  }

  public List<Channel> getChannelsForUser(final User user) {
    List<Channel> resultList = new ArrayList<>();
    List<String> channelIds = jedis.lrange(BOTKEY + ".user." + user.getId() + ".channellist", 0, 100);

    if (channelIds != null) {
      for (String channelId : channelIds) {
        Channel channel = getChannel(channelId);
        resultList.add(channel);
      }
    }

    return resultList;
  }

  public Channel getChannel(final String channelId) {
    Channel channel = new Channel();
    channel.setId(UUID.fromString(channelId));
    Map<String, String> stringStringMap = jedis.hgetAll(BOTKEY + ".channel." + channel.getId());
    channel.setName(stringStringMap.get("name"));
    String messagecount = stringStringMap.get("messagecount");

    Long messageCount;
    if (messagecount != null) {
      messageCount = new Long(messagecount);
    } else {
      messageCount = 0L;
    }

    channel.setMessageCount(messageCount);
    // TODO dynamisches Ende
    List<String> targetStrings = jedis.lrange(BOTKEY + ".channeltarget." + channelId, 0, 100);
    for (String targetString : targetStrings) {
      channel.addTarget(targetString);
    }

    return channel;
  }

  public void setWaitFor(final String chatId, final String channelname) {

    jedis.set(BOTKEY + ".chat." + chatId + ".waitfor", "channelname");
  }

  public void deleteChannel(final User user, final Channel channel) {

    jedis.lrem(BOTKEY + ".user." + user.getId() + ".channellist", 0, channel.getId().toString());

    String channelID = channel.getId().toString();
    String hashKey = BOTKEY + ".channel." + channelID;
    jedis.del(hashKey);

    // liste loeschen
    String channelTargetKey = BOTKEY + ".channeltarget." + channelID;
    jedis.ltrim(channelTargetKey, -1, 0);
    jedis.del(channelTargetKey);

  }
}
