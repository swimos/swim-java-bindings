package ai.swim.structure;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.recon.ReconParser;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.result.ParseResult;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerProxy;

public class FormParser<T> extends Parser<T> {
  private Recognizer<T> recognizer;
  private ReconParser parser;

  public FormParser(Class<T> clazz) {
    this.recognizer = RecognizerProxy.getInstance().lookup(clazz);
  }

  public FormParser(Recognizer<T> recognizer) {
    this.recognizer = recognizer;
  }

  @Override
  public Parser<T> feed(Input input) {
    if (parser == null) {
      this.parser = new ReconParser().feed(input);
    }

    while (this.parser.hasEvents()) {
      ParseResult<ReadEvent> result = this.parser.next();
      if (result.isOk()) {
        ReadEvent readEvent = result.bind();
        System.out.println(readEvent);
        this.recognizer = this.recognizer.feedEvent(result.bind());
        if (recognizer.isDone()) {
          return Parser.done(recognizer.bind());
        } else if (recognizer.isError()) {
          return Parser.error(input, recognizer.trap().toString());
        }
      } else if (result.isError()) {
        throw new AssertionError();
      }

      if (this.parser.isError()) {
        throw new AssertionError();
      }
    }

    if (this.parser.isError()) {
      throw new AssertionError();
    }

    return this;
  }

  @Override
  public String toString() {
    return "FormParser{" +
        "recognizer=" + recognizer +
        ", parser=" + parser +
        '}';
  }
}
