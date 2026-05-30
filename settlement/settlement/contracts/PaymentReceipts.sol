// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.24;

contract PaymentReceipts {
    event ReceiptRecorded(
        bytes32 indexed paymentId,
        address indexed payer,
        address indexed payee,
        uint256 amount,
        bytes32 memoHash,
        uint64 timestamp
    );

    mapping(bytes32 => bool) public recorded;

    function recordReceipt(
        bytes32 paymentId,
        address payer,
        address payee,
        uint256 amount,
        bytes32 memoHash,
        uint64 timestamp
    ) external {
        require(!recorded[paymentId], "receipt already recorded");
        recorded[paymentId] = true;
        emit ReceiptRecorded(paymentId, payer, payee, amount, memoHash, timestamp);
    }
}
