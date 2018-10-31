/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.capsule;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule.BlockId;
import org.tron.common.utils.Time;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.protos.Protocol.Confirmation;
import org.tron.protos.Protocol.Confirmation.Builder;

@Slf4j
public class ConfirmationCapsule implements ProtoCapsule<Confirmation> {

  private BlockId blockId = new BlockId(Sha256Hash.ZERO_HASH, 0);

  private Confirmation confirmation;

  public ConfirmationCapsule(
      BlockId genesisBlockId, ByteString witnessAddress, ByteString witnessSignature) {
    Confirmation.BlockId blockId =
        Confirmation.BlockId.newBuilder()
            .setHash(genesisBlockId.getByteString())
            .setNumber(genesisBlockId.getNum())
            .build();

    Builder builder = Confirmation.newBuilder();
    builder.setBlockId(blockId);
    builder.setWitnessAddress(witnessAddress);
    builder.setWitnessSignature(witnessSignature);

    this.confirmation = builder.build();
  }

  public ConfirmationCapsule(BlockId genesisBlockId, ByteString witnessAddress) {
    Confirmation.BlockId blockId =
        Confirmation.BlockId.newBuilder()
            .setHash(genesisBlockId.getByteString())
            .setNumber(genesisBlockId.getNum())
            .build();

    Builder builder = Confirmation.newBuilder();
    builder.setBlockId(blockId);
    builder.setWitnessAddress(witnessAddress);

    this.confirmation = builder.build();
  }

  public void sign(byte[] privateKey, Sha256Hash blockRawHash) {
    ECKey ecKey = ECKey.fromPrivate(privateKey);
    ECDSASignature signature = ecKey.sign(blockRawHash.getBytes());
    ByteString sig = ByteString.copyFrom(signature.toByteArray());

    this.confirmation = this.confirmation.toBuilder().setWitnessSignature(sig).build();
  }

  @Override
  public byte[] getData() {
    return this.confirmation.toByteArray();
  }

  @Override
  public Confirmation getInstance() {
    return this.confirmation;
  }
}