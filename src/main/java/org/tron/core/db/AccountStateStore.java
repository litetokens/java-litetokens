package org.tron.core.db;

import com.google.protobuf.ByteString;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.BytesCapsule;

@Component
public class AccountStateStore extends TronStoreWithRevoking<BytesCapsule> {

  @Autowired
  protected AccountStateStore(@Value("account-state") String dbName) {
    super(dbName);
  }

  @Override
  public BytesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new BytesCapsule(value);
  }

  public BytesCapsule getById(ByteString id) {
    return get(id.toByteArray());
  }
}
