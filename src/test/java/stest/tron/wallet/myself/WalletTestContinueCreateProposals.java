package stest.tron.wallet.myself;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.api.WalletGrpc;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class WalletTestContinueCreateProposals {

  private static final int WITNESS_COUNT = 27;
  private static final int PROPOSAL_COUNT = 19;
  private static final int APPROVAL_COUNT = 10;

  private static final String CONFIG_PATH = "testng.conf";
  private static final String WITNESS_KEY_PREFIX = "mainWitness.key";
  private static HashMap<String, byte[]> witnessAddessMap = new HashMap<>();
//  private static ArrayList<Long> approvalValueList = new ArrayList<>();
  private static long[] approvalValueList = new long[]{
      0,
      1,
      10,
      88,
      1024,
      81000,
      100000,
      86400000,
      100000000000000000L,
      999999999999999999L
  };
  static{
    for(int i = 1; i < WITNESS_COUNT+1; i++){
      String key = Configuration.getByPath(CONFIG_PATH)
          .getString(WITNESS_KEY_PREFIX + i);
      witnessAddessMap.put(key, PublicMethed.getFinalAddress(key));
    }
  }

  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private static final long now = System.currentTimeMillis();

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);


  private static int[] randomArray(int min, int max, int n){
    if (n > (max - min + 1) || max < min) {
      return null;
    }
    int[] result = new int[n];
    int count = 0;
    while(count < n) {
      int num = (int)Math.round((Math.random() * (max - min))) + min;
      boolean notFound = true;
      for (int j = 0; j < n; j++) {
        if(num == result[j]){
          notFound = false;
          break;
        }
      }
      if(notFound){
        result[count] = num;
        count++;
      }
    }
    return result;
  }

  private static int randomInt(int min, int max){
    return (int) Math.round(Math.random()*(max-min)+min);
  }

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
  }

  @Test(enabled = true)
  public void testApproveProposal() {
    for (int ii = 0; ii < 1000000000; ii++) {
      for (int i = 1; i <= PROPOSAL_COUNT; i++) {
        HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
        int[] approvalArray = randomArray(0, PROPOSAL_COUNT, i);
        for (int j = 0; j < approvalArray.length; j++) {
          long approvalId = approvalArray[j];
          long value = approvalValueList[randomInt(0, APPROVAL_COUNT-1)];
          logger.info("proposal id: " + approvalId + ", value: "+ value);
          // can't set it in mainnet
          if (approvalId == 10 && value == 1){
            logger.info("skip");
            continue;
          }
          proposalMap.put(approvalId, value);
        }
        int witnessId = randomInt(1, WITNESS_COUNT);
        String key = Configuration.getByPath(CONFIG_PATH)
            .getString(WITNESS_KEY_PREFIX + witnessId);
        boolean ret = PublicMethed
            .createProposal(witnessAddessMap.get(key), key, proposalMap, blockingStubFull);

        if (ret) {
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }

          ProposalList proposalList = blockingStubFull
              .listProposals(EmptyMessage.newBuilder().build());
          Optional<ProposalList> listProposals = Optional.ofNullable(proposalList);
          final Integer proposalId = listProposals.get().getProposalsCount();
          logger.info("The proposal id: " + Integer.toString(proposalId) + ", proposal map: " + listProposals.get().getProposals(0).getParametersMap());

          // approveProposal
          WitnessList witnesslist = blockingStubFull
              .listWitnesses(EmptyMessage.newBuilder().build());
          Optional<WitnessList> result = Optional.ofNullable(witnesslist);
          WitnessList witnessList = result.get();
          int approveCount = randomInt(witnessList.getWitnessesCount() * 2 / 3 - 3, WITNESS_COUNT);
          logger.info("The approve count willbe: " + approveCount);

          for (int k = 1; k <= approveCount; k++) {
            String newKey = Configuration.getByPath(CONFIG_PATH)
                .getString(WITNESS_KEY_PREFIX + k);
            PublicMethed.approveProposal(witnessAddessMap.get(newKey), newKey, proposalId, true,
                    blockingStubFull);
          }

          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }

          proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
          listProposals = Optional.ofNullable(proposalList);

          logger.info("The proposalID " + listProposals.get().getProposals(0).getProposalId()
              + ", approved count is: " + listProposals.get().getProposals(0).getApprovalsCount());
//          Assert.assertTrue(listProposals.get().getProposals(0).getApprovalsCount()== approveCount);
          java.util.Date d = new java.util.Date(
              listProposals.get().getProposals(0).getExpirationTime());
          logger.info(Long.toString(listProposals.get().getProposals(0).getExpirationTime()));
          logger.info("Expiration time is: " + d.toString());
        }else{
          logger.info("ret is False");
        }

        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      logger.info("for finish");

    }
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


