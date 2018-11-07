package org.tron.core.db2.core;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.backup.BackupDbUtil;

@Slf4j
@Aspect
public class ManagerAspect {

  @Autowired
  private BackupDbUtil util;

  //define a pointcut of pushBlock
  @Pointcut("execution(** org.tron.core.db.Manager.pushBlock(..)) && args(block)")
  public void pointPushBlock(BlockCapsule block) {

  }

  //backup db before pushBlock

  @Before("pointPushBlock(block)")
  public void backupDb(BlockCapsule block) {
      util.doBackup(block);
  }

  /*@Around("pointPushBlock(block)")
  public Object aroundBlock(ProceedingJoinPoint joinPoint, BlockCapsule block) throws Throwable{
    System.out.println("环绕通知前....");
    Object obj= (Object) joinPoint.proceed();
    System.out.println("环绕通知后....");
    return obj;
  }*/

  @AfterThrowing("pointPushBlock(block)")
  public void logBeforePushBlock(BlockCapsule block) {
    logger.info("AfterThrowing pushBlock");
  }
}