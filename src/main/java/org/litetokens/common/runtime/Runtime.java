package org.litetokens.common.runtime;

import org.litetokens.common.runtime.vm.program.InternalTransaction.XltType;
import org.litetokens.common.runtime.vm.program.ProgramResult;
import org.litetokens.core.exception.ContractExeException;
import org.litetokens.core.exception.ContractValidateException;
import org.litetokens.core.exception.VMIllegalException;


public interface Runtime {

  boolean isCallConstant() throws ContractValidateException;

  void execute() throws ContractValidateException, ContractExeException, VMIllegalException;

  void go();

  XltType getXltType();

  void finalization();

  ProgramResult getResult();

  String getRuntimeError();
}
