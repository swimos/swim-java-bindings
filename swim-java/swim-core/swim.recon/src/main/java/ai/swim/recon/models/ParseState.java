package ai.swim.recon.models;

public enum ParseState {
  Init,
  AttrBodyStartOrNl,
  AttrBodyAfterValue,
  AttrBodyAfterSlot,
  AttrBodySlot,
  AttrBodyAfterSep,
  AfterAttr,
  RecordBodyStartOrNl,
  RecordBodyAfterValue,
  RecordBodyAfterSlot,
  RecordBodySlot,
  RecordBodyAfterSep,
}
