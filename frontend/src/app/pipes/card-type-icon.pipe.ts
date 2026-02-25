import { Pipe, PipeTransform } from '@angular/core';
import { CardType } from '../models/payment-method.model';

@Pipe({
  name: 'cardTypeIcon'
})
export class CardTypeIconPipe implements PipeTransform {
  transform(cardType: CardType): string {
    switch(cardType) {
      case CardType.VISA:
        return 'fa-brands fa-cc-visa';
      case CardType.MASTERCARD:
        return 'fa-brands fa-cc-mastercard';
      case CardType.AMEX:
        return 'fa-brands fa-cc-amex';
      case CardType.DISCOVER:
        return 'fa-brands fa-cc-discover';
      default:
        return 'fa-regular fa-credit-card';
    }
  }
}