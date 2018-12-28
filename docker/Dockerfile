FROM litetokensprotocol/litetokens-gradle

RUN set -o errexit -o nounset \
#  Download and build java-litetokens
    && echo "git clone" \
    && git clone https://github.com/litetokens/java-litetokens.git \
    && cd java-litetokens \
    && gradle build

# Change work directory
WORKDIR /java-litetokens

# open port 18888
EXPOSE 18888

