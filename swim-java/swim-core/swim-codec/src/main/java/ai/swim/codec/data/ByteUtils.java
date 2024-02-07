package ai.swim.codec.data;

class ByteUtils {
  private static final int ODD_PRIME = 31;

  public static int boundedHashcode(byte[] arr, int len) {
    int result = 1;

    for (int i = 0; i < len; i++) {
      result = ODD_PRIME * result + arr[i];
    }

    return result;
  }

  public static int accumulateHashcode(int acc, byte b) {
    return ODD_PRIME * acc + b;
  }

  public static int accumulateHashCode(int acc, byte[] arr, int len) {
    for (int i = 0; i < len; i++) {
      acc = ODD_PRIME * acc + arr[i];
    }
    return acc;
  }

  public static int checkedAdd(int a, int b) {
    int r = a + b;
    if (((a ^ r) & (b ^ r)) < 0) {
      throw new BufferOverflowException();
    } else {
      return r;
    }
  }

  public static String boundedToString(byte[] arr, int len) {
    if (len == 0) {
      return "[]";
    } else {
      // Chars: elem comma space = 3
      // +2 = '[' ']'
      int stringLength = (3 * len) + 2;
      StringBuilder builder = new StringBuilder(stringLength);
      builder.append('[');

      for (int i = 0; i < len; i++) {
        builder.append(arr[i]);
        if (i + 1 == len) {
          break;
        }

        builder.append(", ");
      }

      return builder.append(']').toString();
    }
  }
}
