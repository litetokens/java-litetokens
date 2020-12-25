# LITETOKENS protobuf protocol

## LITETOKENS uses Google protobuf protocol, which involves accounts, blocks, and multiple levels of transmission.

+ There are three types of accounts: basic account, asset release account and contract account. An account contains 6 attributes: account name, account type, address, balance, voting, and other assets.
+ Furthermore, the basic account can be applied to become a verification node, which has additional attributes, statistical voting number, public key, URL, and historical performance parameters.

   Three types of `Account`: `Normal`, `AssetIssue`, and `Contract`.

    enum AccountType {
      Normal = 0;
      AssetIssue = 1;
      Contract = 2;
     }

   A `Account` contains 7 kinds of parameters:
   `account_name`: The name of the account-for example: "_SicCongsAccount_".
   `type`: The type of the account-for example: _0_ represents the account type is `Normal`.
   `balance`: XLT balance of the account-for example: _4213312_.
   `votes`: The number of votes from the account-for example: _{("0x1b7w...9xj3",323),("0x8djq...j12m",88),...,("0x82nd...mx6i",10001)}_.
   `asset`: other assets on the account except XLT-for example: _{<"WishToken",66666>,<"Dogie",233>}_.
   `latest_operation_time`: The latest active time of the account.
   
    // Account
    message Account {
      message Vote {
        bytes vote_address = 1;
        int64 vote_count = 2;
       }
       bytes accout_name = 1;
       AccountType type = 2;
       bytes address = 3;
       int64 balance = 4;
       repeated Vote votes = 5;
       map<string, int64> asset = 6;
       int64 latest_operation_time = 10;
     }

   A `Witness` contains 8 kinds of parameters:
   `address`: the address of the verification node-for example: _"0xu82h...7237"_.
   `voteCount`: The number of votes obtained by the verification node-for example: _234234_.
   `pubKey`: the public key of the verification node-for example: _"0xu82h...7237"_.
   `url`: url link of verification node.
   `totalProduce`: the number of blocks produced by the verification node-for example: _2434_.
   `totalMissed`: The number of blocks lost by the verification node-for example: _7_.
   `latestBlockNum`: The latest block height-for example: _4522_.
   `isJobs`: Boolean table type flag bit.

    // Witness
    message Witness {
      bytes address = 1;
      int64 voteCount = 2;
      bytes pubKey = 3;
      string url = 4;
      int64 totalProduced = 5;
      int64 totalMissed = 6;
      int64 latestBlockNum = 7;
      bool isJobs = 9;
     }

+ A block consists of a block header and multiple transactions. The block header contains the basic information of the block such as timestamp, the root of the transaction dictionary tree, parent hash, and signature.

   A `block` contains `transactions` and `block_header`.
   `transactions`: Transaction information in the block.
   `block_header`: One of the components of the block.

    // block
    message Block {
      repeated Transaction transactions = 1;
      BlockHeader block_header = 2;
     }

   `BlockHeader` includes `raw_data` and `witness_signature`.
   `raw_data`: `raw` information.
   `witness_signature`: The signature from the block header to the verification node.

   The message `raw` contains 6 kinds of parameters:
   `timestamp`: The timestamp of the message body-for example: _14356325_.
   `txTrieRoot`: The root of the Merkle Tree-for example: _"7dacsa...3ed"_.
   `parentHash`: The hash value of the previous block-for example: _"7dacsa...3ed"_.
   `number`: block height-for example: _13534657_.
   `witness_id`: the id of the verification node-for example: _"0xu82h...7237"_.
   `witness_address`: the address of the verification node-for example: _"0xu82h...7237"_.

    message BlockHeader {
      message raw {
        int64 timestamp = 1;
        bytes txTrieRoot = 2;
        bytes parentHash = 3;
        //bytes nonce = 5;
        //bytes difficulty = 6;
        uint64 number = 7;
        uint64 witness_id = 8;
        bytes witness_address = 9;
       }
       raw raw_data = 1;
       bytes witness_signature = 2;
     }

   The message body `ChainInventory` includes `BlockId` and `remain_num`.
   `BlockId`: the identity information of the block.
   `remain_num`: The number of remaining blocks during the synchronization process.
   
   A `BlockId` contains 2 parameters:
   `hash`: The hash value of the block.
   `number`: The height is the current block number.
   
    message ChainInventory {
      message BlockId {
        bytes hash = 1;
        int64 number = 2;
       }
     repeated BlockId ids = 1;
     int64 remain_num = 2;
     }
           
+ There are many types of trading contracts, including account creation contract, account update contract, transfer contract, transfer assertion contract, asset voting contract, witness node voting contract, witness node creation contract, witness node update contract, asset release contract, participation in asset release 11 types of and deployment contracts.

   `AccountCreatContract` contains 3 kinds of parameters:
   `type`: Account type-for example: _0_ represents the account type `Normal`.
   `account_name`: Account name-for example: _"SiCongsaccount"_.
   `owner_address`: the address of the contract holder-for example: _"0xu82h...7237"_.

    message AccountCreateContract {
      AccountType type = 1;
      bytes account_name = 2;
      bytes owner_address = 3;
     }
   `AccountUpdateContract` contains 2 kinds of parameters:
   `account_name`: Account name-for example: _"SiCongsaccount"_.
   `owner_address`: the address of the contract holder-for example: _"0xu82h...7237"_.
   
    message AccountUpdateContract {
      bytes account_name = 1;
      bytes owner_address = 2;
     }
     
   `TransferContract` contains 3 kinds of parameters:
   `amount`: XLT amount-for example: _12534_.
   `to_address`: Receiver's address-for example: _"0xu82h...7237"_.
   `owner_address`: the address of the contract holder-for example: _"0xu82h...7237"_.

    message TransferContract {
      bytes owner_address = 1;
      bytes to_address = 2;
      int64 amount = 3;
     }

   `TransferAssetContract` contains 4 kinds of parameters:
   `asset_name`: Asset name-for example: _”SiCongsaccount”_.
   `to_address`: recipient address-for example: _"0xu82h...7237"_.
   `owner_address`: the address of the contract holder-for example: _"0xu82h...7237"_.
   `amount`: The target asset amount-for example: _12353_.

    message TransferAssetContract {
      bytes asset_name = 1;
      bytes owner_address = 2;
      bytes to_address = 3;
      int64 amount = 4;
     }

   `VoteAssetContract` contains 4 kinds of parameters:
   `vote_address`: voter's address-for example: _"0xu82h...7237"_.
   `support`: vote for or not-for example: _true_.
   `owner_address`: the address of the contract holder-for example: _"0xu82h...7237"_.
   `count`: the number of votes-for example: _2324234_.

    message VoteAssetContract {
      bytes owner_address = 1;
      repeated bytes vote_address = 2;
      bool support = 3;
      int32 count = 5;
     }

   `VoteWitnessContract` contains 4 kinds of parameters:
   `vote_ad
