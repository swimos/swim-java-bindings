package ai.swim.recon;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.models.ParseState;
import ai.swim.recon.models.ParserTransition;
import ai.swim.recon.models.items.ItemsKind;
import ai.swim.recon.models.state.ModifyState;
import ai.swim.recon.models.state.PushAttrNewRec;
import ai.swim.recon.models.state.StateChange;
import ai.swim.recon.result.ParseResult;
import ai.swim.recon.result.ResultError;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import static ai.swim.codec.Parser.preceded;
import static ai.swim.codec.parsers.combinators.Alt.alt;
import static ai.swim.codec.parsers.text.Multispace0.multispace0;
import static ai.swim.codec.parsers.text.StringExt.space0;
import static ai.swim.recon.ReconParserParts.parseAfterAttr;
import static ai.swim.recon.ReconParserParts.parseAfterSlot;
import static ai.swim.recon.ReconParserParts.parseAfterValue;
import static ai.swim.recon.ReconParserParts.parseInit;
import static ai.swim.recon.ReconParserParts.parseNotAfterItem;
import static ai.swim.recon.ReconParserParts.parseSlotValue;

/**
 * An incremental recon parser.
 */
public final class ReconParser {

  private final Deque<ParseState> state;
  private Input input;
  private Parser<ParserTransition> current;
  private PendingEvents pending;
  private boolean complete;
  private boolean clearIfNone;

  public ReconParser() {
    this.state = new ArrayDeque<>(Collections.singleton(ParseState.Init));
    this.complete = false;
    this.clearIfNone = false;
  }

  private ReconParser(Input input, Deque<ParseState> state, Parser<ParserTransition> current, PendingEvents pending, boolean complete, boolean clearIfNone) {
    this.input = input;
    this.state = state;
    this.current = current;
    this.pending = pending;
    this.complete = complete;
    this.clearIfNone = clearIfNone;
  }

  private static Parser<ParserTransition> initParser() {
    return alt(
        new Parser<>() {
          @Override
          public Parser<ParserTransition> feed(Input input) {
            if (input.isDone()) {
              return Parser.done(ReadEvent.extant().transition());
            } else {
              return Parser.error(input, "Expected an empty input");
            }
          }
        },
        preceded(multispace0(), parseInit())
    );
  }

  /**
   * Returns whether the parser is in an error state.
   */
  public boolean isError() {
    if (this.complete) {
      return false;
    } else if (this.current == null) {
      return false;
    } else {
      return this.current.isError();
    }
  }

  /**
   * Returns the cause of the error if this parser is in an error state.
   *
   * @throws IllegalStateException if this parser is not in an error state
   */
  public ResultError<ReadEvent> error() {
    if (this.current != null && this.current.isError()) {
      ParserError<?> error = (ParserError<?>) this.current;
      return new ResultError<>(error.cause(), error.location());
    } else {
      throw new IllegalStateException("Parser is not in an error state");
    }
  }

  /**
   * Returns whether the parser is in a continuation state.
   */
  public boolean isCont() {
    if (this.complete) {
      return false;
    } else if (this.current == null) {
      if (this.input == null) {
        return true;
      } else {
        return this.input.isContinuation();
      }
    } else {
      return this.current.isCont();
    }
  }

  /**
   * Returns whether the input is done.
   */
  public boolean isDone() {
    if (input == null) {
      return false;
    } else {
      return input.isDone();
    }
  }

  /**
   * Return whether there are any pending events available.
   */
  public boolean hasEvents() {
    return !(this.complete && this.pending == null);
  }

  /**
   * Feeds this {@code ReconParser} more data to consume. This does not invoke any parsing operations and instead
   * returns a {@code ReconParser} that contains an extension of any remaining and unconsumed {@code Input} and the
   * provided {@code Input}.
   * <p>
   * The input must be available between multiple invocations of the {@code ReconParser#next} method and must not be
   * modified once it has been provided. Once an inner parser has decided on a parsing branch to take it will strip off
   * any consumed tokens from the {@code input}.
   */
  public ReconParser feed(Input input) {
    Objects.requireNonNull(input);

    if (this.complete) {
      throw new IllegalStateException("Cannot feed a completed parser more data");
    }

    Input newInput;
    if (this.input == null) {
      newInput = input;
    } else {
      newInput = this.input.extend(input);
    }

    return new ReconParser(newInput, this.state, this.current, this.pending, this.complete, this.clearIfNone);
  }

  /**
   * Incrementally parses as much data as possible from the provided {@code Input} and returns a result representing
   * the operation.
   *
   * @return a {@code ParseResult} in one of the following states:
   * - {@code ParseOk}: parsed successfully and an event was produced.
   * - {@code ParseContinuation}: not enough data is available to produce an event.
   * - {@code ParseError}: the {@code Input} is invalid.
   */
  public ParseResult<ReadEvent> next() {
    if (input == null) {
      throw new IllegalStateException("No input provided to recon parser");
    }

    // If there are any pending events then drain them before attempting to parse anything else
    ParseResult<ReadEvent> event = drainEvent();

    if (event != null) {
      return event;
    }

    if (this.complete) {
      return ParseResult.end();
    } else if (this.current != null) {
      // The current parser didn't complete in its last iteration, attempt to feed it more data.
      if (this.current.isError()) {
        ParserError<?> parserError = (ParserError<?>) this.current;
        return new ResultError<>(parserError.cause(), parserError.location());
      } else if (this.input.isContinuation()) {
        ParseResult<List<ReadEvent>> result = this.feed();
        if (result.isOk()) {
          List<ReadEvent> events = result.bind();
          if (!events.isEmpty()) {
            this.pending = new PendingEvents(events);
            return drainEvent();
          }
        } else {
          return result.cast();
        }

        if (!this.complete && this.input.isDone()) {
          return ParseResult.error("Not enough data", this.input.location());
        } else {
          return ParseResult.continuation();
        }
      }
    }

    if (this.state.peekLast() != null) {
      // Continue parsing tokens while the input has remaining tokens.
      while (true) {
        ParseResult<List<ReadEvent>> result = this.nextEvent();
        if (result.isOk()) {
          List<ReadEvent> events = result.bind();
          if (!events.isEmpty()) {
            this.pending = new PendingEvents(events);
            return drainEvent();
          }
        } else {
          return result.cast();
        }
      }
    }

    // At this point, the parsing completed successfully
    this.complete = true;
    return ParseResult.end();
  }

  private ParseResult<ReadEvent> drainEvent() {
    if (pending == null) {
      return null;
    }

    ReadEvent event = pending.next();
    if (pending.done()) {
      pending = null;
    }

    return ParseResult.ok(event);
  }

  private ParseResult<List<ReadEvent>> feed() {
    this.current = this.current.feed(this.input);

    if (this.current.isCont()) {
      return ParseResult.continuation();
    } else if (this.current.isError()) {
      ParserError<?> parserError = (ParserError<?>) this.current;
      return new ResultError<>(parserError.cause(), parserError.location());
    }

    ParserTransition output = this.current.bind();
    this.transition(output.getChange(), clearIfNone);
    this.current = null;
    return ParseResult.ok(output.getEvents());
  }

  private ParseResult<List<ReadEvent>> parseEvent(Parser<ParserTransition> parser, boolean clearIfNone) {
    if (this.current == null) {
      this.current = parser;
      this.clearIfNone = clearIfNone;
    }

    return this.feed();
  }

  private ParseResult<List<ReadEvent>> nextEvent() {
    switch (this.state.getLast()) {
      case Init:
        return parseEvent(initParser(), true);
      case AfterAttr:
        if (input.isDone()) {
          this.current = null;
          this.complete = true;
          return ParseResult.ok(List.of(ReadEvent.startBody(), ReadEvent.endRecord()));
        } else {
          return parseEvent(preceded(multispace0(), parseAfterAttr()), false);
        }
      case RecordBodyStartOrNl:
        return parseEvent(preceded(multispace0(), parseNotAfterItem(ItemsKind.record(), false)), false);
      case AttrBodyStartOrNl:
        return parseEvent(preceded(multispace0(), parseNotAfterItem(ItemsKind.attr(), false)), false);
      case RecordBodyAfterSep:
        return parseEvent(preceded(multispace0(), parseNotAfterItem(ItemsKind.record(), true)), false);
      case AttrBodyAfterSep:
        return parseEvent(preceded(multispace0(), parseNotAfterItem(ItemsKind.attr(), true)), false);
      case RecordBodyAfterValue:
        return parseEvent(preceded(space0(), parseAfterValue(ItemsKind.record())).map(s -> {
          if (s.isPresent()) {
            ParseState parseState = s.get();
            this.state.removeLast();
            this.state.addLast(parseState);

            if (parseState == ParseState.RecordBodySlot) {
              return new ParserTransition(ReadEvent.slot());
            } else {
              return new ParserTransition();
            }
          } else {
            this.transition(StateChange.popAfterItem(), false);
            return new ParserTransition(ReadEvent.endRecord());
          }
        }), false);
      case RecordBodyAfterSlot:
        return parseEvent(preceded(space0(), parseAfterSlot(ItemsKind.record())).map(s -> {
          if (s.isPresent()) {
            this.state.removeLast();
            this.state.addLast(s.get());
            return new ParserTransition();
          } else {
            this.transition(StateChange.popAfterItem(), false);
            return new ParserTransition(ReadEvent.endRecord());
          }
        }), false);
      case AttrBodyAfterSlot:
        return parseEvent(preceded(space0(), parseAfterSlot(ItemsKind.attr())).map(s -> {
          if (s.isPresent()) {
            this.state.removeLast();
            this.state.addLast(s.get());
            return new ParserTransition();
          } else {
            this.transition(StateChange.popAfterAttr(), false);
            return new ParserTransition(ReadEvent.endAttribute());
          }
        }), false);
      case AttrBodyAfterValue:
        return parseEvent(preceded(space0(), parseAfterValue(ItemsKind.attr())).map(s -> {
          if (s.isPresent()) {
            ParseState parseState = s.get();
            this.state.removeLast();
            this.state.addLast(parseState);

            if (parseState == ParseState.AttrBodySlot) {
              return new ParserTransition(ReadEvent.slot());
            } else {
              return new ParserTransition();
            }
          } else {
            this.transition(StateChange.popAfterAttr(), false);
            return new ParserTransition(ReadEvent.endAttribute());
          }
        }), false);
      case RecordBodySlot:
        return parseEvent(preceded(space0(), parseSlotValue(ItemsKind.record())), false);
      case AttrBodySlot:
        return parseEvent(preceded(space0(), parseSlotValue(ItemsKind.attr())), false);
      default:
        throw new AssertionError();
    }
  }

  private void transition(StateChange stateChange, boolean clearIfNone) {
    if (stateChange == null) {
      if (clearIfNone) {
        this.state.clear();
      }
      return;
    }

    if (stateChange.isNone()) {
      if (clearIfNone) {
        this.state.clear();
      }
    } else if (stateChange.isPopAfterAttr()) {
      this.state.pollLast();
      ParseState last = this.state.pollLast();
      if (last != null) {
        this.state.addLast(ParseState.AfterAttr);
      }
    } else if (stateChange.isPopAfterItem()) {
      this.state.pollLast();
      ParseState parseState = this.state.pollLast();
      if (parseState != null) {
        switch (parseState) {
          case Init:
            this.state.addLast(ParseState.AfterAttr);
            break;
          case AttrBodyStartOrNl:
          case AttrBodyAfterSep:
            this.state.addLast(ParseState.AttrBodyAfterValue);
            break;
          case AttrBodySlot:
            this.state.addLast(ParseState.AttrBodyAfterSlot);
            break;
          case RecordBodyStartOrNl:
          case RecordBodyAfterSep:
            this.state.addLast(ParseState.RecordBodyAfterValue);
            break;
          case RecordBodySlot:
            this.state.addLast(ParseState.RecordBodyAfterSlot);
            break;
          default:
            throw new IllegalStateException("Invalid state transition from: " + parseState + ", to: " + stateChange);
        }
      }
    } else if (stateChange.isChangeState()) {
      ParseState last = this.state.pollLast();
      if (last != null) {
        this.state.addLast(((ModifyState) stateChange).getState());
      }
    } else if (stateChange.isPushAttr()) {
      this.state.addLast(ParseState.AttrBodyStartOrNl);
    } else if (stateChange.isPushAttrNewRec()) {
      PushAttrNewRec pushAttr = (PushAttrNewRec) stateChange;
      if (pushAttr.hasBody()) {
        this.state.addLast(ParseState.Init);
        this.state.addLast(ParseState.AttrBodyStartOrNl);
      } else {
        this.state.addLast(ParseState.AfterAttr);
      }
    } else if (stateChange.isPushAttrNewRec()) {
      this.state.addLast(ParseState.AttrBodyStartOrNl);
    } else if (stateChange.isPushBody()) {
      this.state.addLast(ParseState.RecordBodyStartOrNl);
    } else {
      throw new AssertionError();
    }
  }

  @Override
  public String toString() {
    return "ReconParser{" +
        "input=" + input +
        ", state=" + state +
        ", current=" + current +
        ", pending=" + pending +
        ", complete=" + complete +
        ", clearIfNone=" + clearIfNone +
        '}';
  }

  private static class PendingEvents {
    private final List<ReadEvent> events;
    private int idx;

    private PendingEvents(List<ReadEvent> events) {
      this.events = events;
      this.idx = 0;
    }

    public boolean done() {
      return idx >= events.size();
    }

    public ReadEvent next() {
      if (done()) {
        return null;
      } else {
        ReadEvent event = events.get(idx);
        idx += 1;

        return event;
      }
    }
  }
}
