package ai.swim.recon;

import ai.swim.structure.form.Parser;
import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.recognizer.Recognizer;
import org.junit.jupiter.api.Test;
import swim.codec.Binary;

class ReconParserTest {

  @Test
  void t() {
    Recognizer<Envelope> recognizer = new EnvelopeRecognizer();


  }

  interface DidRead {

    void didRead(Envelope envelope);

  }

  static class ValueLane {

    DidRead didRead;
    Recognizer<Envelope> recognizer;
    Parser parser;

    void didRead(byte[] bytes) {
      this.parser = parser.feed(Binary.input(bytes));

      if (this.parser.isDone()) {
        this.recognizer = this.recognizer.feedEvent(this.parser.bind());

        if (this.recognizer.isDone()) {
          this.didRead.didRead(this.recognizer.bind());
        } else if (this.recognizer.isError()) {
          throw this.recognizer.trap();
        }
      } else if (this.parser.isError()) {
        throw this.parser.trap();
      }
    }

  }

  static abstract class Envelope {

    public boolean isEvent() {
      return false;
    }

  }

  static class EnvelopeRecognizer extends Recognizer<Envelope> {

    @Override
    public Recognizer<Envelope> feedEvent(ReadEvent event) {
      return null;
    }

    @Override
    public Envelope bind() {
      return null;
    }

  }

}