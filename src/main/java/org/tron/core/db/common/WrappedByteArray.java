package org.tron.core.db.common;

import java.util.Arrays;
import lombok.Getter;

public final class WrappedByteArray {

  @Getter
  private byte[] bytes;

  public static WrappedByteArray of(byte[] bytes) {
    return new WrappedByteArray(bytes);
  }

  private WrappedByteArray(byte[] bytes) {
    if (bytes == null) {
      this.bytes = null;
    } else {
      this.bytes = Arrays.copyOf(bytes, bytes.length);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WrappedByteArray byteArray = (WrappedByteArray) o;
    return Arrays.equals(bytes, byteArray.bytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }
}
