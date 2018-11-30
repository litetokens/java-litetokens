package org.tron.core.db;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Streams;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.CodeCapsule;

@Slf4j
@Component
public class CodeStore extends TronStoreWithRevoking<CodeCapsule> {
  @Getter
  private Cache<ByteArrayWrapper, CodeCapsule> codeCache = CacheBuilder
      .newBuilder().maximumSize(1000_000).recordStats().build();

  @Autowired
  private CodeStore(@Value("code") String dbName) {
    super(dbName);
  }

  @Override
  public CodeCapsule get(byte[] key) {
    CodeCapsule ret = codeCache.getIfPresent(new ByteArrayWrapper(key));
    if (ret == null) {
      ret = getUnchecked(key);
      codeCache.put(new ByteArrayWrapper(key), ret);
    }
    return ret;
  }

  public long getTotalCodes() {
    return Streams.stream(revokingDB.iterator()).count();
  }

  private static CodeStore instance;

  public static void destory() {
    instance = null;
  }

  void destroy() {
    instance = null;
  }

  public byte[] findCodeByHash(byte[] hash) {
    return revokingDB.getUnchecked(hash);
  }
}
