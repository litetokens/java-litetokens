/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.litetokens.common.runtime.config;

import lombok.Setter;
import org.litetokens.common.utils.ForkController;
import org.litetokens.core.config.Parameter.ForkBlockVersionConsts;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.db.Manager;

/**
 * For developer only
 */
public class VMConfig {

  public static final int MAX_CODE_LENGTH = 1024 * 1024;

  public static final int MAX_FEE_LIMIT = 1_000_000_000; //1000 xlt

  private boolean vmTraceCompressed = false;
  private boolean vmTrace = Args.getInstance().isVmTrace();

  //Odyssey3.2 hard fork -- ForkBlockVersionConsts.ENERGY_LIMIT
  @Setter
  private static boolean ENERGY_LIMIT_HARD_FORK = false;

  @Setter
  private static boolean ALLOW_LVM_TRANSFER_TRC10 = false;

  private VMConfig() {
  }

  private static class SystemPropertiesInstance {

    private static final VMConfig INSTANCE = new VMConfig();
  }

  public static VMConfig getInstance() {
    return SystemPropertiesInstance.INSTANCE;
  }

  public boolean vmTrace() {
    return vmTrace;
  }

  public boolean vmTraceCompressed() {
    return vmTraceCompressed;
  }

  public static void initVmHardFork() {
    ENERGY_LIMIT_HARD_FORK = ForkController.instance().pass(ForkBlockVersionConsts.ENERGY_LIMIT);
  }

  public static void initAllowLvmTransferTrc10(long allow) {    ALLOW_LVM_TRANSFER_TRC10 = allow == 1;
  }

  public static boolean getEnergyLimitHardFork() {
    return ENERGY_LIMIT_HARD_FORK;
  }

  public static boolean allowLvmTransferTrc10() {
    return ALLOW_LVM_TRANSFER_TRC10;
  }

}