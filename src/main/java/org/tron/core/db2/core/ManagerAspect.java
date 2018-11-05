package org.tron.core.db2.core;

/**
 * @program: java-tron
 * @description:
 * @author: shydesky@gmail.com
 * @create: 2018-10-11
 **/

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.backup.BackupDbUtil;

@Slf4j
@Aspect
public class ManagerAspect {
  /**
   * 定义PushBlock的切点
   */

  @Autowired
  private BackupDbUtil util;

  @Pointcut("execution(** org.tron.core.db.Manager.pushBlock(..)) && args(block)")
  public void pointPushBlock(BlockCapsule block) {

  }

  /**
   * 目标方法执行之前调用
   */
  @Before("pointPushBlock(block)")
  public void backupDb(BlockCapsule block) {
    if (block.getNum() % 5000 == 0) {
      util.doBackup();
    }
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