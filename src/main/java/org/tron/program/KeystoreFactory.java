package org.tron.program;

import com.beust.jcommander.JCommander;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Utils;
import org.tron.keystore.CipherException;
import org.tron.keystore.Credentials;
import org.tron.keystore.WalletUtils;

public class KeystoreFactory {

  private static final Logger logger = LoggerFactory.getLogger("KeystoreFactory");
  private static final String FilePath = "Wallet";
  public boolean passwordValid(String password) {
    if (StringUtils.isEmpty(password)) {
      logger.warn("Warning: Password is empty !!");
      return false;
    }
    if (password.length() < 6) {
      logger.warn("Warning: Password is too short !!");
      return false;
    }
    //Other rule;
    return true;
  }

  private String inputPassword(){
    Scanner in = new Scanner(System.in);
    while (true) {
      String input = in.nextLine().trim();
      String password = input.split("\\s+")[0];
      if (passwordValid(password)){
        return password;
      }
      System.out.println("Invalid password, please input again.");
    }
  }

  private void genKeystore() throws CipherException, IOException {
    String password0;
    while (true) {
      System.out.println("Please input password.");
      password0 = inputPassword();
      System.out.println("Please input password again.");
      String password1 = inputPassword();
      if (password0.equals(password1)){
        break;
      }
      System.out.println("The passwords do not match, please input again.");
    }

    ECKey eCkey = new ECKey(Utils.random);
    File file = new File(FilePath);
    if (!file.exists()) {
      file.mkdir();
    } else {
      if (!file.isDirectory()) {
        file.delete();
        file.mkdir();
      }
    }
    String fileName = WalletUtils.generateWalletFile(password0, eCkey, file, true);
    System.out.println("Gen a keystore its name " + fileName);
    Credentials credentials = WalletUtils.loadCredentials(password0, new File(file, fileName));
    System.out.println("Your address is " + credentials.getAddress());;
  }

  private void help() {
    System.out.println("You can enter the following command: ");
    System.out.println("GenKeystore");
    System.out.println("Exit or Quit");
    System.out.println("Input any one of then, you will get more tips.");
  }

  private void run() {
    Scanner in = new Scanner(System.in);
    while (true) {
      try {
        String cmdLine = in.nextLine().trim();
        String[] cmdArray = cmdLine.split("\\s+");
        // split on trim() string will always return at the minimum: [""]
        String cmd = cmdArray[0];
        if ("".equals(cmd)) {
          continue;
        }
        String[] parameters = Arrays.copyOfRange(cmdArray, 1, cmdArray.length);
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
          case "exit":
          case "quit": {
            System.out.println("Exit !!!");
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

  public static void main(String[] args) {
    KeystoreFactory cli = new KeystoreFactory();

    JCommander.newBuilder()
        .addObject(cli)
        .build()
        .parse(args);

    cli.run();
  }
}