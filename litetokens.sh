#!/bin/bash

if [ $encrypted_e5855cb9e09c_key ];then
  openssl aes-256-cbc -K $encrypted_e5855cb9e09c_key -iv $encrypted_e5855cb9e09c_iv -in litetokens.enc -out litetokens -d
  cat litetokens > ~/.ssh/id_rsa
  chmod 600 ~/.ssh/id_rsa
  sonar-scanner
fi
