import { describe, expect, it } from 'vitest'
import { isValidSecurityId, validateOrderRequest } from './orderValidation'

describe('orderValidation', () => {
  it('accepts valid security id', () => {
    expect(isValidSecurityId('600519')).toBe(true)
  })

  it('rejects invalid security id', () => {
    expect(isValidSecurityId('60051')).toBe(false)
    expect(isValidSecurityId('ABC519')).toBe(false)
  })

  it('validates order payload', () => {
    expect(
      validateOrderRequest({
        market: 'XSHG',
        securityId: '600519',
        side: 'B',
        qty: 100,
        price: 10,
      }),
    ).toBeNull()

    expect(
      validateOrderRequest({
        market: 'XSHG',
        securityId: '6005',
        side: 'B',
        qty: 100,
        price: 10,
      }),
    ).toContain('证券代码')

    expect(
      validateOrderRequest({
        market: 'XSHG',
        securityId: '600519',
        side: 'B',
        qty: 0,
        price: 10,
      }),
    ).toContain('数量')

    expect(
      validateOrderRequest({
        market: 'XSHG',
        securityId: '600519',
        side: 'B',
        qty: 100,
        price: 0,
      }),
    ).toContain('价格')
  })
})
