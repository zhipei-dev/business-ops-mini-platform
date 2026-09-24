const { test, expect } = require('@playwright/test');

test('purchase to receipt to sales fulfillment updates ledger and audit', async ({ page }) => {
  await page.goto('/');

  await page.locator('#product-form input[name=sku]').fill('E2E-SKU');
  await page.locator('#product-form input[name=name]').fill('E2E product');
  await page.locator('#product-form input[name=unitPrice]').fill('10');
  await page.locator('#product-form button').click();

  await expect(page.locator('#products')).toContainText('E2E-SKU');
  await expect(page.locator('#purchase-form select[name=productId] option'))
    .toHaveText('E2E-SKU — E2E product');

  await page.locator('#purchase-form input[name=party]').fill('Demo Supplier');
  await page.locator('#purchase-form input[name=quantity]').fill('10');
  await page.locator('#purchase-form input[name=unitPrice]').fill('10');
  await page.locator('#purchase-form button').click();

  await expect(page.getByRole('button', { name: 'Submit' })).toBeVisible();
  await page.getByRole('button', { name: 'Submit' }).click();
  await expect(page.getByRole('button', { name: 'Approve' })).toBeVisible();
  await page.getByRole('button', { name: 'Approve' }).click();
  await expect(page.getByRole('button', { name: 'Receive' })).toBeVisible();
  await page.getByRole('button', { name: 'Receive' }).click();

  const inventoryRow = page.locator('#inventory tr').filter({ hasText: 'E2E-SKU' });
  await expect(inventoryRow).toContainText('10');

  await page.locator('#sales-form input[name=party]').fill('Demo Customer');
  await page.locator('#sales-form input[name=quantity]').fill('4');
  await page.locator('#sales-form input[name=unitPrice]').fill('10');
  await page.locator('#sales-form button').click();

  await expect(page.getByRole('button', { name: 'Confirm' })).toBeVisible();
  await page.getByRole('button', { name: 'Confirm' }).click();
  await expect(page.getByRole('button', { name: 'Fulfill' })).toBeVisible();
  await page.getByRole('button', { name: 'Fulfill' }).click();

  await expect(inventoryRow).toContainText('6');
  await expect(page.locator('#audit')).toContainText('FULFILL');
});
