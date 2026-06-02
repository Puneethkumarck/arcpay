# AgentRegistry — compile & deploy

`AgentRegistry.sol` is the verifiable on-chain projection of agent identity. PostgreSQL
is the source of truth; this contract is the tamper-evident record. The platform gas
wallet is the sole signer (custodial model), so it is the contract's immutable
`registrar` — set to `msg.sender` at deploy time. **Deploy with the same wallet whose
key the Identity service runs as (`PLATFORM_WALLET_PRIVATE_KEY`)**, or state changes
will revert with the `NotRegistrar()` custom error.

## Pinned artifacts

- `AgentRegistry.sol` — source (`pragma ^0.8.24`)
- `AgentRegistry.abi.json` — ABI (the adapter hand-encodes against this; no codegen)
- `AgentRegistry.bin` — compiled creation bytecode (used by the integration test to
  deploy to a local ganache node)

## Recompile (only if the source changes)

```bash
cd identity/identity/contracts
docker run --rm -v "$PWD":/sources -w /sources ethereum/solc:0.8.24 \
  --bin --abi --optimize -o out --overwrite AgentRegistry.sol
python3 -m json.tool out/AgentRegistry.abi > AgentRegistry.abi.json
printf '0x%s\n' "$(cat out/AgentRegistry.bin)" > AgentRegistry.bin
rm -rf out
```

## Deploy + verify on Arc testnet (manual — requires a funded wallet)

This step needs network access and a funded gas wallet, so it is run by an operator,
not in CI. Using [foundry](https://book.getfoundry.sh/):

```bash
# Deploy with the platform wallet (becomes the registrar)
cast send --rpc-url "$ARC_TESTNET_RPC_URL" \
  --private-key "$PLATFORM_WALLET_PRIVATE_KEY" \
  --create "0x$(cat AgentRegistry.bin | sed 's/^0x//')"
# → record the deployed contract address from the receipt

# Verify on the Arc explorer (Blockscout-compatible)
forge verify-contract <DEPLOYED_ADDRESS> AgentRegistry.sol:AgentRegistry \
  --verifier blockscout --verifier-url "$ARC_EXPLORER_API_URL" \
  --compiler-version 0.8.24
```

Then set the address the service reads:

```dotenv
AGENT_REGISTRY_ADDRESS=<DEPLOYED_ADDRESS>   # arcpay.contract.agent-registry-address
```

Record the deployed address here once live:

| Network     | Address | Registrar | Deployed (tx) |
|-------------|---------|-----------|---------------|
| Arc testnet | `0x8A3A6E9825A2b7A6fAe65ebcC8cD95C33327f3Ba` | `0xeaE1E22C917412dC5142305d5ae151668Bd7E8B1` | [`0x1d19a8…a85851`](https://testnet.arcscan.app/tx/0x1d19a8eed774f30e0b7fb04f46e70cea321f096596e62f6c72937c1e5ca85851) |

Set `AGENT_REGISTRY_ADDRESS=0x8A3A6E9825A2b7A6fAe65ebcC8cD95C33327f3Ba` and run the Identity
service with the registrar wallet's key as `PLATFORM_WALLET_PRIVATE_KEY`.

## Rotating the registrar (key rotation / compromise)

The registrar (the only address allowed to mutate agents) is set to the deployer
and can be rotated via a **two-step** transfer — the new wallet must accept, so a
typo can't strand the contract. Run step 1 from the **current** platform wallet and
step 2 from the **new** wallet:

```bash
# 1) current registrar nominates the new wallet
cast send <CONTRACT_ADDRESS> "transferRegistrar(address)" <NEW_REGISTRAR_ADDRESS> \
  --rpc-url "$ARC_TESTNET_RPC_URL" --private-key "$CURRENT_PLATFORM_WALLET_PRIVATE_KEY"

# 2) new wallet accepts the role
cast send <CONTRACT_ADDRESS> "acceptRegistrar()" \
  --rpc-url "$ARC_TESTNET_RPC_URL" --private-key "$NEW_PLATFORM_WALLET_PRIVATE_KEY"
```

After step 2, update `PLATFORM_WALLET_PRIVATE_KEY` to the new wallet's key and
restart the Identity service. Until step 2 completes, the original registrar
remains in control. Confirm with `cast call <CONTRACT_ADDRESS> "registrar()(address)"`.
