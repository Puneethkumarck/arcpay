// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.24;

// Verifiable on-chain projection of agent identity. PostgreSQL is the source of
// truth; this contract is the tamper-evident record. The platform gas wallet is
// the sole signer (custodial model), so all state changes are registrar-gated.
contract AgentRegistry {
    struct Agent {
        bytes32 owner;
        bytes32 metadataHash;
        bytes32 policyHash;
        bool active;
        bool exists;
        uint64 createdAt;
    }

    address public immutable registrar;
    mapping(bytes32 => Agent) private agents;
    mapping(bytes32 => bytes32[]) private ownerAgents;

    event AgentRegistered(bytes32 indexed agentId, bytes32 indexed owner, bytes32 metadataHash, uint64 timestamp);
    event AgentDeactivated(bytes32 indexed agentId, uint64 timestamp);
    event AgentReactivated(bytes32 indexed agentId, uint64 timestamp);
    event MetadataUpdated(bytes32 indexed agentId, bytes32 metadataHash, uint64 timestamp);
    event PolicyUpdated(bytes32 indexed agentId, bytes32 policyHash, uint64 timestamp);

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "not registrar");
        _;
    }

    constructor() {
        registrar = msg.sender;
    }

    function registerAgent(bytes32 agentId, bytes32 owner, bytes32 metadataHash, uint64 timestamp)
        external
        onlyRegistrar
    {
        require(!agents[agentId].exists, "agent already registered");
        agents[agentId] = Agent({
            owner: owner,
            metadataHash: metadataHash,
            policyHash: bytes32(0),
            active: true,
            exists: true,
            createdAt: timestamp
        });
        ownerAgents[owner].push(agentId);
        emit AgentRegistered(agentId, owner, metadataHash, timestamp);
    }

    function deactivateAgent(bytes32 agentId, uint64 timestamp) external onlyRegistrar {
        require(agents[agentId].exists, "unknown agent");
        agents[agentId].active = false;
        emit AgentDeactivated(agentId, timestamp);
    }

    function reactivateAgent(bytes32 agentId, uint64 timestamp) external onlyRegistrar {
        require(agents[agentId].exists, "unknown agent");
        agents[agentId].active = true;
        emit AgentReactivated(agentId, timestamp);
    }

    function updateMetadata(bytes32 agentId, bytes32 metadataHash, uint64 timestamp) external onlyRegistrar {
        require(agents[agentId].exists, "unknown agent");
        agents[agentId].metadataHash = metadataHash;
        emit MetadataUpdated(agentId, metadataHash, timestamp);
    }

    function updatePolicy(bytes32 agentId, bytes32 policyHash, uint64 timestamp) external onlyRegistrar {
        require(agents[agentId].exists, "unknown agent");
        agents[agentId].policyHash = policyHash;
        emit PolicyUpdated(agentId, policyHash, timestamp);
    }

    function getAgent(bytes32 agentId)
        external
        view
        returns (bytes32 owner, bytes32 metadataHash, bytes32 policyHash, bool active, uint64 createdAt)
    {
        Agent storage agent = agents[agentId];
        require(agent.exists, "unknown agent");
        return (agent.owner, agent.metadataHash, agent.policyHash, agent.active, agent.createdAt);
    }

    function isAgentActive(bytes32 agentId) external view returns (bool) {
        return agents[agentId].active;
    }

    function getAgentsByOwner(bytes32 owner) external view returns (bytes32[] memory) {
        return ownerAgents[owner];
    }
}
