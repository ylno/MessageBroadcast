package info.frankl.bots;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import info.frankl.CreateChannelException;
import info.frankl.bots.service.Emoji;
import info.frankl.event.MessageEvent;
import info.frankl.model.Channel;
import info.frankl.service.DataService;
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

import java.util.ArrayList;
import java.util.List;

public class KonvBot extends TelegramLongPollingBot {

  private static final Logger logger = LoggerFactory.getLogger(KonvBot.class);


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

    } catch (TelegramApiException e) {
      logger.debug("send failed", e);
    }
  }

  public void callbackQuery(final Update update) throws TelegramApiException {
    CallbackQuery callbackQuery = update.getCallbackQuery();
    String channelId = callbackQuery.getData();
    logger.debug("data {}", channelId);

    final AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
    answerCallbackQuery.setCallbackQueryId(callbackQuery.getId());

    answerCallbackQuery.setText("channel not found");
    info.frankl.model.User user = dataService.getChatDao().getUser(callbackQuery.getFrom().getId());
    logger.debug("user {}", user.getId());
    List<Channel> channels = dataService.getChatDao().getChannelsForUser(user);
    for (Channel channel : channels) {
      if (channel.getId().toString().equals(channelId)) {
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
  }

  private void chatMessage(final Update update) {

    final Long chatIdLong = update.getMessage().getChatId();
    SendMessage message = new SendMessage();
    message.setChatId(chatIdLong);
    final User from = update.getMessage().getFrom();
    String chatId = String.valueOf(chatIdLong);
    message.setChatId(chatId);

    info.frankl.model.User user = dataService.getChatDao().getUser(from.getId());

    String text = update.getMessage().getText();
    String waitfor = dataService.getChatDao().getAndDeleteWaitFor(chatId, this);
    if (waitfor != null && waitfor.equals("channelname")) {

      String channelName = text;
      try {
        Channel channel = dataService.getChatDao().createChannel(user, channelName);
        message.setText("channel " + channel.getName() + " created");

      } catch (CreateChannelException e) {
        StringBuffer answer = new StringBuffer();
        answer.append(e.getMessage());
        answer.append("\nGive me a name for the channel");
        message.setText(answer.toString());
        dataService.getChatDao().setWaitFor(chatId, "channelname");

      }

    } else if (text.equals("HELP") || text.equals("/start")) {

      StringBuilder helptext = new StringBuilder();
      helptext.append("Use this bot to receive telegram messages from anywhere. Receive your server-monitoring messages in telegram, filled out web-forms etc. All you need is to send a post message.\n\n");
      helptext.append("A channel is a input channel for messages. You can have many channels, they are bound to your telegram user.\n\n");
      helptext.append("Activate a channel in any chat to receive messages. The @KonvBot must be included in this channel. One channel can be activated in many chats. Messages to this channel will be broadcasted to every chat it is activated in.\n\n");
      helptext.append("Messages to a channel can be send with a post-message from anywhere. Structure of the postmessage: '{\"target\": \"channel-id\",\"message\": \"{your message}\"}'\n\n");
      helptext.append("channel-id: the channel-id, get it from your channel-list\n");
      helptext.append("{your message}: send the text that should be send to telegram.\n\n");
      helptext.append("curl-example:\ncurl -H \"Content-Type: application/json\" -X POST -d '{\"target\": \"9288ec3b-c32c-482d-b9a1-06b08df9aaba\",\"message\": \"This is a telegram message\"}' https://message.frankl.info/message\n\n");
      helptext.append("Please rate the bot at: https://telegram.me/storebot?start=KonvBot");

      message.setText(helptext.toString());

    } else if (text.equals("LIST")) {

      List<Channel> channelList = dataService.getChatDao().getChannelsForUser(user);

      StringBuilder answer = new StringBuilder();
      answer.append("Channellist for " + user.getId() + "\n");

      for (Channel channel : channelList) {
        answer.append(channel.getName()).append("\n");
        answer.append(" ID: ").append(channel.getId()).append("\n");
        answer.append(" name").append(": ").append(channel.getName()).append("\n");
        answer.append(" messages").append(": ").append(channel.getMessageCount()).append("\n\n");
        answer.append(" send message to this channel eg").append(": ").append("curl -H \"Content-Type: application/json\" -X POST -d '{\"target\": \"" + channel.getId() + "\",\"message\": \"This is your telegram message\"}' https://message.frankl.info/message").append("\n\n");

      }

      message.setText(answer.toString());

      message.setReplyMarkup(getMainMenuKeyboard());

    } else if (text.equals("CREATE CHANNEL")) {
      message.setText("Give me a name for the channel");
      dataService.getChatDao().setWaitFor(chatId, "channelname");

    } else if (text.equals("ACTIVATE")) {
      InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
      List<List<InlineKeyboardButton>> rows = new ArrayList<>();
      List<InlineKeyboardButton> row = new ArrayList<>();

      for (Channel channel : dataService.getChatDao().getChannelsForUser(user)) {
        String emoji;
        if (channel.hasTarget(chatId)) {
          emoji = Emoji.CROSS_MARK.toString();
        } else {
          emoji = "";
        }
        row.add(new InlineKeyboardButton().setText(channel.getName() + " " + emoji).setCallbackData(channel.getId().toString()));
      }

      rows.add(row);

      // Add it to the message
      inlineKeyboardMarkup.setKeyboard(rows);
      message.setReplyMarkup(inlineKeyboardMarkup);
      message.setText("Choose channel to activate in actual chat");

    } else {
      message.setText("test");
      message.setReplyMarkup(getMainMenuKeyboard());

    }

    try {
      sendMessage(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
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
    keyboardFirstRow.add("HELP");
    keyboardFirstRow.add("LIST");
    keyboardFirstRow.add("CREATE CHANNEL");
    keyboardFirstRow.add("ACTIVATE");
//    KeyboardRow keyboardSecondRow = new KeyboardRow();
//    keyboardSecondRow.add(getSettingsCommand(language));
//    keyboardSecondRow.add(getRateCommand(language));
    keyboard.add(keyboardFirstRow);
//    keyboard.add(keyboardSecondRow);
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
        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(target);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Channel ").append(channel.getName()).append("\n");
        stringBuilder.append(messageEvent.getMessage());

        sendMessage.setText(stringBuilder.toString());
        sendMessage(sendMessage);

      }
      channel.increaseMessageCount();
      dataService.getChatDao().persistChannel(channel);

    } catch (TelegramApiException e) {
      logger.error("error", e);
    }
  }

}
