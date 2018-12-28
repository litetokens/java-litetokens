package org.litetokens.core.services.interfaceOnSolidity;

import org.springframework.stereotype.Component;
import org.litetokens.common.entity.NodeInfo;
import org.litetokens.core.services.NodeInfoService;

@Component
public class NodeInfoOnSolidityService extends NodeInfoService {

  @Override
  protected void setBlockInfo(NodeInfo nodeInfo) {
    super.setBlockInfo(nodeInfo);
    nodeInfo.setBlock(nodeInfo.getSolidityBlock());
    nodeInfo.setBeginSyncNum(-1);
  }

  @Override
  protected void setCheatWitnessInfo(NodeInfo nodeInfo) {
  }

}
