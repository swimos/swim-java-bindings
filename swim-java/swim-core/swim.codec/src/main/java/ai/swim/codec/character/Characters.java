package ai.swim.codec.character;

public class Characters {

  public static boolean tagNoCase(int[] next, int[] tag) {
    if (next.length != tag.length) {
      return false;
    }

    for (int i = 0; i < tag.length; i++) {
      int n = next[i];
      int t = tag[i];

      if (Character.toLowerCase(n) != Character.toLowerCase(t)) {
        return false;
      }
    }

    return true;
  }

}
