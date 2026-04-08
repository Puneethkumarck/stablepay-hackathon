/**
 * Program IDL in camelCase format in order to be used in JS/TS.
 *
 * Note that this is only a type helper and is not the actual IDL. The original
 * IDL can be found at `target/idl/stablepay_escrow.json`.
 */
export type StablepayEscrow = {
  "address": "6G9X8RArxw6f6n41wRKZMsgzRtHuUgPSkYipyjQu8NXD",
  "metadata": {
    "name": "stablepayEscrow",
    "version": "0.1.0",
    "spec": "0.1.0"
  },
  "instructions": [
    {
      "name": "cancel",
      "docs": [
        "Cancel an active escrow — sender-initiated, returns USDC."
      ],
      "discriminator": [
        232,
        219,
        223,
        41,
        219,
        236,
        220,
        190
      ],
      "accounts": [
        {
          "name": "sender",
          "docs": [
            "The original sender who cancels and receives refunded tokens plus rent."
          ],
          "writable": true,
          "signer": true,
          "relations": [
            "escrow"
          ]
        },
        {
          "name": "escrow",
          "docs": [
            "The escrow PDA — must be active and owned by sender.",
            "Closed after cancel; rent returned to sender."
          ],
          "writable": true,
          "pda": {
            "seeds": [
              {
                "kind": "const",
                "value": [
                  101,
                  115,
                  99,
                  114,
                  111,
                  119
                ]
              },
              {
                "kind": "account",
                "path": "escrow.remittance_id",
                "account": "escrow"
              }
            ]
          }
        },
        {
          "name": "vault",
          "docs": [
            "The vault token account holding escrowed USDC."
          ],
          "writable": true,
          "pda": {
            "seeds": [
              {
                "kind": "const",
                "value": [
                  118,
                  97,
                  117,
                  108,
                  116
                ]
              },
              {
                "kind": "account",
                "path": "escrow"
              }
            ]
          }
        },
        {
          "name": "senderToken",
          "docs": [
            "The sender's USDC token account that receives the refunded tokens."
          ],
          "writable": true
        },
        {
          "name": "tokenProgram",
          "docs": [
            "SPL Token program for CPI transfers."
          ],
          "address": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        }
      ],
      "args": []
    },
    {
      "name": "claim",
      "docs": [
        "Release escrowed USDC to the recipient and close the escrow."
      ],
      "discriminator": [
        62,
        198,
        214,
        193,
        213,
        159,
        108,
        210
      ],
      "accounts": [
        {
          "name": "claimAuthority",
          "docs": [
            "The backend-controlled signer that authorizes the claim."
          ],
          "signer": true,
          "relations": [
            "escrow"
          ]
        },
        {
          "name": "escrow",
          "docs": [
            "The escrow PDA holding remittance metadata.",
            "Closed after claim — rent returned to sender."
          ],
          "writable": true,
          "pda": {
            "seeds": [
              {
                "kind": "const",
                "value": [
                  101,
                  115,
                  99,
                  114,
                  111,
                  119
                ]
              },
              {
                "kind": "account",
                "path": "escrow.remittance_id",
                "account": "escrow"
              }
            ]
          }
        },
        {
          "name": "vault",
          "docs": [
            "The vault token account owned by the escrow PDA."
          ],
          "writable": true,
          "pda": {
            "seeds": [
              {
                "kind": "const",
                "value": [
                  118,
                  97,
                  117,
                  108,
                  116
                ]
              },
              {
                "kind": "account",
                "path": "escrow"
              }
            ]
          }
        },
        {
          "name": "recipientToken",
          "docs": [
            "The recipient's USDC token account to receive the funds."
          ],
          "writable": true
        },
        {
          "name": "sender",
          "docs": [
            "The original sender who receives rent from the closed escrow."
          ],
          "writable": true,
          "relations": [
            "escrow"
          ]
        },
        {
          "name": "tokenProgram",
          "docs": [
            "SPL Token program for CPI transfers."
          ],
          "address": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        }
      ],
      "args": []
    },
    {
      "name": "deposit",
      "docs": [
        "Lock USDC in an escrow PDA for a cross-border remittance."
      ],
      "discriminator": [
        242,
        35,
        198,
        137,
        82,
        225,
        242,
        182
      ],
      "accounts": [
        {
          "name": "sender",
          "docs": [
            "The sender who deposits funds and pays for account creation."
          ],
          "writable": true,
          "signer": true
        },
        {
          "name": "escrow",
          "docs": [
            "The escrow PDA that holds remittance metadata."
          ],
          "writable": true,
          "pda": {
            "seeds": [
              {
                "kind": "const",
                "value": [
                  101,
                  115,
                  99,
                  114,
                  111,
                  119
                ]
              },
              {
                "kind": "account",
                "path": "remittanceId"
              }
            ]
          }
        },
        {
          "name": "vault",
          "docs": [
            "The vault token account owned by the escrow PDA."
          ],
          "writable": true,
          "pda": {
            "seeds": [
              {
                "kind": "const",
                "value": [
                  118,
                  97,
                  117,
                  108,
                  116
                ]
              },
              {
                "kind": "account",
                "path": "escrow"
              }
            ]
          }
        },
        {
          "name": "senderToken",
          "docs": [
            "The sender's USDC token account."
          ],
          "writable": true
        },
        {
          "name": "usdcMint",
          "docs": [
            "The SPL token mint (USDC on devnet). Stored in escrow for claim/refund validation."
          ]
        },
        {
          "name": "claimAuthority",
          "docs": [
            "The claim authority pubkey — stored but not required to sign at deposit."
          ]
        },
        {
          "name": "remittanceId",
          "docs": [
            "Unique remittance ID used as PDA seed."
          ]
        },
        {
          "name": "systemProgram",
          "address": "11111111111111111111111111111111"
        },
        {
          "name": "tokenProgram",
          "address": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        }
      ],
      "args": [
        {
          "name": "amount",
          "type": "u64"
        },
        {
          "name": "deadline",
          "type": "i64"
        }
      ]
    },
    {
      "name": "refund",
      "docs": [
        "Refund escrowed USDC to the sender after deadline expiry."
      ],
      "discriminator": [
        2,
        96,
        183,
        251,
        63,
        208,
        46,
        46
      ],
      "accounts": [
        {
          "name": "payer",
          "docs": [
            "The caller who pays the transaction fee. Anyone can call this."
          ],
          "writable": true,
          "signer": true
        },
        {
          "name": "escrow",
          "docs": [
            "The escrow PDA — must be active and owned by the sender.",
            "Closed after refund, rent returned to sender."
          ],
          "writable": true,
          "pda": {
            "seeds": [
              {
                "kind": "const",
                "value": [
                  101,
                  115,
                  99,
                  114,
                  111,
                  119
                ]
              },
              {
                "kind": "account",
                "path": "escrow.remittance_id",
                "account": "escrow"
              }
            ]
          }
        },
        {
          "name": "vault",
          "docs": [
            "The vault token account holding escrowed USDC."
          ],
          "writable": true,
          "pda": {
            "seeds": [
              {
                "kind": "const",
                "value": [
                  118,
                  97,
                  117,
                  108,
                  116
                ]
              },
              {
                "kind": "account",
                "path": "escrow"
              }
            ]
          }
        },
        {
          "name": "sender",
          "docs": [
            "The original sender who receives rent from the closed escrow."
          ],
          "writable": true,
          "relations": [
            "escrow"
          ]
        },
        {
          "name": "senderToken",
          "docs": [
            "The sender's USDC token account that receives the refund."
          ],
          "writable": true
        },
        {
          "name": "tokenProgram",
          "docs": [
            "SPL Token program for CPI transfers."
          ],
          "address": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        }
      ],
      "args": []
    }
  ],
  "accounts": [
    {
      "name": "escrow",
      "discriminator": [
        31,
        213,
        123,
        187,
        186,
        22,
        218,
        155
      ]
    }
  ],
  "errors": [
    {
      "code": 6000,
      "name": "amountTooSmall",
      "msg": "Escrow amount must be greater than zero"
    },
    {
      "code": 6001,
      "name": "alreadyInitialized",
      "msg": "Escrow has already been initialized"
    },
    {
      "code": 6002,
      "name": "unauthorizedSender",
      "msg": "Unauthorized: caller is not the escrow sender"
    },
    {
      "code": 6003,
      "name": "unauthorizedClaimAuthority",
      "msg": "Unauthorized: caller is not the claim authority"
    },
    {
      "code": 6004,
      "name": "overflow",
      "msg": "Arithmetic overflow"
    },
    {
      "code": 6005,
      "name": "invalidEscrowStatus",
      "msg": "Escrow is not in the expected status"
    },
    {
      "code": 6006,
      "name": "escrowExpired",
      "msg": "Escrow has expired"
    },
    {
      "code": 6007,
      "name": "escrowNotExpired",
      "msg": "Escrow has not yet expired"
    },
    {
      "code": 6008,
      "name": "invalidMint",
      "msg": "Token mint does not match expected USDC mint"
    },
    {
      "code": 6009,
      "name": "invalidTokenOwner",
      "msg": "Token account owner mismatch"
    }
  ],
  "types": [
    {
      "name": "escrow",
      "docs": [
        "Escrow account holding USDC for a cross-border remittance.",
        "",
        "Created by the deposit instruction. Released to the recipient on claim,",
        "or refunded to the sender after expiry."
      ],
      "type": {
        "kind": "struct",
        "fields": [
          {
            "name": "sender",
            "docs": [
              "The sender who deposited funds."
            ],
            "type": "pubkey"
          },
          {
            "name": "claimAuthority",
            "docs": [
              "Backend-controlled signer that authorizes claim."
            ],
            "type": "pubkey"
          },
          {
            "name": "mint",
            "docs": [
              "SPL token mint (USDC)."
            ],
            "type": "pubkey"
          },
          {
            "name": "amount",
            "docs": [
              "Deposited amount in token base units."
            ],
            "type": "u64"
          },
          {
            "name": "deadline",
            "docs": [
              "Unix timestamp after which refund is allowed."
            ],
            "type": "i64"
          },
          {
            "name": "status",
            "docs": [
              "Current lifecycle state."
            ],
            "type": {
              "defined": {
                "name": "escrowStatus"
              }
            }
          },
          {
            "name": "bump",
            "docs": [
              "Stored canonical PDA bump."
            ],
            "type": "u8"
          },
          {
            "name": "remittanceId",
            "docs": [
              "Unique ID linking on-chain escrow to off-chain remittance."
            ],
            "type": "pubkey"
          }
        ]
      }
    },
    {
      "name": "escrowStatus",
      "docs": [
        "Lifecycle state of an escrow."
      ],
      "type": {
        "kind": "enum",
        "variants": [
          {
            "name": "active"
          },
          {
            "name": "claimed"
          },
          {
            "name": "refunded"
          },
          {
            "name": "cancelled"
          }
        ]
      }
    }
  ]
};
