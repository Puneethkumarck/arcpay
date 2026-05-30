# PaymentReceipts.sol

On-chain payment receipt registry for the ArcPay settlement service on Circle's Arc L1.

The receipt is a **non-authoritative, verifiable projection** of a settled payment.
The authoritative payment record lives in PostgreSQL; an on-chain receipt failure
never changes a `COMPLETED` payment (see `Web3jReceiptWriter` — best-effort, async).

## Function signature (pinned ABI)

```solidity
function recordReceipt(
    bytes32 paymentId,
    address payer,
    address payee,
    uint256 amount,
    bytes32 memoHash,
    uint64  timestamp
) external;

event ReceiptRecorded(
    bytes32 indexed paymentId,
    address indexed payer,
    address indexed payee,
    uint256 amount,
    bytes32 memoHash,
    uint64  timestamp
);
```

`onChainRef` returned by the writer is the transaction hash of the `recordReceipt`
call (which carries the `ReceiptRecorded` event).

`memoHash = keccak256(memo)` computed off-chain by the writer; the zero hash
(`0x00…00`) is sent when the memo is absent.

The ABI is pinned in [`PaymentReceipts.abi.json`](./PaymentReceipts.abi.json). The
`Web3jReceiptWriter` hand-encodes the `recordReceipt` `Function` against this ABI
(no codegen toolchain required) and submits it through a single nonce-managed
`RawTransactionManager` bound to the platform gas wallet.

## Deploy / verify (Foundry)

Prerequisites: `forge` (Foundry) and an Arc Testnet RPC URL + funded gas wallet.

```bash
# build
forge build

# deploy to Arc Testnet
forge create \
  --rpc-url "$ARC_TESTNET_RPC_URL" \
  --private-key "$GAS_WALLET_PRIVATE_KEY" \
  contracts/PaymentReceipts.sol:PaymentReceipts

# the printed "Deployed to:" address becomes PAYMENT_RECEIPTS_ADDRESS
export PAYMENT_RECEIPTS_ADDRESS=0x...

# verify (if a block explorer / verifier is configured for Arc Testnet)
forge verify-contract \
  --rpc-url "$ARC_TESTNET_RPC_URL" \
  "$PAYMENT_RECEIPTS_ADDRESS" \
  contracts/PaymentReceipts.sol:PaymentReceipts
```

Wire the deployed address into config via `arcpay.contract.payment-receipts-address`
(env `PAYMENT_RECEIPTS_ADDRESS`). The gas wallet key is supplied via
`arcpay.gas-wallet.private-key` (env `GAS_WALLET_PRIVATE_KEY`) and is never logged.

Gas on Arc is paid in USDC from the platform gas wallet; the writer emits a
`settlement.receipt.gas_wallet.low_balance` metric when the wallet balance drops
below the configured threshold.
