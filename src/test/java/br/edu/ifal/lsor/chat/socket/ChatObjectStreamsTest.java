package br.edu.ifal.lsor.chat.socket;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ChatObjectStreamsTest {

  @Test
  void writesMutableObjectFreshStateAfterReset() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bytes);
    ArrayList<String> list = new ArrayList<>();
    list.add("first");

    ChatObjectStreams.writeAndReset(out, list);
    list.add("second");
    ChatObjectStreams.writeAndReset(out, list);

    out.close();

    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    @SuppressWarnings("unchecked")
    ArrayList<String> firstRead = (ArrayList<String>) in.readObject();
    @SuppressWarnings("unchecked")
    ArrayList<String> secondRead = (ArrayList<String>) in.readObject();

    assertEquals(1, firstRead.size());
    assertEquals("first", firstRead.get(0));
    assertEquals(2, secondRead.size());
    assertEquals("first", secondRead.get(0));
    assertEquals("second", secondRead.get(1));
  }
}
