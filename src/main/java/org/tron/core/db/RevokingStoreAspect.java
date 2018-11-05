package org.tron.core.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.iq80.leveldb.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;

/**
 * @program: java-tron
 * @description:
 * @author: shydesky@gmail.com
 * @create: 2018-10-15
 **/

@Slf4j
@Aspect
public class RevokingStoreAspect {

  @Pointcut("execution(* org.tron.core.db2.core.RevokingDBWithCachingOldValue.onCreate(..))")
  public void pointPut() {

  }

  @AfterThrowing("pointPut()")
  public void demandRefund() {
    logger.info("find exceptions");
  }

  @AfterReturning("pointPut()")
  public synchronized void beforePut() throws IOException {
    logger.info("After applyBlock！");
  }
}