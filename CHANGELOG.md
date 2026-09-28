# Changelog

## [1.5.0](https://github.com/Blaaiz/blaaiz-java-sdk/compare/v1.4.0...v1.5.0) (2026-09-28)


### Features

* **oauth:** request the compliance-kyc scopes by default ([e65e2c7](https://github.com/Blaaiz/blaaiz-java-sdk/commit/e65e2c7ec51921c9840491254634707a3bedf302))
* **oauth:** request the compliance-kyc:pii:read scope by default ([ec6cda6](https://github.com/Blaaiz/blaaiz-java-sdk/commit/ec6cda647147baa4de9296b09964f7c790733ada))
* **signa:** add Signa merchant KYC session service ([23f5ca7](https://github.com/Blaaiz/blaaiz-java-sdk/commit/23f5ca7be3db50791e06f793b6ca38d924f2d193))
* **signa:** read captured applicant data and documents ([40fa733](https://github.com/Blaaiz/blaaiz-java-sdk/commit/40fa733c95a1060ed414cd3f2eada4722b09095f))

## 1.4.0 - 2026-08-28

This release brings the SDK up to date with the current Blaaiz API. All Blaaiz SDKs move to 1.4.0 together, so the same version means the same features in every language.

### Added
- Merchant reference on payouts and collections. Blaaiz saves it, returns it, and lets you find the transaction by it.
- Swaps: move money between two of your business wallets.
- Refunds: start a refund and get a refund.
- Rates: list the exchange rates for your business.
- Bank checks: verify a GBP account (payee) and a EUR IBAN.
- Interac money request: ask a payer for money by email.
- Business customer KYB: add and remove owners, upload owner ID files, upgrade to full KYB, and submit for review.
- Business customer documents: upload, list, get, update, and delete.

### Fixed
- Create a business customer without personal ID fields. The API does not allow them for a business.
- Upload customer files with the correct request method.
- Start a collection without the old, unused `currency` field.
- Update and replay a webhook on the correct address.

### Deprecated
- The swap method is now `initiate()`. The old `swap()` name still works, but it will be removed in a future major version.
