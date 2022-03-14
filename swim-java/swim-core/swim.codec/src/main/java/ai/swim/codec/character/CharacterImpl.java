package ai.swim.codec.character;

class CharacterImpl {

  public static boolean tagNoCase(char[] next, char[] tag) {
    if (next.length != tag.length) {
      return false;
    }

    for (int i = 0; i < tag.length; i++) {
      char n = next[i];
      char t = tag[i];

      if (Character.toLowerCase(n) != Character.toLowerCase(t)) {
        return false;
      }
    }

    return true;
  }

}
