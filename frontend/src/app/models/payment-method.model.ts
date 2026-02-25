export enum CardType {
  VISA = 'VISA',
  MASTERCARD = 'MASTERCARD',
  AMEX = 'AMEX',
  DISCOVER = 'DISCOVER'
}

export interface PaymentMethod {
  id: number;
  cardHolderName: string;
  cardType: CardType;
  lastFourDigits: string;
  expiryMonth: number;
  expiryYear: number;
  isDefault: boolean;
  isActive: boolean;
  createdAt: Date;
}

export interface AddCardRequest {
  cardNumber: string;
  cardHolderName: string;
  cardType: CardType;
  expiryMonth: number;
  expiryYear: number;
  cvv: string;
  setAsDefault?: boolean;
}

export interface SetDefaultCardRequest {
  cardId: number;
}