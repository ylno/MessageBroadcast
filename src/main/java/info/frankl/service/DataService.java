package info.frankl.service;

import info.frankl.dao.ChatDAO;

public class DataService {

  private final ChatDAO chatDao;

  public DataService(final ChatDAO chatDao) {
    this.chatDao = chatDao;
  }

  public ChatDAO getChatDao() {
    return chatDao;
  }
}
