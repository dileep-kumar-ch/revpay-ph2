export interface Card {
    id: number;
    cardHolderName: string;
    lastFourDigits: string;
    expiryDate: string;
    cardType: 'VISA' | 'MASTERCARD' | 'AMEX' | 'DISCOVER' | 'OTHER';
    paymentMethodType: 'DEBIT' | 'CREDIT';
    isDefault: boolean;
}

export interface AddCardRequest {
    cardHolderName: string;
    cardNumber: string;
    expiryDate: string;
    cvv: string;
    paymentMethodType: 'DEBIT' | 'CREDIT';
    setAsDefault: boolean;
}
