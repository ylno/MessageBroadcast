package info.frankl.bots;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import info.frankl.CreateChannelException;
import info.frankl.bots.service.Emoji;
import info.frankl.event.MessageEvent;
import info.frankl.model.Channel;
import info.frankl.service.DataService;

public class KonvBot extends TelegramLongPollingBot {

  private static final Logger logger = LoggerFactory.getLogger(KonvBot.class);

  public static final int MAX_TELEGRAM_MESSAGE_SIZE = 4096;
  private static final String VERSION = "1.1.1";

  private final String botKey;

  private final String botName;

  private final DataService dataService;

  public KonvBot(final EventBus eventBus, final String botKey, final String botName, final DataService dataService) {
    super();
    this.botKey = botKey;
    this.botName = botName;
    this.dataService = dataService;

    eventBus.register(MessageEvent.class);

  }

  @Override
  public void onUpdateReceived(final Update update) {
    logger.debug("update {}", update);

    try {

      if (update.getCallbackQuery() != null) {
        callbackQuery(update);

      } else {

        chatMessage(update);

      }

      //    message.enableMarkdown(true);

    }
    catch (TelegramApiException e) {
      logger.debug("send failed", e);
    }
    catch (IllegalMessageException e) {
      logger.debug("no message in update, skipping");
    }
    catch (Exception e) {
      logger.debug("Unknown exception", e);
    }
  }

  //test
  public void callbackQuery(final Update update) throws TelegramApiException {
    CallbackQuery callbackQuery = update.getCallbackQuery();
    String command = callbackQuery.getData();

    String[] splitted = command.split("/");
    String action = splitted[0];
    String target = splitted[1];

    logger.debug("data {} target {}", action, target);

    if (action.equals("ACTIVATECHANNEL")) {
      final AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
      answerCallbackQuery.setCallbackQueryId(callbackQuery.getId());

      answerCallbackQuery.setText("channel not found");
      info.frankl.model.User user = dataService.getChatDao().getUser(callbackQuery.getFrom().getId());
      logger.debug("user {}", user.getId());

      List<Channel> channels = dataService.getChatDao().getChannelsForUser(user);
      for (Channel channel : channels) {
        if (channel.getId().toString().equals(target)) {
          if (!channel.hasTarget(String.valueOf(callbackQuery.getMessage().getChat().getId()))) {
            channel.addTarget(String.valueOf(callbackQuery.getMessage().getChat().getId()));
            dataService.getChatDao().persistChannel(user, channel);
            answerCallbackQuery.setText("channel added");
          } else {
            channel.removeTarget(String.valueOf(callbackQuery.getMessage().getChat().getId()));
            dataService.getChatDao().persistChannel(user, channel);
            answerCallbackQuery.setText("channel removed!");
          }

          logger.debug("Target {} added to channel {}", callbackQuery.getMessage().getChat().getId(), channel.getId());
        }
      }
      answerCallbackQuery(answerCallbackQuery);
    } else if (action.equals("EDITCHANNEL")) {
      SendMessage sendMessage = new SendMessage();
      StringBuilder text = new StringBuilder("what do you want to do with channel ");
      Channel channel = dataService.getChatDao().getChannel(target);
      text.append(channel.getName());
      sendMessage.setText(text.toString());
      sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());

      InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
      List<List<InlineKeyboardButton>> rows = new ArrayList<>();
      List<InlineKeyboardButton> row = new ArrayList<>();
      rows.add(row);
      row.add(new InlineKeyboardButton().setText("Info").setCallbackData("INFOCHANNEL/" + target));
      row.add(new InlineKeyboardButton().setText("Activate").setCallbackData("ACTIVATECHANNEL/" + target));
      row.add(new InlineKeyboardButton().setText("Delete").setCallbackData("DELETECHANNEL/" + target));
      inlineKeyboardMarkup.setKeyboard(rows);

      sendMessage.setReplyMarkup(inlineKeyboardMarkup);
      sendMessage(sendMessage);
    } else if (action.equals("DELETECHANNEL")) {
      final AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
      answerCallbackQuery.setCallbackQueryId(callbackQuery.getId());

      info.frankl.model.User user = dataService.getChatDao().getUser(callbackQuery.getFrom().getId());

      Channel channel = dataService.getChatDao().getChannel(target);
      dataService.getChatDao().deleteChannel(user, channel);

      answerCallbackQuery.setText("Channel deleted");
      answerCallbackQuery(answerCallbackQuery);

    } else if (action.equals("INFOCHANNEL")) {
      // INfo
      info.frankl.model.User user = dataService.getChatDao().getUser(callbackQuery.getFrom().getId());

      Channel channel = dataService.getChatDao().getChannel(target);

      SendMessage sendMessage = new SendMessage();
      sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
      StringBuilder answer = new StringBuilder("Information about channel\n");

      answer.append(channel.getName()).append("\n");
      answer.append(" ID: ").append(channel.getId()).append("\n");
      answer.append(" name").append(": ").append(channel.getName()).append("\n");
      answer.append(" messages").append(": ").append(channel.getMessageCount()).append("\n");
      answer.append(" test channel").append(": ").append("https://message.frankl.info/test?channelid=").append(channel.getId()).append("\n");
      answer.append(" or use simple link to send receive a message").append(": ").append("https://message.frankl.info/message/").append(channel.getId()).append("/This%20is%20a%20example%20message%20to%20telegram").append("\n");
      sendMessage.setText(answer.toString());
      sendMessage(sendMessage);
    }

  }

  private void chatMessage(final Update update) throws IllegalMessageException {

    SendMessage message = new SendMessage();
    if (update.getMessage() == null) {
      throw new IllegalMessageException();
    }
    final Long chatIdLong = update.getMessage().getChatId();
    String chatId = String.valueOf(chatIdLong);
    message.setChatId(chatId);

    final User from = update.getMessage().getFrom();

    info.frankl.model.User user = dataService.getChatDao().getUser(from.getId());

    String text = update.getMessage().getText();
    String waitfor = dataService.getChatDao().getAndDeleteWaitFor(chatId, this);
    if (waitfor != null && waitfor.equals("channelname")) {

      String channelName = text;
      try {
        Channel channel = dataService.getChatDao().createChannel(user, channelName);
        logger.debug("chat: ", update.getMessage().getChatId());
        channel.addTarget(String.valueOf(update.getMessage().getChat().getId()));
        dataService.getChatDao().persistChannel(user, channel);
        message.setText("channel " + channel.getName() + " created and activated here");

      } catch (CreateChannelException e) {
        StringBuffer answer = new StringBuffer();
        answer.append(e.getMessage());
        answer.append("\nGive me a name for the channel");
        message.setText(answer.toString());
        dataService.getChatDao().setWaitFor(chatId, "channelname");
      }

    } else if (text.toLowerCase().equals("help") || text.equals("/start")) {

      StringBuilder helptext = new StringBuilder();
      helptext.append("Use this bot to receive telegram messages from anywhere. Receive your server-monitoring messages in telegram, filled out web-forms etc. All you need is to send a post message.\n\n");
      helptext.append("A channel is a input channel for messages. You can have many channels, they are bound to your telegram user.\n\n");
      helptext.append("Activate a channel in any chat to receive messages. The @KonvBot must be included in this channel. One channel can be activated in many chats. Messages to this channel will be broadcasted to every chat it is activated in.\n\n");
      helptext.append("Messages to a channel can be send with a post-message from anywhere. Structure of the postmessage: '{\"target\": \"channel-id\",\"message\": \"{your message}\"}'\n\n");
      helptext.append("channel-id: the channel-id, get it from your channel-list\n");
      helptext.append("{your message}: send the text that should be send to telegram.\n\n");
      helptext.append(
          "curl-example:\ncurl -H \"Content-Type: application/json\" -X POST -d '{\"target\": \"9288ec3b-c32c-482d-b9a1-06b08df9aaba\",\"message\": \"This is a telegram message\"}' https://message.frankl.info/message\n\n");
      helptext.append("Version: " + VERSION);

      message.setReplyMarkup(getMainMenuKeyboard());
      message.setText(helptext.toString());

    } else if (text.equals("LIST")) {

      List<Channel> channelList = dataService.getChatDao().getChannelsForUser(user);

      StringBuilder answer = new StringBuilder();
      answer.append("Channellist for " + user.getId() + "\n");

      for (Channel channel : channelList) {
        answer.append(channel.getName()).append("\n");
        answer.append(" ID: ").append(channel.getId()).append("\n");
        answer.append(" name").append(": ").append(channel.getName()).append("\n");
        answer.append(" messages").append(": ").append(channel.getMessageCount()).append("\n");
        answer.append(" test channel").append(": ").append("https://message.frankl.info/test?channelid=").append(channel.getId()).append("\n");
        answer.append(" or use simple link to send receive a message").append(": ").append("https://message.frankl.info/message/").append(channel.getId()).append("/This%20is%20a%20example%20message%20to%20telegram").append("\n");

      }

      message.setText(answer.toString());

      message.setReplyMarkup(getMainMenuKeyboard());

    } else if (text.toLowerCase().equals("new channel")) {
      message.setText("Give me a name for the channel");
      dataService.getChatDao().setWaitFor(chatId, "channelname");

    } else if (text.equals("Channels")) {
      message.setText("Choose channel to edit");
      InlineKeyboardMarkup inlineKeyboardMarkup = getChannellistKeyboard(chatId, user, "EDITCHANNEL");
      message.setReplyMarkup(inlineKeyboardMarkup);

    } else if (text.equals("ACTIVATE")) {
      InlineKeyboardMarkup inlineKeyboardMarkup = getChannellistKeyboard(chatId, user, "ACTIVATECHANNEL");
      message.setReplyMarkup(inlineKeyboardMarkup);
      message.setText("Choose channel to activate/deactivate in actual chat. Activated chats have an x-sign.");

    } else if (text.toLowerCase().equals("/stats")) {
      StringBuilder answer = new StringBuilder("messages total: " + dataService.getMessageCount());
      message.setText(answer.toString());

    } else {
      message.setText("I did not understand that. Try HELP");
      message.setReplyMarkup(getMainMenuKeyboard());

    }

    try {
      sendMessage(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private InlineKeyboardMarkup getChannellistKeyboard(final String chatId, final info.frankl.model.User user, String action) {
    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
    List<InlineKeyboardButton> row = new ArrayList<>();

    int counter = 0;
    for (Channel channel : dataService.getChatDao().getChannelsForUser(user)) {
      String emoji;
      if (channel.hasTarget(chatId)) {
        emoji = Emoji.CROSS_MARK.toString();
      } else {
        emoji = "";
      }

      row.add(new InlineKeyboardButton().setText(channel.getName() + " " + emoji).setCallbackData(action + "/" + channel.getId().toString()));
      counter++;
      if (counter == 4) {
        counter = 0;
        rows.add(row);
        row = new ArrayList<>();
      }
    }

    if (counter != 0) {
      rows.add(row);
    }

    // Add it to the message
    inlineKeyboardMarkup.setKeyboard(rows);
    return inlineKeyboardMarkup;
  }

  @Override
  public String getBotUsername() {
    return botName;
  }

  @Override
  public String getBotToken() {
    return botKey;
  }

  private static ReplyKeyboardMarkup getMainMenuKeyboard() {
    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
    replyKeyboardMarkup.setSelective(true);
    replyKeyboardMarkup.setResizeKeyboard(true);
    replyKeyboardMarkup.setOneTimeKeyboad(false);

    List<KeyboardRow> keyboard = new ArrayList<>();
    KeyboardRow keyboardFirstRow = new KeyboardRow();
    keyboardFirstRow.add("Help");
    keyboardFirstRow.add("Channels");
    keyboardFirstRow.add("New Channel");
    keyboard.add(keyboardFirstRow);
    replyKeyboardMarkup.setKeyboard(keyboard);

    return replyKeyboardMarkup;
  }

  @Subscribe
  public void messageHandler(MessageEvent messageEvent) {
    logger.debug("received messageEvent {}", messageEvent);

    try {

      // find chat
      Channel channel = dataService.getChatDao().getChannel(messageEvent.getChannel());

      for (String target : channel.getTargetList()) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Channel ").append(channel.getName()).append("\n");

        String message = messageEvent.getMessage();
        List<String> messages = splitStringIntoSmallMessages(message);

        for (String part : messages) {
          SendMessage sendMessage = new SendMessage();
          sendMessage.setChatId(target);
          sendMessage.setText(part);
          sendMessage(sendMessage);
        }


      }
      channel.increaseMessageCount();
      dataService.getChatDao().persistChannel(channel);

      dataService.increaseMessageCount();

    } catch (TelegramApiException e) {
      logger.error("Telegram-error", e);
    } catch (Exception e) {
      logger.error("Exception", e);
    }
  }

  protected List<String> splitStringIntoSmallMessages(final String message) {
    return splitStringIntoParts(message, MAX_TELEGRAM_MESSAGE_SIZE);
  }

  protected List<String> splitStringIntoParts(String string, int length) {
    ArrayList<String> strings = new ArrayList<>();
    int beginIndex = 0;
    while (beginIndex < string.length()) {
      int rest = string.length() - beginIndex;
      int nextLength = rest > length ? length : rest;
      int endIndex = beginIndex + nextLength;
      strings.add(string.substring(beginIndex, endIndex));
      beginIndex += nextLength;
    }
    return strings;
  }

}
