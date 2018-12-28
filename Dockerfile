FROM litetokensprotocol/litetokens-gradle

RUN set -o errexit -o nounset \
    && echo "git clone" \
    && git clone https://github.com/litetokens/java-litetokens.git \
    && cd java-litetokens \
    && gradle build

WORKDIR /java-litetokens

EXPOSE 18888