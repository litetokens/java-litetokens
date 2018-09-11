package org.tron.program;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;

public class ParseAccount {

  public static HashMap<String, Long> accountBalance = new HashMap<>();

  public static void main(String[] args) throws IOException {
    AtomicLong index = new AtomicLong(0);
    Files.lines(Paths.get("/Users/huzhenyuan/Desktop/account_list.txt")).forEach(line -> {
      String[] parts = line.split(" ");
      accountBalance.put(parts[0], Long.parseLong(parts[1]));
      long i = index.incrementAndGet();
      if (i % 10000 == 0) {
        System.out.println("account_list:" + i);
      }
    });

    index.set(0);
    Files.lines(Paths.get("/Users/huzhenyuan/Desktop/28GBwordlist.txt")).parallel().forEach(line -> {
      ECKey ecKey = ECKey.fromPrivate(
          Sha256Hash.hash(ByteString.copyFrom(line, StandardCharsets.UTF_8).toByteArray()));
      String address = Wallet.encode58Check(ecKey.getAddress());
      if (accountBalance.get(address) != null) {
        System.out.println(address + " " + Hex.toHexString(ecKey.getPrivKeyBytes()) + " " + accountBalance.get(address));
      }
      long i = index.incrementAndGet();
      if (i % 1000 == 0) {
        System.out.println("word_list:" + i);
      }
    });
  }
}
