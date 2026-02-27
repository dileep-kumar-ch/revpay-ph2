import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'cardMask'
})
export class CardMaskPipe implements PipeTransform {
  transform(value: string): string {
    if (!value) return '';
    // Show only last 4 digits with mask
    return '•••• •••• •••• ' + value;
  }
}