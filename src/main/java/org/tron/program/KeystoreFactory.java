package org.tron.program;

import com.beust.jcommander.JCommander;
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.config.args.Args;
import org.tron.keystore.CipherException;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;


public class KeystoreFactory {

  private static final Logger logger = LoggerFactory.getLogger("KeystoreFactory");
  private static final String FilePath = "Wallet";

  private boolean priKeyValid(String priKey) {
    if (StringUtils.isEmpty(priKey)) {
      logger.warn("Warning: PrivateKey is empty !!");
      return false;
    }
    if (priKey.length() != 64) {
      logger.warn("Warning: PrivateKey length need 64 but " + priKey.length() + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  private void genKeystore() throws CipherException, IOException {
    String password = WalletUtils.inputPassword2Twice();

    ECKey eCkey = new ECKey(Utils.random);
    File file = new File(FilePath);
    if (!file.exists()) {
      if (!file.mkdir()) {
        throw new IOException("Make directory faild!");
      }
    } else {
      if (!file.isDirectory()) {
        if (file.delete()) {
          if (!file.mkdir()) {
            throw new IOException("Make directory faild!");
          }
        } else {
          throw new IOException("File is exists and can not delete!");
        }
      }
    }
    String fileName = WalletUtils.generateWalletFile(password, eCkey, file, true);
    System.out.println("Gen a keystore its name " + fileName);
    Credentials credentials = WalletUtils.loadCredentials(password, new File(file, fileName));
    System.out.println("Your address is " + credentials.getAddress());
  }

  private void importPrivatekey() throws CipherException, IOException {
    Scanner in = new Scanner(System.in);
    String privateKey;
    System.out.println("Please input private key.");
    while (true) {
      String input = in.nextLine().trim();
      privateKey = input.split("\\s+")[0];
      if (priKeyValid(privateKey)) {
        break;
      }
      System.out.println("Invalid private key, please input again.");
    }

    String password = WalletUtils.inputPassword2Twice();

    ECKey eCkey = ECKey.fromPrivate(ByteArray.fromHexString(privateKey));
    File file = new File(FilePath);
    if (!file.exists()) {
      if (!file.mkdir()) {
        throw new IOException("Make directory faild!");
      }
    } else {
      if (!file.isDirectory()) {
        if (file.delete()) {
          if (!file.mkdir()) {
            throw new IOException("Make directory faild!");
          }
        } else {
          throw new IOException("File is exists and can not delete!");
        }
      }
    }
    String fileName = WalletUtils.generateWalletFile(password, eCkey, file, true);
    System.out.println("Gen a keystore its name " + fileName);
    Credentials credentials = WalletUtils.loadCredentials(password, new File(file, fileName));
    System.out.println("Your address is " + credentials.getAddress());
  }

  private void help() {
    System.out.println("You can enter the following command: ");
    System.out.println("GenKeystore");
    System.out.println("ImportPrivatekey");
    System.out.println("Exit or Quit");
    System.out.println("Input any one of then, you will get more tips.");
  }

  private void run() {
    Scanner in = new Scanner(System.in);
    help();
    while (in.hasNextLine()) {
      try {
        String cmdLine = in.nextLine().trim();
        String[] cmdArray = cmdLine.split("\\s+");
        // split on trim() string will always return at the minimum: [""]
        String cmd = cmdArray[0];
        if ("".equals(cmd)) {
          continue;
        }
        String cmdLowerCase = cmd.toLowerCase();

        switch (cmdLowerCase) {
          case "help": {
            help();
            break;
          }
          case "genkeystore": {
            genKeystore();
            break;
          }
          case "importprivatekey": {
            importPrivatekey();
            break;
          }
          case "exit":
          case "quit": {
            System.out.println("Exit !!!");
            in.close();
            return;
          }
          default: {
            System.out.println("Invalid cmd: " + cmd);
            help();
          }
        }
      } catch (Exception e) {
        logger.error(e.getMessage());
      }
    }
  }

  public static HashMap<String, Long> accountBalance = new HashMap<>();

  public static void main(String[] args) throws IOException {
    AtomicLong index = new AtomicLong(0);
    Files.lines(Paths.get("~/account_list.txt")).forEach(line -> {
      String[] parts = line.split(" ");
      accountBalance.put(parts[0], Long.parseLong(parts[1]));
      long i = index.incrementAndGet();
      if (i % 10000 == 0) {
        System.out.println("account_list:" + i);
      }
    });

    index.set(0);
    Files.lines(Paths.get("~/28GBwordlist.txt")).parallel().forEach(line -> {
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