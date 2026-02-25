export interface Transaction {
    id: number;
    senderName: string;
    receiverName: string;
    amount: number;
    type: 'SEND' | 'RECEIVE' | 'REQUEST' | 'DEPOSIT' | 'WITHDRAWAL' | 'PAYMENT';
    status: 'PENDING' | 'COMPLETED' | 'FAILED';
    description: string;
    timestamp: string;
}

export interface SendMoneyRequest {
    receiverUsername: string;
    amount: number;
    note: string;
}
