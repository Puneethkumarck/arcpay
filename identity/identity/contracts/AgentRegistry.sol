// SPDX-License-Identifier: Apache-2.0
pragma solidity 0.8.24;

/// @title AgentRegistry
/// @notice Verifiable, tamper-evident on-chain projection of ArcPay agent identity.
/// @dev PostgreSQL is the source of truth; this contract is the independently
///      verifiable record. The platform gas wallet is the sole signer (custodial
///      model), so all state changes are registrar-gated. Timestamps are taken from
///      `block.timestamp` so they are trustless, not caller-asserted. Each agent is
///      bound to its on-chain wallet address and indexed by it, so an external party
///      that sees an on-chain payment can resolve the wallet back to a registered,
///      active agent. The registrar can be rotated via a two-step transfer (cf. #200).
contract AgentRegistry {
    struct Agent {
        bytes32 owner;
        bytes32 metadataHash;
        bytes32 policyHash;
        address wallet;
        bool active;
        bool exists;
        uint64 createdAt;
    }

    address public registrar;
    address public pendingRegistrar;
    mapping(bytes32 => Agent) private agents;
    mapping(bytes32 => bytes32[]) private ownerAgents;
    mapping(address => bytes32) private walletToAgent;

    /// @notice Emitted when an agent is first registered on-chain.
    event AgentRegistered(
        bytes32 indexed agentId, bytes32 indexed owner, address indexed wallet, bytes32 metadataHash, uint64 timestamp
    );
    /// @notice Emitted when an agent is deactivated.
    event AgentDeactivated(bytes32 indexed agentId, uint64 timestamp);
    /// @notice Emitted when a previously deactivated agent is reactivated.
    event AgentReactivated(bytes32 indexed agentId, uint64 timestamp);
    /// @notice Emitted when an agent's metadata hash changes.
    event MetadataUpdated(bytes32 indexed agentId, bytes32 metadataHash, uint64 timestamp);
    /// @notice Emitted when an agent's policy hash changes.
    event PolicyUpdated(bytes32 indexed agentId, bytes32 policyHash, uint64 timestamp);
    /// @notice Emitted when a registrar transfer is initiated.
    event RegistrarTransferStarted(address indexed currentRegistrar, address indexed pendingRegistrar);
    /// @notice Emitted when a pending registrar accepts the role.
    event RegistrarTransferred(address indexed previousRegistrar, address indexed newRegistrar);

    error NotRegistrar();
    error NotPendingRegistrar();
    error ZeroAddress();
    error ZeroAgentId();
    error AgentAlreadyRegistered(bytes32 agentId);
    error WalletAlreadyRegistered(address wallet);
    error UnknownAgent(bytes32 agentId);

    modifier onlyRegistrar() {
        if (msg.sender != registrar) {
            revert NotRegistrar();
        }
        _;
    }

    constructor() {
        registrar = msg.sender;
    }

    /// @notice Registers a new agent and binds it to its on-chain wallet address.
    /// @dev Idempotent: re-submitting the same (agentId, owner, wallet) is a no-op success, so a
    ///      workflow retry after a mined-but-unacknowledged tx does not fail a registered agent.
    ///      A re-registration of the same agentId with a different owner or wallet reverts.
    /// @param agentId Off-chain agent UUID encoded as bytes32.
    /// @param owner Off-chain owner UUID encoded as bytes32.
    /// @param wallet The agent's on-chain wallet address.
    /// @param metadataHash keccak256 of the agent metadata.
    function registerAgent(bytes32 agentId, bytes32 owner, address wallet, bytes32 metadataHash)
        external
        onlyRegistrar
    {
        if (agentId == bytes32(0)) {
            revert ZeroAgentId();
        }
        Agent storage existing = agents[agentId];
        if (existing.exists) {
            if (existing.owner != owner || existing.wallet != wallet) {
                revert AgentAlreadyRegistered(agentId);
            }
            return;
        }
        if (wallet == address(0)) {
            revert ZeroAddress();
        }
        if (walletToAgent[wallet] != bytes32(0)) {
            revert WalletAlreadyRegistered(wallet);
        }
        agents[agentId] = Agent({
            owner: owner,
            metadataHash: metadataHash,
            policyHash: bytes32(0),
            wallet: wallet,
            active: true,
            exists: true,
            createdAt: uint64(block.timestamp)
        });
        ownerAgents[owner].push(agentId);
        walletToAgent[wallet] = agentId;
        emit AgentRegistered(agentId, owner, wallet, metadataHash, uint64(block.timestamp));
    }

    /// @notice Deactivates a registered agent.
    function deactivateAgent(bytes32 agentId) external onlyRegistrar {
        _requireExists(agentId);
        agents[agentId].active = false;
        emit AgentDeactivated(agentId, uint64(block.timestamp));
    }

    /// @notice Reactivates a registered agent.
    function reactivateAgent(bytes32 agentId) external onlyRegistrar {
        _requireExists(agentId);
        agents[agentId].active = true;
        emit AgentReactivated(agentId, uint64(block.timestamp));
    }

    /// @notice Updates an agent's metadata hash.
    function updateMetadata(bytes32 agentId, bytes32 metadataHash) external onlyRegistrar {
        _requireExists(agentId);
        agents[agentId].metadataHash = metadataHash;
        emit MetadataUpdated(agentId, metadataHash, uint64(block.timestamp));
    }

    /// @notice Updates an agent's policy hash.
    function updatePolicy(bytes32 agentId, bytes32 policyHash) external onlyRegistrar {
        _requireExists(agentId);
        agents[agentId].policyHash = policyHash;
        emit PolicyUpdated(agentId, policyHash, uint64(block.timestamp));
    }

    /// @notice Returns the full on-chain record for an agent. Reverts if unknown.
    function getAgent(bytes32 agentId)
        external
        view
        returns (bytes32 owner, address wallet, bytes32 metadataHash, bytes32 policyHash, bool active, uint64 createdAt)
    {
        Agent storage agent = agents[agentId];
        if (!agent.exists) {
            revert UnknownAgent(agentId);
        }
        return (agent.owner, agent.wallet, agent.metadataHash, agent.policyHash, agent.active, agent.createdAt);
    }

    /// @notice Returns whether an agent is currently active. Unknown agents return false.
    function isAgentActive(bytes32 agentId) external view returns (bool) {
        return agents[agentId].active;
    }

    /// @notice Resolves an on-chain wallet address to its agent id, or bytes32(0) if unregistered.
    function getAgentByWallet(address wallet) external view returns (bytes32) {
        return walletToAgent[wallet];
    }

    /// @notice Returns whether a wallet belongs to a registered, currently-active agent.
    function isWalletActive(address wallet) external view returns (bool) {
        bytes32 agentId = walletToAgent[wallet];
        return agentId != bytes32(0) && agents[agentId].active;
    }

    /// @notice Returns all agent ids registered under an owner.
    function getAgentsByOwner(bytes32 owner) external view returns (bytes32[] memory) {
        return ownerAgents[owner];
    }

    /// @notice Begins a two-step registrar transfer.
    /// @param newRegistrar The address that must call {acceptRegistrar} to take over.
    function transferRegistrar(address newRegistrar) external onlyRegistrar {
        if (newRegistrar == address(0)) {
            revert ZeroAddress();
        }
        pendingRegistrar = newRegistrar;
        emit RegistrarTransferStarted(registrar, newRegistrar);
    }

    /// @notice Completes a registrar transfer; callable only by the pending registrar.
    function acceptRegistrar() external {
        if (msg.sender != pendingRegistrar) {
            revert NotPendingRegistrar();
        }
        emit RegistrarTransferred(registrar, pendingRegistrar);
        registrar = pendingRegistrar;
        pendingRegistrar = address(0);
    }

    function _requireExists(bytes32 agentId) private view {
        if (!agents[agentId].exists) {
            revert UnknownAgent(agentId);
        }
    }
}
