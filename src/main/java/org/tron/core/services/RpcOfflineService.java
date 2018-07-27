package org.tron.core.services;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.api.GrpcAPI.AddressPrKeyPairMessage;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc.WalletImplBase;
import org.tron.common.application.Service;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol.TransactionSign;

@Component
@Slf4j
public class RpcOfflineService implements Service {

  private int port = Args.getInstance().getRpcPort();
  private Server apiServer;
  @Autowired
  private Wallet wallet;


  @Override
  public void init() {
  }

  @Override
  public void init(Args args) {
  }

  @Override
  public void start() {
    try {
      NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(port)
          .addService(new WalletApi());

      Args args = Args.getInstance();

      if (args.getRpcThreadNum() > 0) {
        serverBuilder = serverBuilder
            .executor(Executors.newFixedThreadPool(args.getRpcThreadNum()));
      }

      // Set configs from config.conf or default value
      serverBuilder
          .maxConcurrentCallsPerConnection(args.getMaxConcurrentCallsPerConnection())
          .flowControlWindow(args.getFlowControlWindow())
          .maxConnectionIdle(args.getMaxConnectionIdleInMillis(), TimeUnit.MILLISECONDS)
          .maxConnectionAge(args.getMaxConnectionAgeInMillis(), TimeUnit.MILLISECONDS)
          .maxMessageSize(args.getMaxMessageSize())
          .maxHeaderListSize(args.getMaxHeaderListSize());

      apiServer = serverBuilder.build().start();
    } catch (IOException e) {
      logger.debug(e.getMessage(), e);
    }

    logger.info("RpcApiService started, listening on " + port);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.err.println("*** shutting down gRPC server since JVM is shutting down");
      //server.this.stop();
      System.err.println("*** server shut down");
    }));
  }

  /**
   * WalletApi.
   */
  private class WalletApi extends WalletImplBase {
    @Override
    public void getTransactionSign2(TransactionSign req,
        StreamObserver<TransactionExtention> responseObserver) {
      TransactionExtention.Builder trxExtBuilder = TransactionExtention.newBuilder();
      Return.Builder retBuilder = Return.newBuilder();
      try {
        TransactionCapsule trx = wallet.getTransactionSign(req);
        trxExtBuilder.setTransaction(trx.getInstance());
        trxExtBuilder.setTxid(trx.getTransactionId().getByteString());
        retBuilder.setResult(true).setCode(response_code.SUCCESS);
      } catch (Exception e) {
        retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
            .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
        logger.info("exception caught" + e.getMessage());
      }
      trxExtBuilder.setResult(retBuilder);
      responseObserver.onNext(trxExtBuilder.build());
      responseObserver.onCompleted();
    }

    @Override
    public void createAddress(BytesMessage req,
        StreamObserver<BytesMessage> responseObserver) {
      byte[] address = wallet.createAdresss(req.getValue().toByteArray());
      BytesMessage.Builder builder = BytesMessage.newBuilder();
      builder.setValue(ByteString.copyFrom(address));
      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
    }

    @Override
    public void generateAddress(EmptyMessage request,
        StreamObserver<AddressPrKeyPairMessage> responseObserver) {
      ECKey ecKey = new ECKey(Utils.getRandom());
      byte[] priKey = ecKey.getPrivKeyBytes();
      byte[] address = ecKey.getAddress();
      String addressStr = Wallet.encode58Check(address);
      String priKeyStr = Hex.encodeHexString(priKey);
      AddressPrKeyPairMessage.Builder builder = AddressPrKeyPairMessage.newBuilder();
      builder.setAddress(addressStr);
      builder.setPrivateKey(priKeyStr);
      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
    }
  }

  @Override
  public void stop() {
    if (apiServer != null) {
      apiServer.shutdown();
    }
  }

  /**
   * ...
   */
  public void blockUntilShutdown() {
    if (apiServer != null) {
      try {
        apiServer.awaitTermination();
      } catch (InterruptedException e) {
        logger.debug(e.getMessage(), e);
      }
    }
  }
}
