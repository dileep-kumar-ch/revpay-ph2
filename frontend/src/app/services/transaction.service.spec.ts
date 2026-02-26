import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { TransactionService } from './transaction.service';
import { environment } from '../../environments/environment';
import { SendMoneyRequest, Transaction } from '../models/transaction.model';

describe('TransactionService', () => {
  let service: TransactionService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/transactions`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(TransactionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call POST /transactions/send for sendMoney', () => {
    const request: SendMoneyRequest = {
      receiverUsername: 'bob',
      amount: 25,
      note: 'Dinner'
    };

    const response: Transaction = {
      id: 1,
      senderName: 'Alice Doe',
      receiverName: 'Bob Smith',
      amount: 25,
      type: 'SEND',
      status: 'COMPLETED',
      description: 'Dinner',
      timestamp: '2026-02-26T10:00:00'
    };

    service.sendMoney(request).subscribe(res => {
      expect(res).toEqual(response);
    });

    const req = httpMock.expectOne(`${apiUrl}/send`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);
  });

  it('should call GET /transactions for getTransactions', () => {
    const response: Transaction[] = [
      {
        id: 1,
        senderName: 'Alice Doe',
        receiverName: 'Bob Smith',
        amount: 25,
        type: 'SEND',
        status: 'COMPLETED',
        description: 'Dinner',
        timestamp: '2026-02-26T10:00:00'
      }
    ];

    service.getTransactions().subscribe(res => {
      expect(res).toEqual(response);
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    req.flush(response);
  });
});
