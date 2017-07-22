package info.frankl.bots;

import com.google.common.eventbus.EventBus;
import info.frankl.dao.ChatDAO;
import info.frankl.service.DataService;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

public class KonvBotTest {

  @Test
  public void splitStringIntoSmallMessages() throws Exception {

    KonvBot konvBot = getKonvBot();

    List<String> strings = konvBot.splitStringIntoSmallMessages("this is very short");
    assertThat(strings.size(), is(1));
    assertThat(strings.get(0), is("this is very short"));

    strings = konvBot.splitStringIntoSmallMessages(buildTestMessage(4096));
    assertThat(strings.size(), is(1));
    assertThat(strings.get(0), startsWith("aaaaaaa"));
    assertThat(strings.get(0), endsWith("aaaaaaa"));
    assertThat(strings.get(0).length(), is(4096));

  }

  @Test
  public void splitStringIntoSmallMessagesTwoMessages() throws Exception {

    KonvBot konvBot = getKonvBot();

    final List<String> strings = konvBot.splitStringIntoSmallMessages(buildTestMessage(4096) + "b");
    assertThat(strings.size(), is(2));
    assertThat(strings.get(0), startsWith("aaaaaaa"));
    assertThat(strings.get(0), endsWith("aaaaaaa"));
    assertThat(strings.get(1), endsWith("b"));

  }

  @Test
  public void splitStringIntoParts() throws Exception {
    KonvBot konvBot = getKonvBot();
    List<String> splitted = konvBot.splitStringIntoParts("abc", 3);
    assertThat(splitted.size(), is(1));
    assertThat(splitted.get(0), is("abc"));
  }

  @Test
  public void splitStringIntoParts2() throws Exception {
    KonvBot konvBot = getKonvBot();
    List<String> splitted = konvBot.splitStringIntoParts("abccba", 3);
    assertThat(splitted.size(), is(2));
    assertThat(splitted.get(0), is("abc"));
    assertThat(splitted.get(1), is("cba"));
  }

  @Test
  public void splitStringIntoParts3() throws Exception {
    KonvBot konvBot = getKonvBot();
    List<String> splitted = konvBot.splitStringIntoParts("abccbaabc", 3);
    assertThat(splitted.size(), is(3));
    assertThat(splitted.get(0), is("abc"));
    assertThat(splitted.get(1), is("cba"));
    assertThat(splitted.get(2), is("abc"));
  }

  @Test
  public void splitStringIntoParts3AndOne() throws Exception {
    KonvBot konvBot = getKonvBot();
    List<String> splitted = konvBot.splitStringIntoParts("abccbaabca", 3);
    assertThat(splitted.size(), is(4));
    assertThat(splitted.get(0), is("abc"));
    assertThat(splitted.get(1), is("cba"));
    assertThat(splitted.get(2), is("abc"));
    assertThat(splitted.get(3), is("a"));
  }

  public KonvBot getKonvBot() {
    return new KonvBot(new EventBus(), "", "", new DataService(new ChatDAO("")));
  }

  public String buildTestMessage(final int testlenght) {
    StringBuilder testString = new StringBuilder();
    for (int i = 0; i < testlenght; i++) {
      testString.append("a");
    }
    return testString.toString();
  }

}
