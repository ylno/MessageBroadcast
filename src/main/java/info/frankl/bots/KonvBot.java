package info.frankl.bots;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import info.frankl.CreateChannelException;
import info.frankl.dao.ChatDAO;
import info.frankl.event.MessageEvent;
import info.frankl.model.Channel;
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

  private final ChatDAO chatDao;

  private EventBus eventBus;

  private final String botKey;

  private final String botName;

  public KonvBot(final EventBus eventBus, final String botKey, final String botName) {
    super();
    this.eventBus = eventBus;
    this.botKey = botKey;
    this.botName = botName;
    chatDao = new ChatDAO();

    eventBus.register(MessageEvent.class);

  }

  @Override
  public void onUpdateReceived(final Update update) {
    logger.debug("update {}", update);

    try {
      Long chatIdLong;

      CallbackQuery callbackQuery = update.getCallbackQuery();
      User from;
      if (callbackQuery != null) {
        String channelId = callbackQuery.getData();
        logger.debug("data {}", channelId);

        final AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(callbackQuery.getId());

        answerCallbackQuery.setText("channel not found");
        info.frankl.model.User user = chatDao.getUser(callbackQuery.getFrom().getId());
        logger.debug("user {}", user.getId());
        List<Channel> channels = chatDao.getChannelsForUser(user);
        for (Channel channel : channels) {
          if (channel.getId().toString().equals(channelId)) {
            channel.addTarget(String.valueOf(callbackQuery.getMessage().getChat().getId()));
            chatDao.persistChannel(user, channel);
            answerCallbackQuery.setText("channel added");
            logger.debug("Target {} added to channel {}", callbackQuery.getMessage().getChat().getId(), channel.getId());
          }
        }

        answerCallbackQuery.setText("ok");

        answerCallbackQuery(answerCallbackQuery);

      } else {
        SendMessage message = new SendMessage();
        chatIdLong = update.getMessage().getChatId();
        message.setChatId(chatIdLong);
        from = update.getMessage().getFrom();
        String chatId = String.valueOf(chatIdLong);
        message.setChatId(chatId);
        chatMessage(update, message, from, chatId);
        sendMessage(message);
      }

//    message.enableMarkdown(true);

    } catch (TelegramApiException e) {
      logger.debug("send failed", e);
    }
  }

  private void chatMessage(final Update update, final SendMessage message, final User from, final String chatId) {

    info.frankl.model.User user = chatDao.getUser(from.getId());

    String text = update.getMessage().getText();
    String waitfor = chatDao.getAndDeleteWaitFor(chatId, this);
    if (waitfor != null && waitfor.equals("channelname")) {

      String channelName = text;
      try {
        Channel channel = chatDao.createChannel(user, channelName);
        message.setText("channel " + channel.getName() + " created");

      } catch (CreateChannelException e) {
        StringBuffer answer = new StringBuffer();
        answer.append(e.getMessage());
        answer.append("\nGive me a name for the channel");
        message.setText(answer.toString());
        chatDao.setWaitFor(chatId, "channelname");

      }

    } else if (text.equals("HELP") || text.equals("/start")) {
      message.setText("HELP - List your channels");

    } else if (text.equals("LIST")) {

      List<Channel> channelList = chatDao.getChannelsForUser(user);

      StringBuilder answer = new StringBuilder();
      answer.append("Channellist for " + user.getId() + "\n");
      for (Channel channel : channelList) {
        answer.append(channel.getName()).append("\n");
        answer.append(" name").append(": ").append(channel.getName()).append("\n");
        answer.append(" messages").append(": ").append(channel.getMessageCount()).append("\n");
      }

      message.setText(answer.toString());

      message.setReplyMarkup(getMainMenuKeyboard());

    } else if (text.equals("CREATE CHANNEL")) {
      message.setText("Give me a name for the channel");
      chatDao.setWaitFor(chatId, "channelname");

    } else if (text.equals("ACTIVATE")) {
      InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
      List<List<InlineKeyboardButton>> rows = new ArrayList<>();
      List<InlineKeyboardButton> row = new ArrayList<>();

      for (Channel channel : chatDao.getChannelsForUser(user)) {
        row.add(new InlineKeyboardButton().setText(channel.getName()).setCallbackData(channel.getId().toString()));
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
      Channel channel = chatDao.getChannel(messageEvent.getChannel());

      for (String target : channel.getTargetList()) {
        SendMessage message = new SendMessage();
        message.setChatId(target);
        message.setText(messageEvent.getMessage());
        sendMessage(message);
        chatDao.persistChannel(channel);
      }
      channel.increaseMessageCount();

    } catch (TelegramApiException e) {
      logger.error("error", e);
    }
  }

}
